package com.pabaumgartner.photogallery.wizard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.regex.Pattern;

final class HttpEndpoints {

	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	static final Duration API_TIMEOUT = Duration.ofSeconds(30);

	static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);

	static final int MAX_RESPONSE_BODY_BYTES = 10 * 1024 * 1024;

	static final int MAX_REDIRECTS = 5;

	private static final Pattern IP_LITERAL = Pattern.compile("^\\[?[0-9a-f.:]+]?$");

	private static final int MAX_LOGGED_BODY_LENGTH = 200;

	private static final Set<String> CONTENT_HEADERS = Set.of("content-type", "content-length");

	private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);

	private HttpEndpoints() {
	}

	static HttpClient newClient() {
		return HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			// Redirects are followed by send(...) instead, so that each hop is
			// re-checked before credentials go back on the wire.
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();
	}

	/**
	 * Sends a request that carries credentials, bounding the response body and following
	 * redirects only to locations that {@link #requireCredentialSafeTransport(URI)}
	 * accepts.
	 */
	static HttpResponse<String> send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
		HttpRequest current = request;
		for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
			HttpResponse<String> response = client.send(current, boundedString());
			HttpRequest next = redirect(current, response);
			if (next == null) {
				return response;
			}
			current = next;
		}
		throw new IOException("Gave up after " + MAX_REDIRECTS + " redirects starting at " + request.uri());
	}

	private static HttpResponse.BodyHandler<String> boundedString() {
		return responseInfo -> new BoundedStringSubscriber();
	}

	private static HttpRequest redirect(HttpRequest request, HttpResponse<String> response) throws IOException {
		int status = response.statusCode();
		if (!REDIRECT_STATUSES.contains(status)) {
			return null;
		}
		String location = response.headers().firstValue("Location").orElse("");
		if (location.isBlank()) {
			return null;
		}

		URI target = request.uri().resolve(location);
		requireCredentialSafeTransport(target);

		// Only 307 and 308 replay the method and body; the older codes degrade to a
		// bodyless GET, which is what the JDK's own redirect handling does.
		boolean replayBody = status == 307 || status == 308;
		HttpRequest.Builder builder = HttpRequest
			.newBuilder(request,
					(name, value) -> replayBody || !CONTENT_HEADERS.contains(name.toLowerCase(Locale.ROOT)))
			.uri(target);
		if (replayBody) {
			builder.method(request.method(), request.bodyPublisher().orElseGet(HttpRequest.BodyPublishers::noBody));
		}
		else {
			builder.GET();
		}
		return builder.build();
	}

	/**
	 * Rejects sending credentials in clear text to anything but the local machine or a
	 * private network, so a mistyped {@code http://} endpoint cannot leak the gallery
	 * password across the public internet.
	 */
	static void requireCredentialSafeTransport(URI uri) throws IOException {
		if ("https".equalsIgnoreCase(uri.getScheme()) || isPrivateOrLoopback(uri.getHost())) {
			return;
		}
		throw new IOException("Refusing to send credentials to " + uri.getScheme() + "://" + uri.getHost()
				+ " in clear text; use https, or a loopback or private network address");
	}

	static String truncateForLog(String body) {
		if (body == null || body.isBlank()) {
			return "<empty>";
		}
		String singleLine = body.replaceAll("\\s+", " ").trim();
		return singleLine.length() <= MAX_LOGGED_BODY_LENGTH ? singleLine
				: singleLine.substring(0, MAX_LOGGED_BODY_LENGTH) + "…";
	}

	static String sanitizeMultipartFilename(String filename) {
		return filename.replaceAll("[\"\\\\\\r\\n]", "_");
	}

	static void restoreInterruptFlag(Exception ex) {
		if (ex instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		}
	}

	private static boolean isPrivateOrLoopback(String host) {
		if (host == null || host.isBlank()) {
			return false;
		}
		String normalized = host.toLowerCase(Locale.ROOT);
		if (normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.endsWith(".local")) {
			return true;
		}
		if (!IP_LITERAL.matcher(normalized).matches()) {
			return false;
		}
		try {
			InetAddress address = InetAddress.getByName(normalized);
			return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
					|| address.isAnyLocalAddress();
		}
		catch (UnknownHostException ex) {
			return false;
		}
	}

	/**
	 * Collects a response body as UTF-8 text, failing rather than buffering without limit
	 * when a server sends far more than an API response should.
	 */
	private static final class BoundedStringSubscriber implements HttpResponse.BodySubscriber<String> {

		private final CompletableFuture<String> body = new CompletableFuture<>();

		private final ByteArrayOutputStream collected = new ByteArrayOutputStream();

		private Flow.Subscription subscription;

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(List<ByteBuffer> buffers) {
			for (ByteBuffer buffer : buffers) {
				if (collected.size() + buffer.remaining() > MAX_RESPONSE_BODY_BYTES) {
					subscription.cancel();
					body.completeExceptionally(
							new IOException("Response body exceeds the " + MAX_RESPONSE_BODY_BYTES + " byte limit"));
					return;
				}
				byte[] chunk = new byte[buffer.remaining()];
				buffer.get(chunk);
				collected.writeBytes(chunk);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			body.completeExceptionally(throwable);
		}

		@Override
		public void onComplete() {
			body.complete(collected.toString(StandardCharsets.UTF_8));
		}

		@Override
		public CompletionStage<String> getBody() {
			return body;
		}

	}

}
