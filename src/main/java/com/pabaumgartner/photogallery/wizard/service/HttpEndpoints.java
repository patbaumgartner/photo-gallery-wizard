package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

final class HttpEndpoints {

	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	static final Duration API_TIMEOUT = Duration.ofSeconds(30);

	static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);

	private static final Pattern IP_LITERAL = Pattern.compile("^\\[?[0-9a-f.:]+]?$");

	private static final int MAX_LOGGED_BODY_LENGTH = 200;

	private HttpEndpoints() {
	}

	static HttpClient newClient() {
		return HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
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

}
