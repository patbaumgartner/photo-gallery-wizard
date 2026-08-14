package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

		FakeHttpClient client = FakeHttpClient.replying(201, "Created");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user@test.com", "secret"), client);

		service.upload(csvFile);

		assertThat(client.recorded()).hasSize(1);
	}

	@Test
	void uploadIncludesCredentialsAsFormFields() throws IOException {
		Path csvFile = tempDir.resolve("creds.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user@test.com", "s3cret"), client);

		service.upload(csvFile);

		assertThat(client.recorded()).hasSize(1);
		String body = client.recorded().getFirst().body();
		assertThat(body).contains("name=\"username\"");
		assertThat(body).contains("user@test.com");
		assertThat(body).contains("name=\"password\"");
		assertThat(body).contains("s3cret");
	}

	@Test
	void uploadUsesCorrectUrl() throws IOException {
		Path csvFile = tempDir.resolve("my-codes.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.recorded().getFirst().uri()).isEqualTo("https://example.com/schulfotos/upload.php");
	}

	@Test
	void uploadUsesPutMethod() throws IOException {
		Path csvFile = tempDir.resolve("method-test.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.recorded().getFirst().method()).isEqualTo("POST");
	}

	@Test
	void uploadThrowsOnFailureStatus() throws IOException {
		Path csvFile = tempDir.resolve("fail-test.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(403, "Forbidden");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		assertThatThrownBy(() -> service.upload(csvFile)).isInstanceOf(IOException.class)
			.hasMessageContaining("failed with status 403");
	}

	@Test
	void uploadSkipsWhenNoCredentials() throws IOException {
		Path csvFile = tempDir.resolve("nocreds.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("", ""), client);

		service.upload(csvFile);

		assertThat(client.recorded()).isEmpty();
	}

	@Test
	void uploadSetsContentTypeHeader() throws IOException {
		Path csvFile = tempDir.resolve("content-type.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("https://example.com/schulfotos"),
				picPeakProperties("user", "pass"), client);

		service.upload(csvFile);

		assertThat(client.recorded().getFirst().request().headers().firstValue("Content-Type"))
			.hasValueSatisfying(contentType -> assertThat(contentType).startsWith("multipart/form-data; boundary="));
	}

	@Test
	void uploadRefusesToSendCredentialsOverPlainHttpToAPublicHost() throws IOException {
		Path csvFile = tempDir.resolve("insecure.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		FakeHttpClient client = FakeHttpClient.replying(200, "OK");
		CsvUploadService service = new CsvUploadService(schulfotosProperties("http://example.com/schulfotos"),
				picPeakProperties("user@test.com", "secret"), client);

		assertThatThrownBy(() -> service.upload(csvFile)).isInstanceOf(IOException.class)
			.hasMessageContaining("Refusing to send credentials");
		assertThat(client.recorded()).isEmpty();
	}

	@Test
	void uploadAllowsPlainHttpOnLoopbackAndPrivateNetworks() throws IOException {
		Path csvFile = tempDir.resolve("lan.csv");
		Files.writeString(csvFile, "data", StandardCharsets.UTF_8);

		for (String baseUrl : List.of("http://localhost:8080", "http://127.0.0.1:8080", "http://192.168.1.10",
				"http://gallery.local")) {
			FakeHttpClient client = FakeHttpClient.replying(200, "OK");
			CsvUploadService service = new CsvUploadService(schulfotosProperties(baseUrl),
					picPeakProperties("user@test.com", "secret"), client);

			service.upload(csvFile);

			assertThat(client.recorded()).as("expected %s to be accepted", baseUrl).hasSize(1);
		}
	}

}
