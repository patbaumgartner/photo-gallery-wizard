package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSession;

import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvUploadServiceTest {

	@TempDir
	Path tempDir;

	private static SchulfotosProperties schulfotosProperties(String baseUrl) {
		return new SchulfotosProperties(baseUrl, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null,
				null, 0);
	}

	private static PicPeakProperties picPeakProperties(String username, String password) {
		return new PicPeakProperties(false, true, null, username, password, null, null, null, null, false, null, 0,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
				null, null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	@Test
	void uploadSendsRequest() throws IOException {
		Path csvFile = tempDir.resolve("test-codes.csv");
		Files.writeString(csvFile, "Number,Code\n1,ABCD-1234-WXYZ", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(201, "Created");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user@test.com", "secret"), client);

		service.upload(csvFile);

		assertThat(client.requests).hasSize(1);
	}

	@Test
	void uploadIncludesCredentialsAsFormFields() throws IOException {
		Path csvFile = tempDir.resolve("creds.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingBodyHttpClient client = new RecordingBodyHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user@test.com", "s3cret"), client);

		service.upload(csvFile);

		assertThat(client.bodies).hasSize(1);
		String body = new String(client.bodies.get(0), StandardCharsets.UTF_8);
		assertThat(body).contains("name=\"username\"");
		assertThat(body).contains("user@test.com");
		assertThat(body).contains("name=\"password\"");
		assertThat(body).contains("s3cret");
	}

	@Test
	void uploadUsesCorrectUrl() throws IOException {
		Path csvFile = tempDir.resolve("my-codes.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.requests.get(0).uri().toString()).isEqualTo("https://example.com/schulfotos/upload.php");
	}

	@Test
	void uploadUsesPutMethod() throws IOException {
		Path csvFile = tempDir.resolve("method-test.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.requests.get(0).method()).isEqualTo("POST");
	}

	@Test
	void uploadThrowsOnFailureStatus() throws IOException {
		Path csvFile = tempDir.resolve("fail-test.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(403, "Forbidden");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		assertThatThrownBy(() -> service.upload(csvFile)).isInstanceOf(IOException.class)
			.hasMessageContaining("failed with status 403");
	}

	@Test
	void uploadSkipsWhenNoCredentials() throws IOException {
		Path csvFile = tempDir.resolve("nocreds.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("", ""), client);

		service.upload(csvFile);

		assertThat(client.requests).isEmpty();
	}

	@Test
	void uploadSetsContentTypeHeader() throws IOException {
		Path csvFile = tempDir.resolve("content-type.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.requests.get(0).headers().firstValue("Content-Type")).isPresent();
		assertThat(client.requests.get(0).headers().firstValue("Content-Type").get())
			.startsWith("multipart/form-data; boundary=");
	}

	@Test
	void uploadRefusesToSendCredentialsOverPlainHttpToAPublicHost() throws IOException {
		Path csvFile = tempDir.resolve("insecure.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		RecordingHttpClient client = new RecordingHttpClient(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("http://example.com/schulfotos"),
				picPeakProperties("user@test.com", "secret"), client);

		assertThatThrownBy(() -> service.upload(csvFile)).isInstanceOf(IOException.class)
			.hasMessageContaining("Refusing to send credentials");
		assertThat(client.requests).isEmpty();
	}

	@Test
	void uploadAllowsPlainHttpOnLoopbackAndPrivateNetworks() throws IOException {
		Path csvFile = tempDir.resolve("lan.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		for (String baseUrl : List.of("http://localhost:8080", "http://127.0.0.1:8080", "http://192.168.1.10",
				"http://gallery.local")) {
			RecordingHttpClient client = new RecordingHttpClient(200, "OK");
			CsvUploadService service = new CsvUploadService(schulfotosProperties(baseUrl),
					picPeakProperties("user@test.com", "secret"), client);

			service.upload(csvFile);

			assertThat(client.requests).as("expected %s to be accepted", baseUrl).hasSize(1);
		}
	}

	// --- Stub HttpClient ---

	static class RecordingHttpClient extends HttpClient {

		final int statusCode;

		final String body;

		final List<HttpRequest> requests = new ArrayList<>();

		RecordingHttpClient(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
				throws IOException, InterruptedException {
			requests.add(request);
			return (HttpResponse<T>) new StubHttpResponse(statusCode, body, request.uri());
		}

		@Override
		public java.util.Optional<java.net.CookieHandler> cookieHandler() {
			return java.util.Optional.empty();
		}

		@Override
		public java.util.Optional<java.time.Duration> connectTimeout() {
			return java.util.Optional.empty();
		}

		@Override
		public Redirect followRedirects() {
			return Redirect.NEVER;
		}

		@Override
		public java.util.Optional<java.net.ProxySelector> proxy() {
			return java.util.Optional.empty();
		}

		@Override
		public javax.net.ssl.SSLContext sslContext() {
			return null;
		}

		@Override
		public javax.net.ssl.SSLParameters sslParameters() {
			return new javax.net.ssl.SSLParameters();
		}

		@Override
		public java.util.Optional<java.net.Authenticator> authenticator() {
			return java.util.Optional.empty();
		}

		@Override
		public Version version() {
			return Version.HTTP_1_1;
		}

		@Override
		public java.util.Optional<java.util.concurrent.Executor> executor() {
			return java.util.Optional.empty();
		}

		@Override
		public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
				HttpResponse.BodyHandler<T> responseBodyHandler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
				HttpResponse.BodyHandler<T> responseBodyHandler,
				HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
			throw new UnsupportedOperationException();
		}

	}

	static class StubHttpResponse implements HttpResponse<String> {

		private final int statusCode;

		private final String body;

		private final URI uri;

		StubHttpResponse(int statusCode, String body, URI uri) {
			this.statusCode = statusCode;
			this.body = body;
			this.uri = uri;
		}

		@Override
		public int statusCode() {
			return statusCode;
		}

		@Override
		public HttpRequest request() {
			return null;
		}

		@Override
		public java.util.Optional<HttpResponse<String>> previousResponse() {
			return java.util.Optional.empty();
		}

		@Override
		public HttpHeaders headers() {
			return HttpHeaders.of(Map.of(), (a, b) -> true);
		}

		@Override
		public String body() {
			return body;
		}

		@Override
		public java.util.Optional<SSLSession> sslSession() {
			return java.util.Optional.empty();
		}

		@Override
		public URI uri() {
			return uri;
		}

		@Override
		public HttpClient.Version version() {
			return HttpClient.Version.HTTP_1_1;
		}

	}

	static class RecordingBodyHttpClient extends RecordingHttpClient {

		final List<byte[]> bodies = new ArrayList<>();

		RecordingBodyHttpClient(int statusCode, String responseBody) {
			super(statusCode, responseBody);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
				throws IOException, InterruptedException {
			requests.add(request);
			request.bodyPublisher().ifPresent(pub -> {
				java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
				pub.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
					@Override
					public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
						subscription.request(Long.MAX_VALUE);
					}

					@Override
					public void onNext(java.nio.ByteBuffer item) {
						byte[] data = new byte[item.remaining()];
						item.get(data);
						collected.writeBytes(data);
					}

					@Override
					public void onError(Throwable throwable) {
					}

					@Override
					public void onComplete() {
					}
				});
				bodies.add(collected.toByteArray());
			});
			return (HttpResponse<T>) new StubHttpResponse(statusCode, body, request.uri());
		}

	}

}
