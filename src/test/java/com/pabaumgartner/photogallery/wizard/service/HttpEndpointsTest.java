package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpEndpointsTest {

	private final List<String> received = new ArrayList<>();

	private final HttpClient client = HttpEndpoints.newClient();

	private HttpServer server;

	private String baseUrl;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://example.com/upload.php", "https://gallery.example.com:8443/api",
			"http://localhost:8080/api", "http://127.0.0.1/api", "http://[::1]/api", "http://10.0.0.5/api",
			"http://172.16.4.9/api", "http://192.168.1.10:3000/api", "http://gallery.local/api",
			"http://pics.localhost/api" })
	void credentialsMayBeSentOverHttpsOrToTheLocalNetwork(String url) {
		assertThatCode(() -> HttpEndpoints.requireCredentialSafeTransport(URI.create(url))).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = { "http://example.com/upload.php", "http://8.8.8.8/api", "http://gallery.example.com/api" })
	void credentialsAreNeverSentInClearTextToAPublicHost(String url) {
		assertThatThrownBy(() -> HttpEndpoints.requireCredentialSafeTransport(URI.create(url)))
			.isInstanceOf(IOException.class)
			.hasMessageContaining("Refusing to send credentials");
	}

	@Test
	void multipartFilenamesCannotInjectHeaderLines() {
		assertThat(HttpEndpoints.sanitizeMultipartFilename("photo.jpg")).isEqualTo("photo.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\"b.jpg")).isEqualTo("a_b.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\r\nContent-Type: evil\r\n\r\n.jpg"))
			.isEqualTo("a__Content-Type: evil____.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\\b.jpg")).isEqualTo("a_b.jpg");
	}

	@Test
	void loggedBodiesAreCollapsedAndTruncated() {
		assertThat(HttpEndpoints.truncateForLog(null)).isEqualTo("<empty>");
		assertThat(HttpEndpoints.truncateForLog("   ")).isEqualTo("<empty>");
		assertThat(HttpEndpoints.truncateForLog("line one\n  line two")).isEqualTo("line one line two");
		assertThat(HttpEndpoints.truncateForLog("x".repeat(500))).hasSize(201).endsWith("…");
	}

	@Test
	void restoreInterruptFlagOnlyReactsToInterruption() {
		HttpEndpoints.restoreInterruptFlag(new IOException("boom"));
		assertThat(Thread.currentThread().isInterrupted()).isFalse();

		HttpEndpoints.restoreInterruptFlag(new InterruptedException("stop"));
		assertThat(Thread.interrupted()).isTrue();
	}

	@Test
	void clientIsConfiguredWithAConnectTimeout() {
		assertThat(HttpEndpoints.newClient().connectTimeout()).contains(HttpEndpoints.CONNECT_TIMEOUT);
	}

	@Test
	void clientDoesNotFollowRedirectsOnItsOwn() {
		assertThat(HttpEndpoints.newClient().followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
	}

	@Test
	void responseBodiesAreReturnedWhenTheyFitTheLimit() throws Exception {
		serve("/ok", exchange -> respond(exchange, 200, "{\"token\":\"abc\"}"));

		HttpResponse<String> response = HttpEndpoints.send(client, post("/ok", "payload"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("{\"token\":\"abc\"}");
	}

	@Test
	void oversizedResponseBodiesAreRefusedInsteadOfBuffered() {
		serve("/flood", exchange -> {
			exchange.sendResponseHeaders(200, 0);
			byte[] megabyte = new byte[1024 * 1024];
			try (OutputStream body = exchange.getResponseBody()) {
				for (int i = 0; i <= HttpEndpoints.MAX_RESPONSE_BODY_BYTES / megabyte.length; i++) {
					body.write(megabyte);
				}
			}
			catch (IOException ex) {
				// Expected: the client cancels once the cap is reached.
			}
		});

		assertThatThrownBy(() -> HttpEndpoints.send(client, post("/flood", "payload"))).isInstanceOf(IOException.class)
			.hasMessageContaining("exceeds the " + HttpEndpoints.MAX_RESPONSE_BODY_BYTES + " byte limit");
	}

	@Test
	void permanentRedirectsReplayTheMethodAndBodyToTheNewLocation() throws Exception {
		serve("/moved", exchange -> redirect(exchange, 308, "/final"));
		serve("/final", exchange -> respond(exchange, 200, "done"));

		HttpResponse<String> response = HttpEndpoints.send(client, post("/moved", "payload"));

		assertThat(response.body()).isEqualTo("done");
		assertThat(received).containsExactly("POST /moved payload", "POST /final payload");
	}

	@Test
	void seeOtherRedirectsDropTheBodyAndBecomeAGet() throws Exception {
		serve("/legacy", exchange -> redirect(exchange, 303, "/landing"));
		serve("/landing", exchange -> respond(exchange, 200, "done"));

		HttpResponse<String> response = HttpEndpoints.send(client, post("/legacy", "payload"));

		assertThat(response.body()).isEqualTo("done");
		assertThat(received).containsExactly("POST /legacy payload", "GET /landing ");
	}

	@Test
	void redirectsToAClearTextPublicHostAreRefused() {
		serve("/leak", exchange -> redirect(exchange, 308, "http://example.com/collect"));

		assertThatThrownBy(() -> HttpEndpoints.send(client, post("/leak", "payload"))).isInstanceOf(IOException.class)
			.hasMessageContaining("Refusing to send credentials");
	}

	@Test
	void redirectLoopsAreAbandoned() {
		serve("/loop", exchange -> redirect(exchange, 308, "/loop"));

		assertThatThrownBy(() -> HttpEndpoints.send(client, post("/loop", "payload"))).isInstanceOf(IOException.class)
			.hasMessageContaining("Gave up after " + HttpEndpoints.MAX_REDIRECTS + " redirects");
	}

	private HttpRequest post(String path, String body) {
		return HttpRequest.newBuilder()
			.uri(URI.create(baseUrl + path))
			.timeout(HttpEndpoints.API_TIMEOUT)
			.header("Content-Type", "text/plain")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
	}

	private void serve(String path, ThrowingHandler handler) {
		server.createContext(path, exchange -> {
			try (exchange) {
				received.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
						+ new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				handler.handle(exchange);
			}
		});
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	private static void redirect(HttpExchange exchange, int status, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(status, -1);
	}

	@FunctionalInterface
	private interface ThrowingHandler {

		void handle(HttpExchange exchange) throws IOException;

	}

}
