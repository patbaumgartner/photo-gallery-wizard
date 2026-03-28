package com.pabaumgartner.photogallery.wizard.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PicPeakService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PicPeakService.class);

	private final int maxPasswordRetries;

	private final String klassenfotoFolder;

	private final String portraitPrefix;

	private final String watermarkedSuffix;

	record EventCreationResult(String shareLink, int eventId) {
	}

	public record UploadProgress(double percent, String stage) {
	}

	private final PicPeakProperties picPeakProperties;

	private final CodeGeneratorService codeGeneratorService;

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	@Autowired
	public PicPeakService(PicPeakProperties picPeakProperties, SchulfotosProperties schulfotosProperties,
			CodeGeneratorService codeGeneratorService) {
		this(picPeakProperties, schulfotosProperties, codeGeneratorService, HttpClient.newHttpClient());
	}

	PicPeakService(PicPeakProperties picPeakProperties, SchulfotosProperties schulfotosProperties,
			CodeGeneratorService codeGeneratorService, HttpClient httpClient) {
		this.picPeakProperties = picPeakProperties;
		this.codeGeneratorService = codeGeneratorService;
		this.objectMapper = new ObjectMapper();
		this.httpClient = httpClient;
		this.maxPasswordRetries = picPeakProperties.maxPasswordRetries();
		this.klassenfotoFolder = schulfotosProperties.klassenfotoFolder();
		this.portraitPrefix = schulfotosProperties.portraitPrefix();
		this.watermarkedSuffix = schulfotosProperties.watermarkedSuffix();
	}

	public List<GalleryCode> enrichWithShareLinks(List<GalleryCode> codes, String eventName) {
		return enrichWithShareLinks(codes, eventName, picPeakProperties.enabled(), picPeakProperties.eventDate());
	}

	public List<GalleryCode> enrichWithShareLinks(List<GalleryCode> codes, String eventName, boolean enabled,
			String eventDateOverride) {
		if (!enabled) {
			return codes;
		}

		LOGGER.atInfo()
			.addArgument(() -> codes.size())
			.log("PicPeak integration enabled. Creating {} gallery events...");

		String token = login();
		if (token == null) {
			LOGGER.error("PicPeak login failed. Returning codes without share links.");
			return codes;
		}

		List<GalleryCode> enrichedCodes = new ArrayList<>(codes.size());
		int number = 1;
		for (GalleryCode code : codes) {
			GalleryCode currentCode = code;
			EventCreationResult result = null;
			for (int attempt = 1; attempt <= maxPasswordRetries; attempt++) {
				result = createEvent(token, currentCode, eventName, number, eventDateOverride);
				if (result != null && !result.shareLink().isBlank()) {
					break;
				}
				result = null;
				if (attempt < maxPasswordRetries) {
					String newPassword = codeGeneratorService.generatePassword();
					currentCode = new GalleryCode(currentCode.code(), newPassword);
					LOGGER.warn("Retrying PicPeak event #{} with a new password (attempt {}/{})", number, attempt + 1,
							maxPasswordRetries);
				}
			}
			if (result != null) {
				enrichedCodes.add(new GalleryCode(currentCode.code(), currentCode.password(), result.shareLink(),
						result.eventId()));
				LOGGER.atInfo()
					.addArgument(number)
					.addArgument(result.shareLink())
					.log("Created PicPeak event #{}: {}");
			}
			else {
				throw new IllegalStateException(
						"Failed to create PicPeak event for code #" + number + " (" + code.code() + ") after "
								+ maxPasswordRetries + " attempts. Aborting to prevent wrong URLs in the CSV.");
			}
			number++;
		}

		return enrichedCodes;
	}

	public UploadResult uploadEventPhotos(Path eventDir, List<GalleryCode> codes) {
		return uploadEventPhotos(eventDir, codes, progress -> {
		});
	}

	public UploadResult uploadEventPhotos(Path eventDir, List<GalleryCode> codes,
			Consumer<UploadProgress> progressListener) {
		if (!picPeakProperties.enabled()) {
			progressListener.accept(new UploadProgress(1.0d, "PicPeak deaktiviert"));
			return new UploadResult(0, 0, List.of());
		}

		progressListener.accept(new UploadProgress(0.05d, "PicPeak Login"));

		String token = login();
		if (token == null) {
			LOGGER.error("PicPeak login failed. Cannot upload photos.");
			progressListener.accept(new UploadProgress(1.0d, "Login fehlgeschlagen"));
			return new UploadResult(0, 0, List.of("Login failed"));
		}

		Path klassenfotosWatermarked = eventDir.resolve(klassenfotoFolder + "s" + watermarkedSuffix);
		List<Path> klassenfotos = listImageFiles(klassenfotosWatermarked);

		Integer klassenfotoCategoryId = picPeakProperties.klassenfotoCategoryId() != null
				? picPeakProperties.klassenfotoCategoryId() : picPeakProperties.uploadCategoryId();
		Integer portraitCategoryId = picPeakProperties.portraitCategoryId() != null
				? picPeakProperties.portraitCategoryId() : picPeakProperties.uploadCategoryId();

		int galleriesUpdated = 0;
		int totalFilesUploaded = 0;
		List<String> errors = new ArrayList<>();
		int totalGalleries = Math.max(codes.size(), 1);
		int activeGalleryIndex = 0;

		for (int i = 0; i < codes.size(); i++) {
			progressListener.accept(new UploadProgress(0.10d + 0.80d * ((double) i / totalGalleries),
					"Galerien werden synchronisiert"));
			GalleryCode code = codes.get(i);
			if (code.picPeakEventId() <= 0) {
				LOGGER.warn("Skipping upload for code #{}: no PicPeak event ID", i + 1);
				continue;
			}

			progressListener
				.accept(new UploadProgress(0.10d + 0.80d * ((double) i / totalGalleries), "Galerie wird bereinigt"));
			if (!clearEventPhotos(token, code.picPeakEventId())) {
				errors.add("Failed to clear existing photos in event " + code.picPeakEventId() + " (code #" + (i + 1)
						+ ")");
				continue;
			}

			activeGalleryIndex++;

			Path portraitWatermarked = resolvePortraitFolder(eventDir, activeGalleryIndex, i + 1);
			List<Path> portraitFiles = listImageFiles(portraitWatermarked);

			if (klassenfotos.isEmpty() && portraitFiles.isEmpty()) {
				LOGGER.info("No photos to upload for code #{} (event {})", i + 1, code.picPeakEventId());
				continue;
			}

			int uploaded = 0;
			if (!klassenfotos.isEmpty()) {
				int kUploaded = uploadPhotos(token, code.picPeakEventId(), klassenfotos, klassenfotoCategoryId);
				if (kUploaded > 0) {
					uploaded += kUploaded;
					LOGGER.info("Uploaded {} klassenfotos to PicPeak event {} (code #{})", kUploaded,
							code.picPeakEventId(), i + 1);
				}
				else {
					errors.add("Failed to upload klassenfotos to event " + code.picPeakEventId() + " (code #" + (i + 1)
							+ ")");
				}
			}
			if (!portraitFiles.isEmpty()) {
				int pUploaded = uploadPhotos(token, code.picPeakEventId(), portraitFiles, portraitCategoryId);
				if (pUploaded > 0) {
					uploaded += pUploaded;
					LOGGER.info("Uploaded {} portraits to PicPeak event {} (code #{})", pUploaded,
							code.picPeakEventId(), i + 1);
				}
				else {
					errors.add("Failed to upload portraits to event " + code.picPeakEventId() + " (code #" + (i + 1)
							+ ")");
				}
			}

			if (uploaded > 0) {
				galleriesUpdated++;
				totalFilesUploaded += uploaded;
			}
			progressListener.accept(
					new UploadProgress(0.10d + 0.80d * ((double) (i + 1) / totalGalleries), "Galerie abgeschlossen"));
		}

		progressListener.accept(new UploadProgress(1.0d, "Upload fertig"));
		return new UploadResult(galleriesUpdated, totalFilesUploaded, errors);
	}

	private boolean clearEventPhotos(String token, int eventId) {
		if (tryBulkClear(token, eventId, "DELETE", "/api/admin/events/%d/photos")) {
			LOGGER.info("Cleared existing photos via bulk endpoint for event {}", eventId);
			return true;
		}
		if (tryBulkClear(token, eventId, "POST", "/api/admin/events/%d/photos/clear")) {
			LOGGER.info("Cleared existing photos via clear endpoint for event {}", eventId);
			return true;
		}

		List<Integer> photoIds = listEventPhotoIds(token, eventId);
		if (photoIds.isEmpty()) {
			LOGGER.info("No existing photos to clear for event {}", eventId);
			return true;
		}

		int deleted = 0;
		for (Integer photoId : photoIds) {
			if (deletePhotoById(token, eventId, photoId)) {
				deleted++;
			}
		}

		if (deleted == photoIds.size()) {
			LOGGER.info("Cleared {} existing photos for event {}", deleted, eventId);
			return true;
		}

		LOGGER.error("Could not clear all existing photos for event {} (deleted {}/{})", eventId, deleted,
				photoIds.size());
		return false;
	}

	private boolean tryBulkClear(String token, int eventId, String method, String endpointPattern) {
		try {
			String endpoint = picPeakProperties.apiUrl() + String.format(endpointPattern, eventId);
			HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token);
			if ("DELETE".equals(method)) {
				builder.DELETE();
			}
			else {
				builder.POST(HttpRequest.BodyPublishers.noBody());
			}
			HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			return response.statusCode() >= 200 && response.statusCode() < 300;
		}
		catch (Exception ex) {
			LOGGER.debug("Bulk clear endpoint failed for event {} ({} {}): {}", eventId, method, endpointPattern,
					ex.getMessage());
			return false;
		}
	}

	private List<Integer> listEventPhotoIds(String token, int eventId) {
		List<Integer> ids = new ArrayList<>();
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/admin/events/" + eventId + "/photos"))
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token)
				.GET()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.debug("List photos failed for event {} with status {}", eventId, response.statusCode());
				return ids;
			}

			JsonNode root = objectMapper.readTree(response.body());
			collectPhotoIds(root, ids);
			return ids;
		}
		catch (Exception ex) {
			LOGGER.debug("Could not list photos for event {}: {}", eventId, ex.getMessage());
			return ids;
		}
	}

	private void collectPhotoIds(JsonNode node, List<Integer> ids) {
		if (node == null || node.isNull()) {
			return;
		}
		if (node.isObject()) {
			JsonNode idNode = node.get("id");
			if (idNode != null && idNode.canConvertToInt() && looksLikePhotoNode(node)) {
				int id = idNode.asInt();
				if (!ids.contains(id)) {
					ids.add(id);
				}
			}
			JsonNode photos = node.get("photos");
			if (photos != null) {
				collectPhotoIds(photos, ids);
			}
			JsonNode items = node.get("items");
			if (items != null) {
				collectPhotoIds(items, ids);
			}
			JsonNode data = node.get("data");
			if (data != null) {
				collectPhotoIds(data, ids);
			}
			return;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				collectPhotoIds(child, ids);
			}
		}
	}

	private boolean looksLikePhotoNode(JsonNode node) {
		return node.has("event_id") || node.has("category_id") || node.has("filename") || node.has("file_name")
				|| node.has("image_url") || node.has("url") || node.has("thumb_url") || node.has("thumbnail_url");
	}

	private boolean deletePhotoById(String token, int eventId, int photoId) {
		return tryDeletePhoto(token, picPeakProperties.apiUrl() + "/api/admin/photos/" + photoId) || tryDeletePhoto(
				token, picPeakProperties.apiUrl() + "/api/admin/events/" + eventId + "/photos/" + photoId);
	}

	private boolean tryDeletePhoto(String token, String endpoint) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token)
				.DELETE()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return response.statusCode() >= 200 && response.statusCode() < 300;
		}
		catch (Exception ex) {
			LOGGER.debug("Delete photo endpoint failed ({}): {}", endpoint, ex.getMessage());
			return false;
		}
	}

	private Path resolvePortraitFolder(Path eventDir, int activeGalleryIndex, int absoluteIndex) {
		Path preferred = eventDir.resolve(portraitPrefix + activeGalleryIndex + watermarkedSuffix);
		if (Files.isDirectory(preferred)) {
			return preferred;
		}
		return eventDir.resolve(portraitPrefix + absoluteIndex + watermarkedSuffix);
	}

	private String login() {
		if (picPeakProperties.apiUrl() == null || picPeakProperties.apiUrl().isBlank()) {
			LOGGER.error("PicPeak API URL is not configured. Skipping login.");
			return null;
		}
		try {
			ObjectNode body = objectMapper.createObjectNode();
			body.put("username", picPeakProperties.username());
			body.put("password", picPeakProperties.password());
			body.putNull("recaptchaToken");
			String requestBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/auth/admin/login"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				LOGGER.error("PicPeak login failed with status {}: {}", response.statusCode(), response.body());
				return null;
			}

			String tokenFromCookie = response.headers()
				.allValues("Set-Cookie")
				.stream()
				.filter(c -> c.startsWith("admin_token="))
				.map(c -> c.split(";")[0].substring("admin_token=".length()))
				.findFirst()
				.orElse(null);
			if (tokenFromCookie != null) {
				return tokenFromCookie;
			}

			JsonNode responseJson = objectMapper.readTree(response.body());
			for (String field : List.of("token", "admin_token", "access_token", "jwt")) {
				JsonNode node = responseJson.get(field);
				if (node != null && !node.isNull()) {
					return node.asText();
				}
			}

			LOGGER.error("Could not extract admin token from PicPeak login response: {}", response.body());
			return null;
		}
		catch (Exception ex) {
			LOGGER.error("PicPeak login error: {}", ex.getMessage(), ex);
			return null;
		}
	}

	private EventCreationResult createEvent(String token, GalleryCode code, String eventName, int number,
			String eventDateOverride) {
		try {
			String galleryEventName = eventName.isBlank() ? code.code() : eventName + " # " + number;
			String eventDate = eventDateOverride == null || eventDateOverride.isBlank() ? picPeakProperties.eventDate()
					: eventDateOverride;
			if (eventDate.isBlank()) {
				eventDate = LocalDate.now().toString();
			}

			ObjectNode body = objectMapper.createObjectNode();
			body.put("event_type", picPeakProperties.eventType());
			body.put("event_name", galleryEventName);
			body.put("event_date", eventDate);
			body.put("customer_name", "");
			body.put("customer_email", picPeakProperties.customerEmail());
			body.put("admin_email", picPeakProperties.adminEmail().isBlank() ? picPeakProperties.customerEmail()
					: picPeakProperties.adminEmail());
			body.put("require_password", picPeakProperties.requirePassword());
			body.put("password", code.password());
			body.put("welcome_message", picPeakProperties.welcomeMessage());
			body.put("color_theme", picPeakProperties.colorTheme());
			body.put("header_style", picPeakProperties.headerStyle());
			body.put("hero_divider_style", picPeakProperties.heroDividerStyle());
			body.put("hero_image_anchor", picPeakProperties.heroImageAnchor());
			body.put("expiration_days", picPeakProperties.expirationDays());
			body.put("allow_user_uploads", picPeakProperties.allowUserUploads());
			if (picPeakProperties.uploadCategoryId() != null) {
				body.put("upload_category_id", picPeakProperties.uploadCategoryId());
			}
			else {
				body.putNull("upload_category_id");
			}
			body.put("source_mode", picPeakProperties.sourceMode());
			if (picPeakProperties.externalPath() != null && !picPeakProperties.externalPath().isBlank()) {
				body.put("external_path", picPeakProperties.externalPath());
			}
			else {
				body.putNull("external_path");
			}
			body.put("css_template_id", picPeakProperties.cssTemplateId());
			body.put("protection_level", picPeakProperties.protectionLevel());
			body.put("feedback_enabled", picPeakProperties.feedbackEnabled());
			body.put("allow_ratings", picPeakProperties.allowRatings());
			body.put("allow_likes", picPeakProperties.allowLikes());
			body.put("allow_comments", picPeakProperties.allowComments());
			body.put("allow_favorites", picPeakProperties.allowFavorites());
			body.put("allow_downloads", picPeakProperties.allowDownloads());
			body.put("disable_right_click", picPeakProperties.disableRightClick());
			body.put("enable_devtools_protection", picPeakProperties.enableDevtoolsProtection());
			body.put("use_canvas_rendering", picPeakProperties.useCanvasRendering());
			body.put("watermark_downloads", picPeakProperties.watermarkDownloads());
			if (picPeakProperties.heroPhotoId() != null) {
				body.put("hero_photo_id", picPeakProperties.heroPhotoId());
			}
			else {
				body.putNull("hero_photo_id");
			}
			body.put("hero_logo_visible", picPeakProperties.heroLogoVisible());
			body.put("hero_logo_size", picPeakProperties.heroLogoSize());
			body.put("hero_logo_position", picPeakProperties.heroLogoPosition());
			body.put("require_name_email", picPeakProperties.requireNameEmail());
			body.put("moderate_comments", picPeakProperties.moderateComments());
			body.put("show_feedback_to_guests", picPeakProperties.showFeedbackToGuests());
			String requestBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/admin/events"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200 && response.statusCode() != 201) {
				LOGGER.error("PicPeak event creation failed with status {}: {}", response.statusCode(),
						response.body());
				return null;
			}

			JsonNode responseJson = objectMapper.readTree(response.body());
			JsonNode shareLinkNode = responseJson.get("share_link");
			int eventId = 0;
			JsonNode idNode = responseJson.get("id");
			if (idNode == null) {
				idNode = responseJson.get("event_id");
			}
			if (idNode != null && !idNode.isNull()) {
				eventId = idNode.asInt();
			}
			if (shareLinkNode != null && !shareLinkNode.isNull()) {
				return new EventCreationResult(shareLinkNode.asText(), eventId);
			}

			LOGGER.error("Could not find share_link in PicPeak response: {}", response.body());
			return null;
		}
		catch (Exception ex) {
			LOGGER.error("PicPeak event creation error: {}", ex.getMessage(), ex);
			return null;
		}
	}

	private int uploadPhotos(String token, int eventId, List<Path> photos, Integer categoryId) {
		try {
			String boundary = "----PicPeakUpload" + UUID.randomUUID().toString().replace("-", "");
			var bodyParts = new ArrayList<byte[]>();

			for (Path photo : photos) {
				String filename = photo.getFileName().toString();
				String mimeType = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
				byte[] fileBytes = Files.readAllBytes(photo);

				String partHeader = "--" + boundary + "\r\n"
						+ "Content-Disposition: form-data; name=\"photos\"; filename=\"" + filename + "\"\r\n"
						+ "Content-Type: " + mimeType + "\r\n\r\n";
				bodyParts.add(partHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				bodyParts.add(fileBytes);
				bodyParts.add("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}

			if (categoryId != null) {
				String catPart = "--" + boundary + "\r\n"
						+ "Content-Disposition: form-data; name=\"category_id\"\r\n\r\n" + categoryId + "\r\n";
				bodyParts.add(catPart.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}

			String closingBoundary = "--" + boundary + "--\r\n";
			bodyParts.add(closingBoundary.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			int totalSize = bodyParts.stream().mapToInt(b -> b.length).sum();
			byte[] body = new byte[totalSize];
			int offset = 0;
			for (byte[] part : bodyParts) {
				System.arraycopy(part, 0, body, offset, part.length);
				offset += part.length;
			}

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/admin/events/" + eventId + "/upload"))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				LOGGER.info("Uploaded {} photos to PicPeak event {}", photos.size(), eventId);
				return photos.size();
			}

			LOGGER.error("PicPeak upload failed for event {} with status {}: {}", eventId, response.statusCode(),
					response.body());
			return 0;
		}
		catch (Exception ex) {
			LOGGER.error("PicPeak upload error for event {}: {}", eventId, ex.getMessage(), ex);
			return 0;
		}
	}

	private List<Path> listImageFiles(Path directory) {
		List<Path> files = new ArrayList<>();
		if (!Files.isDirectory(directory)) {
			return files;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, entry -> {
			String name = entry.getFileName().toString().toLowerCase();
			return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
		})) {
			for (Path entry : stream) {
				files.add(entry);
			}
		}
		catch (java.io.IOException ex) {
			LOGGER.warn("Could not list image files in {}: {}", directory, ex.getMessage());
		}
		files.sort(Path::compareTo);
		return files;
	}

	public record UploadResult(int galleriesUpdated, int totalFilesUploaded, List<String> errors) {
	}

}
