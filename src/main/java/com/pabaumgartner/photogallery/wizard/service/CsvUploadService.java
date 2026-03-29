package com.pabaumgartner.photogallery.wizard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
		this(schulfotosProperties, picPeakProperties, HttpClient.newHttpClient());
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

		String filename = csvPath.getFileName().toString();
		String uploadUrl = baseUrl + "/upload.php";

		byte[] fileBytes = Files.readAllBytes(csvPath);

		String boundary = "----CsvUpload" + UUID.randomUUID().toString().replace("-", "");

		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writePart(body, boundary, "username", username);
		writePart(body, boundary, "password", password);
		body.write(("--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename
				+ "\"\r\n" + "Content-Type: text/csv; charset=UTF-8\r\n\r\n")
			.getBytes(StandardCharsets.UTF_8));
		body.write(fileBytes);
		body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uploadUrl))
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				LOGGER.atInfo().addArgument(uploadUrl).log("CSV uploaded successfully to {}");
			}
			else {
				LOGGER.error("CSV upload failed with status {}: {}", response.statusCode(), response.body());
				throw new IOException("CSV upload to " + uploadUrl + " failed with status " + response.statusCode());
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("CSV upload interrupted", ex);
		}
	}

	private void writePart(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
		out.write(("--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value
				+ "\r\n")
			.getBytes(StandardCharsets.UTF_8));
	}

}
