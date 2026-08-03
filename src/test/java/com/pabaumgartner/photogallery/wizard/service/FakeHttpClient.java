package com.pabaumgartner.photogallery.wizard.service;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

/**
 * Replays a fixed sequence of responses and records what was sent, so the HTTP services
 * can be driven without a server. The last response repeats once the sequence runs out.
 */
final class FakeHttpClient extends HttpClient {

	private final List<StubResponse> responses;

	private final List<RecordedRequest> recorded = new ArrayList<>();

	private int next;

	private FakeHttpClient(List<StubResponse> responses) {
		this.responses = List.copyOf(responses);
	}

	static FakeHttpClient replying(int statusCode, String body) {
		return new FakeHttpClient(List.of(new StubResponse(statusCode, body)));
	}

	static FakeHttpClient replyingInOrder(List<StubResponse> responses) {
		return new FakeHttpClient(responses);
	}

	List<RecordedRequest> recorded() {
		return recorded;
	}

	List<String> bodiesSentTo(String uriSuffix) {
		return recorded.stream()
			.filter(request -> request.uri().endsWith(uriSuffix))
			.map(RecordedRequest::body)
			.toList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
		recorded.add(new RecordedRequest(request, bodyOf(request)));
		StubResponse response = responses.get(Math.min(next++, responses.size() - 1));
		return (HttpResponse<T>) new StubHttpResponse(response, request.uri());
	}

	private static String bodyOf(HttpRequest request) {
		return request.bodyPublisher().map(FakeHttpClient::drain).orElse("");
	}

	private static String drain(HttpRequest.BodyPublisher publisher) {
		StringBuilder collected = new StringBuilder();
		CountDownLatch completed = new CountDownLatch(1);
		publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ByteBuffer item) {
				byte[] bytes = new byte[item.remaining()];
				item.get(bytes);
				// Latin-1 round-trips arbitrary bytes, so uploaded image content
				// survives alongside the text parts of a multipart body.
				collected.append(new String(bytes, StandardCharsets.ISO_8859_1));
			}

			@Override
			public void onError(Throwable throwable) {
				completed.countDown();
			}

			@Override
			public void onComplete() {
				completed.countDown();
			}
		});
		try {
			completed.await(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return collected.toString();
	}

	@Override
	public Optional<CookieHandler> cookieHandler() {
		return Optional.empty();
	}

	@Override
	public Optional<Duration> connectTimeout() {
		return Optional.empty();
	}

	@Override
	public Redirect followRedirects() {
		return Redirect.NEVER;
	}

	@Override
	public Optional<ProxySelector> proxy() {
		return Optional.empty();
	}

	@Override
	public SSLContext sslContext() {
		return null;
	}

	@Override
	public SSLParameters sslParameters() {
		return new SSLParameters();
	}

	@Override
	public Optional<Authenticator> authenticator() {
		return Optional.empty();
	}

	@Override
	public Version version() {
		return Version.HTTP_1_1;
	}

	@Override
	public Optional<Executor> executor() {
		return Optional.empty();
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			HttpResponse.BodyHandler<T> responseBodyHandler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
		throw new UnsupportedOperationException();
	}

	record StubResponse(int statusCode, String body, Map<String, List<String>> headers) {

		StubResponse(int statusCode, String body) {
			this(statusCode, body, Map.of());
		}
	}

	record RecordedRequest(HttpRequest request, String body) {

		String method() {
			return request.method();
		}

		String uri() {
			return request.uri().toString();
		}
	}

	private record StubHttpResponse(StubResponse response, URI uri) implements HttpResponse<String> {

		@Override
		public int statusCode() {
			return response.statusCode();
		}

		@Override
		public HttpRequest request() {
			return null;
		}

		@Override
		public Optional<HttpResponse<String>> previousResponse() {
			return Optional.empty();
		}

		@Override
		public HttpHeaders headers() {
			return HttpHeaders.of(response.headers(), (name, value) -> true);
		}

		@Override
		public String body() {
			return response.body();
		}

		@Override
		public Optional<SSLSession> sslSession() {
			return Optional.empty();
		}

		@Override
		public URI uri() {
			return uri;
		}

		@Override
		public Version version() {
			return Version.HTTP_1_1;
		}

	}

}
