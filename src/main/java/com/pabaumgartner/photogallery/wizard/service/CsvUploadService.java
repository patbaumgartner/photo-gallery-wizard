package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CsvUploadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvUploadService.class);

	private final String baseUrl;

	private final String username;

	private final String password;

	private final HttpClient httpClient;

	@Autowired
	public CsvUploadService(SchulfotosProperties schulfotosProperties, PicPeakProperties picPeakProperties) {
		this(schulfotosProperties, picPeakProperties, HttpEndpoints.newClient());
	}

	CsvUploadService(SchulfotosProperties schulfotosProperties, PicPeakProperties picPeakProperties,
			HttpClient httpClient) {
		this.baseUrl = schulfotosProperties.baseUrl();
		this.username = picPeakProperties.username();
		this.password = picPeakProperties.password();
		this.httpClient = httpClient;
	}

	public void upload(Path csvPath) throws IOException {
		if (username.isBlank() || password.isBlank()) {
			LOGGER.warn("CSV upload skipped: credentials not configured");
			return;
		}

		URI uploadUri = URI.create(baseUrl + "/upload.php");
		HttpEndpoints.requireCredentialSafeTransport(uploadUri);

		String filename = HttpEndpoints.sanitizeMultipartFilename(csvPath.getFileName().toString());
		String boundary = "----CsvUpload" + UUID.randomUUID().toString().replace("-", "");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(uploadUri)
			.timeout(HttpEndpoints.UPLOAD_TIMEOUT)
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.concat(formField(boundary, "username", username),
					formField(boundary, "password", password),
					HttpRequest.BodyPublishers.ofString(
							"--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\""
									+ filename + "\"\r\n" + "Content-Type: text/csv; charset=UTF-8\r\n\r\n",
							StandardCharsets.UTF_8),
					HttpRequest.BodyPublishers.ofFile(csvPath),
					HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n", StandardCharsets.UTF_8)))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				LOGGER.atInfo().addArgument(uploadUri).log("CSV uploaded successfully to {}");
			}
			else {
				LOGGER.error("CSV upload failed with status {}: {}", response.statusCode(),
						HttpEndpoints.truncateForLog(response.body()));
				throw new IOException("CSV upload to " + uploadUri + " failed with status " + response.statusCode());
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("CSV upload interrupted", ex);
		}
	}

	private static HttpRequest.BodyPublisher formField(String boundary, String name, String value) {
		return HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\""
				+ name + "\"\r\n\r\n" + value + "\r\n", StandardCharsets.UTF_8);
	}

}
