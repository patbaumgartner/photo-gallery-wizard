package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSession;

import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PicPeakServiceTest {

	@TempDir
	Path tempDir;

	private PicPeakProperties disabledProperties;

	private PicPeakProperties enabledProperties;

	private CodeGeneratorService codeGeneratorService;

	private static PicPeakProperties testProperties(boolean enabled, String apiUrl, String eventDate,
			Integer uploadCategoryId, Integer klassenfotoCategoryId, Integer portraitCategoryId) {
		return new PicPeakProperties(enabled, true, apiUrl, "user", "pass", "schulfotos", eventDate, "test@example.com",
				"admin@example.com", true, "Welcome", 30, false, false, false, false, false, false, false, false, false,
				false, false, false, false, false, false, "standard", "wave", 1, "default", "standard", "managed",
				"center", "medium", "top", uploadCategoryId, klassenfotoCategoryId, portraitCategoryId, null, null, 0);
	}

	private static SchulfotosProperties defaultSchulfotosProperties() {
		return new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null, null,
				0);
	}

	@BeforeEach
	void setUp() {
		disabledProperties = testProperties(false, "https://api.example.com", "", null, null, null);
		enabledProperties = testProperties(true, "https://api.example.com/", "2024-01-15", 5, 6, 7);
		codeGeneratorService = new CodeGeneratorService(defaultSchulfotosProperties());
	}

	@Test
	void enrichWithShareLinksReturnsSameCodesWhenDisabled() {
		PicPeakService service = new PicPeakService(disabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(200, "{}"));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"),
				new GalleryCode("ABCD-5678-STUV", "pass2"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Test Event");
		assertThat(result).isEqualTo(codes);
	}

	@Test
	void enrichWithShareLinksReturnsUnchangedWhenLoginFails() {
		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(401, "Unauthorized"));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Test Event");
		assertThat(result).isEqualTo(codes);
	}

	@Test
	void enrichWithShareLinksExtractsShareLinkFromResponse() {
		List<StubResponse> responses = new ArrayList<>();
		// Login response - return token in Set-Cookie
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		// Event creation response - return share_link
		responses.add(new StubResponse(201, "{\"share_link\":\"https://share.example.com/abc\",\"id\":42}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Test Event");
		assertThat(result).hasSize(1);
		assertThat(result.get(0).shareUrl()).isEqualTo("https://share.example.com/abc");
		assertThat(result.get(0).picPeakEventId()).isEqualTo(42);
	}

	@Test
	void enrichWithShareLinksThrowsWhenEventCreationFailsAfterRetries() {
		List<StubResponse> responses = new ArrayList<>();
		// Login response
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		// 3 failed event creation attempts (returns empty share_link)
		responses.add(new StubResponse(200, "{\"share_link\":\"\"}", Map.of()));
		responses.add(new StubResponse(200, "{\"share_link\":\"\"}", Map.of()));
		responses.add(new StubResponse(200, "{\"share_link\":\"\"}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		assertThatThrownBy(() -> service.enrichWithShareLinks(codes, "Test Event"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to create PicPeak event");
	}

	@Test
	void enrichWithShareLinksExtractsTokenFromCookie() {
		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{}",
				Map.of("Set-Cookie", List.of("admin_token=cookietoken123; Path=/; HttpOnly"))));
		responses.add(new StubResponse(201, "{\"share_link\":\"https://share.example.com/x\",\"id\":1}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Test Event");
		assertThat(result).hasSize(1);
		assertThat(result.get(0).shareUrl()).isEqualTo("https://share.example.com/x");
	}

	@Test
	void uploadEventPhotosDisabled() {
		PicPeakService service = new PicPeakService(disabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(200, "{}"));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1", "url", 42));
		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);
		assertThat(result.galleriesUpdated()).isEqualTo(0);
		assertThat(result.totalFilesUploaded()).isEqualTo(0);
		assertThat(result.errors()).isEmpty();
	}

	@Test
	void uploadEventPhotosDisabledReportsCompletedProgress() {
		PicPeakService service = new PicPeakService(disabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(200, "{}"));
		AtomicReference<PicPeakService.UploadProgress> lastProgress = new AtomicReference<>();
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1", "url", 42));
		service.uploadEventPhotos(tempDir, codes, lastProgress::set);
		assertThat(lastProgress.get()).isNotNull();
		assertThat(lastProgress.get().percent()).isEqualTo(1.0d);
	}

	@Test
	void uploadEventPhotosLoginFailure() {
		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(401, "Unauthorized"));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1", "url", 42));
		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);
		assertThat(result.galleriesUpdated()).isEqualTo(0);
		assertThat(result.errors()).contains("Login failed");
	}

	@Test
	void uploadEventPhotosSkipsCodesWithoutPicPeakId() throws IOException {
		List<StubResponse> responses = new ArrayList<>();
		// Login
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);
		assertThat(result.galleriesUpdated()).isEqualTo(0);
		assertThat(result.totalFilesUploaded()).isEqualTo(0);
	}

	@Test
	void uploadEventPhotosUploadsKlassenfotosAndPortraits() throws IOException {
		Path klassen = tempDir.resolve("klassenfotos-watermarked");
		Path portrait = tempDir.resolve("portrait-1-watermarked");
		Files.createDirectories(klassen);
		Files.createDirectories(portrait);
		Files.writeString(klassen.resolve("class.jpg"), "jpeg-bytes");
		Files.writeString(portrait.resolve("p1.png"), "png-bytes");

		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		responses.add(new StubResponse(200, "{}", Map.of()));
		responses.add(new StubResponse(201, "{}", Map.of()));
		responses.add(new StubResponse(201, "{}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1", "url", 42));

		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);

		assertThat(result.errors()).isEmpty();
		assertThat(result.galleriesUpdated()).isEqualTo(1);
		assertThat(result.totalFilesUploaded()).isEqualTo(2);
	}

	@Test
	void uploadEventPhotosUsesActiveGalleryIndexAfterSkippedCodes() throws IOException {
		Path portrait = tempDir.resolve("portrait-1-watermarked");
		Files.createDirectories(portrait);
		Files.writeString(portrait.resolve("p1.jpg"), "jpeg-bytes");

		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		responses.add(new StubResponse(200, "{}", Map.of()));
		responses.add(new StubResponse(201, "{}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1111-AAAA", "pass0"),
				new GalleryCode("ABCD-2222-BBBB", "pass1", "url", 77));

		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);

		assertThat(result.errors()).isEmpty();
		assertThat(result.galleriesUpdated()).isEqualTo(1);
		assertThat(result.totalFilesUploaded()).isEqualTo(1);
	}

	@Test
	void uploadEventPhotosFallsBackToPerPhotoDeletionWhenBulkClearFails() {
		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		responses.add(new StubResponse(500, "bulk-failed", Map.of()));
		responses.add(new StubResponse(500, "clear-failed", Map.of()));
		responses.add(new StubResponse(200,
				"{\"photos\":[{\"id\":11,\"event_id\":42,\"filename\":\"a.jpg\"},{\"id\":12,\"event_id\":42,\"filename\":\"b.jpg\"}]}",
				Map.of()));
		responses.add(new StubResponse(204, "", Map.of()));
		responses.add(new StubResponse(204, "", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1", "url", 42));

		PicPeakService.UploadResult result = service.uploadEventPhotos(tempDir, codes);

		assertThat(result.errors()).isEmpty();
		assertThat(result.galleriesUpdated()).isEqualTo(0);
		assertThat(result.totalFilesUploaded()).isEqualTo(0);
	}

	@Test
	void uploadResultRecordAccessors() {
		PicPeakService.UploadResult result = new PicPeakService.UploadResult(3, 15, List.of("error1", "error2"));
		assertThat(result.galleriesUpdated()).isEqualTo(3);
		assertThat(result.totalFilesUploaded()).isEqualTo(15);
		assertThat(result.errors()).hasSize(2);
	}

	@Test
	void uploadProgressRecordAccessors() {
		PicPeakService.UploadProgress progress = new PicPeakService.UploadProgress(0.75d, "Uploading");
		assertThat(progress.percent()).isEqualTo(0.75d);
		assertThat(progress.stage()).isEqualTo("Uploading");
	}

	@Test
	void enrichWithShareLinksMultipleCodes() {
		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		responses.add(new StubResponse(201, "{\"share_link\":\"https://share/1\",\"id\":10}", Map.of()));
		responses.add(new StubResponse(201, "{\"share_link\":\"https://share/2\",\"id\":20}", Map.of()));
		responses.add(new StubResponse(201, "{\"share_link\":\"https://share/3\",\"id\":30}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"),
				new GalleryCode("ABCD-5678-STUV", "pass2"), new GalleryCode("ABCD-9012-MNOP", "pass3"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Event");
		assertThat(result).hasSize(3);
		assertThat(result.get(0).picPeakEventId()).isEqualTo(10);
		assertThat(result.get(1).picPeakEventId()).isEqualTo(20);
		assertThat(result.get(2).picPeakEventId()).isEqualTo(30);
	}

	@Test
	void enrichWithShareLinksUsesEventIdField() {
		List<StubResponse> responses = new ArrayList<>();
		responses.add(new StubResponse(200, "{\"token\":\"abc123\"}", Map.of()));
		responses.add(new StubResponse(200, "{\"share_link\":\"https://share/1\",\"event_id\":55}", Map.of()));

		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, sequentialHttpClient(responses));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Event");
		assertThat(result.get(0).picPeakEventId()).isEqualTo(55);
	}

	@Test
	void enrichWithShareLinksOverrideDisabled() {
		PicPeakService service = new PicPeakService(enabledProperties, defaultSchulfotosProperties(),
				codeGeneratorService, stubHttpClient(200, "{}"));
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pass1"));
		List<GalleryCode> result = service.enrichWithShareLinks(codes, "Event", false, null);
		assertThat(result).isEqualTo(codes);
	}

	@Test
	void apiUrlTrailingSlashStrippedInProperties() {
		assertThat(enabledProperties.apiUrl()).isEqualTo("https://api.example.com");
	}

	// --- Stub HttpClient helpers ---

	private static HttpClient stubHttpClient(int statusCode, String body) {
		return sequentialHttpClient(List.of(new StubResponse(statusCode, body, Map.of())));
	}

	private static HttpClient sequentialHttpClient(List<StubResponse> responses) {
		int[] index = { 0 };
		return new HttpClient() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
					throws IOException, InterruptedException {
				StubResponse stub;
				synchronized (index) {
					stub = index[0] < responses.size() ? responses.get(index[0]++)
							: responses.get(responses.size() - 1);
				}
				return (HttpResponse<T>) new StubHttpResponse(stub.statusCode, stub.body, stub.headers, request.uri());
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
		};
	}

	record StubResponse(int statusCode, String body, Map<String, List<String>> headers) {
		StubResponse(int statusCode, String body, Map<String, List<String>> headers) {
			this.statusCode = statusCode;
			this.body = body;
			this.headers = headers != null ? headers : Map.of();
		}
	}

	static class StubHttpResponse implements HttpResponse<String> {

		private final int statusCode;

		private final String body;

		private final Map<String, List<String>> headers;

		private final java.net.URI uri;

		StubHttpResponse(int statusCode, String body, Map<String, List<String>> headers, java.net.URI uri) {
			this.statusCode = statusCode;
			this.body = body;
			this.headers = headers;
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
			return HttpHeaders.of(headers, (a, b) -> true);
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
		public java.net.URI uri() {
			return uri;
		}

		@Override
		public HttpClient.Version version() {
			return HttpClient.Version.HTTP_1_1;
		}

	}

}
