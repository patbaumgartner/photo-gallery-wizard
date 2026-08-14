package com.pabaumgartner.photogallery.wizard.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.pabaumgartner.photogallery.wizard.config.ImageProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.PdfOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PdfGeneratorService.class);

	private static final float MARGIN = 30f;

	private static final float MM_TO_PT = 72f / 25.4f;

	private static final float BOX_EXPANSION_MM = 2f;

	private static final float CELL_PADDING = 14f - BOX_EXPANSION_MM * MM_TO_PT;

	private static final float TEXT_HEIGHT = 75f;

	private static final float QR_BORDER_PAD = 3f;

	private static final float QR_VERTICAL_OFFSET_MM = 2f;

	private static final float CODE_FONT_SIZE = 14f;

	private static final float EVENT_NAME_FONT_SIZE = 17f;

	private static final float EVENT_NAME_GAP = 3f + MM_TO_PT;

	private static final float CUTTING_LINE_WIDTH = 0.5f;

	private static final float CUTTING_MARK_LENGTH = 10f;

	private static final float BACK_LOGO_RATIO = 0.50f;

	private static final float BACK_LOGO_V_PAD = 6f;

	private static final float BACK_LOGO_H_PAD = 8f;

	private static final float BACK_CARD_BORDER_WIDTH = 0.5f;

	private static final float BACK_RULE_WIDTH = 0.4f;

	private static final float BACK_RULE_INSET = 0f;

	private static final float BACK_RULE_GAP = 3.5f;

	private static final float BACK_LABEL_FONT_SIZE = 10f;

	private static final float BACK_PASSWORD_FONT_SIZE = 18f;

	private static final float BACK_URL_FONT_SIZE = 12f;

	private static final float BACK_LABEL_PW_GAP = 5f;

	private static final int MIN_URL_DISPLAY_LENGTH = 6;

	private static final float MIN_FONT_SIZE = 8f;

	private static final float FIT_FONT_MARGIN = 4f;

	private static final String RESOURCES_PREFIX = "src/main/resources/";

	private static final String UNSUPPORTED_CHARACTER_REPLACEMENT = "?";

	private static final int MAX_LOGO_BYTES = 16 * 1024 * 1024;

	private static final float INK = 0.0f;

	private static final float LINE_GRAY = 0.75f;

	private final int logoConnectTimeoutMs;

	private final int logoReadTimeoutMs;

	public PdfGeneratorService(ImageProperties imageProperties) {
		this.logoConnectTimeoutMs = imageProperties.logoConnectTimeoutMs();
		this.logoReadTimeoutMs = imageProperties.logoReadTimeoutMs();
	}

	public int createPdf(List<GalleryCode> codes, LinkedHashMap<GalleryCode, BufferedImage> qrImages,
			PdfOptions options) throws IOException {

		int gridColumns = options.gridColumns();
		int gridRows = options.gridRows();
		Path outputPath = options.outputPath();

		Path parent = outputPath.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		int codesPerPage = gridColumns * gridRows;
		float pageWidth = PDRectangle.A4.getWidth();
		float pageHeight = PDRectangle.A4.getHeight();
		float cellWidth = (pageWidth - 2 * MARGIN) / gridColumns;
		float cellHeight = (pageHeight - 2 * MARGIN) / gridRows;
		float innerWidth = cellWidth - 2 * CELL_PADDING;
		float innerHeight = cellHeight - 2 * CELL_PADDING;
		float qrSize = Math.min(innerWidth - 2 * QR_BORDER_PAD, innerHeight - TEXT_HEIGHT - QR_BORDER_PAD);
		int sheetCount = (int) Math.ceil((double) codes.size() / codesPerPage);

		try (PDDocument document = new PDDocument()) {
			PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			PDImageXObject logoImage = options.logoUrl().isBlank() ? null : loadLogoImage(document, options.logoUrl());

			for (int sheet = 0; sheet < sheetCount; sheet++) {
				int startI = sheet * codesPerPage;
				int endI = Math.min(startI + codesPerPage, codes.size());

				PDPage frontPage = new PDPage(PDRectangle.A4);
				document.addPage(frontPage);
				try (PDPageContentStream content = new PDPageContentStream(document, frontPage)) {
					for (int i = startI; i < endI; i++) {
						int indexOnPage = i - startI;
						GalleryCode code = codes.get(i);
						BufferedImage qrCode = qrImages.get(code);
						if (qrCode == null) {
							throw new IOException("No QR code image was generated for " + code.code());
						}
						float innerX = MARGIN + (indexOnPage % gridColumns) * cellWidth + CELL_PADDING;
						float innerY = pageHeight - MARGIN - (indexOnPage / gridColumns + 1) * cellHeight
								+ CELL_PADDING;
						PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, toByteArray(qrCode),
								"qr-" + code.code());
						drawFrontCell(content, code, qrImage, innerX, innerY, innerWidth, innerHeight, qrSize, fontBold,
								fontRegular, options);
					}
					drawCuttingLines(content, options, pageWidth, pageHeight, gridColumns, gridRows, cellWidth,
							cellHeight);
				}

				PDPage backPage = new PDPage(PDRectangle.A4);
				document.addPage(backPage);
				try (PDPageContentStream content = new PDPageContentStream(document, backPage)) {
					for (int i = startI; i < endI; i++) {
						int indexOnPage = i - startI;
						int mirroredCol = gridColumns - 1 - indexOnPage % gridColumns;
						float innerX = MARGIN + mirroredCol * cellWidth + CELL_PADDING;
						float innerY = pageHeight - MARGIN - (indexOnPage / gridColumns + 1) * cellHeight
								+ CELL_PADDING;
						drawBackCell(content, codes.get(i), innerX, innerY, innerWidth, innerHeight, fontBold,
								fontRegular, logoImage, options);
					}
					drawCuttingLines(content, options, pageWidth, pageHeight, gridColumns, gridRows, cellWidth,
							cellHeight);
				}
			}

			document.save(outputPath.toFile());
		}

		LOGGER.atInfo()
			.addArgument(outputPath)
			.addArgument(sheetCount)
			.addArgument(() -> codes.size())
			.log("Generated PDF: {} ({} duplex sheets, {} codes)");
		return sheetCount;
	}

	private void drawFrontCell(PDPageContentStream content, GalleryCode code, PDImageXObject qrImage, float innerX,
			float innerY, float innerWidth, float innerHeight, float qrSize, PDType1Font fontBold,
			PDType1Font fontRegular, PdfOptions options) throws IOException {
		content.saveGraphicsState();
		try {
			content.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			content.setLineWidth(BACK_CARD_BORDER_WIDTH);
			content.addRect(innerX, innerY, innerWidth, innerHeight);
			content.stroke();

			content.drawImage(qrImage, innerX + (innerWidth - qrSize) / 2,
					innerY + TEXT_HEIGHT - QR_VERTICAL_OFFSET_MM * MM_TO_PT, qrSize, qrSize);

			String codeLabel = toEncodable(fontBold, code.code());
			String galleryCodeLabel = toEncodable(fontRegular, options.galleryCodeLabel());
			String eventName = toEncodable(fontRegular, options.eventName());
			boolean hasEventName = !eventName.isBlank();

			float actualCodeFontSize = fitFontSize(fontBold, codeLabel, innerWidth - FIT_FONT_MARGIN, CODE_FONT_SIZE,
					MIN_FONT_SIZE);
			float combinedHeight = actualCodeFontSize + BACK_LABEL_PW_GAP + BACK_LABEL_FONT_SIZE
					+ (hasEventName ? EVENT_NAME_GAP + EVENT_NAME_FONT_SIZE : 0f);
			float blockStartY = innerY + (TEXT_HEIGHT - combinedHeight) / 2;

			content.setNonStrokingColor(INK, INK, INK);
			drawCenteredText(content, fontBold, actualCodeFontSize, codeLabel, innerX, innerWidth, blockStartY);

			float galleryCodeLabelY = blockStartY + actualCodeFontSize + BACK_LABEL_PW_GAP;
			drawCenteredText(content, fontRegular, BACK_LABEL_FONT_SIZE, galleryCodeLabel, innerX, innerWidth,
					galleryCodeLabelY);

			if (hasEventName) {
				drawCenteredText(content, fontRegular, EVENT_NAME_FONT_SIZE, eventName, innerX, innerWidth,
						galleryCodeLabelY + BACK_LABEL_FONT_SIZE + EVENT_NAME_GAP + MM_TO_PT);
			}
		}
		finally {
			content.restoreGraphicsState();
		}
	}

	private void drawCenteredText(PDPageContentStream content, PDType1Font font, float fontSize, String text,
			float innerX, float innerWidth, float y) throws IOException {
		float textWidth = font.getStringWidth(text) / 1000f * fontSize;
		content.beginText();
		content.setFont(font, fontSize);
		content.newLineAtOffset(innerX + (innerWidth - textWidth) / 2, y);
		content.showText(text);
		content.endText();
	}

	private void drawBackCell(PDPageContentStream cs, GalleryCode code, float innerX, float innerY, float innerWidth,
			float innerHeight, PDType1Font fontBold, PDType1Font fontRegular, PDImageXObject logoImage,
			PdfOptions options) throws IOException {

		String baseUrl = toEncodable(fontBold, options.baseUrl());
		cs.saveGraphicsState();
		try {

			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_CARD_BORDER_WIDTH);
			cs.addRect(innerX, innerY, innerWidth, innerHeight);
			cs.stroke();

			float logoZoneH = innerHeight * BACK_LOGO_RATIO;
			float logoZoneTopY = innerY + innerHeight;
			float logoZoneBotY = logoZoneTopY - logoZoneH;

			if (logoImage != null) {
				float logoAspect = (float) logoImage.getWidth() / logoImage.getHeight();
				float maxLogoW = innerWidth - BACK_LOGO_H_PAD * 2f;
				float maxLogoH = logoZoneH - BACK_LOGO_V_PAD * 2f;
				float logoW = maxLogoW;
				float logoH = logoW / logoAspect;
				if (logoH > maxLogoH) {
					logoH = maxLogoH;
					logoW = logoH * logoAspect;
				}
				float logoX = innerX + (innerWidth - logoW) / 2f;
				float logoY = logoZoneBotY + (logoZoneH - logoH) / 2f;
				cs.drawImage(logoImage, logoX, logoY, logoW, logoH);
			}

			float rule1Y = logoZoneBotY - BACK_RULE_GAP;
			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, rule1Y);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, rule1Y);
			cs.stroke();

			float bottomRuleY = innerY + BACK_RULE_GAP * 2f
					+ (baseUrl.isBlank() ? 0f : BACK_URL_FONT_SIZE + BACK_RULE_GAP);
			float passwordSectionBotY = bottomRuleY + BACK_RULE_GAP;

			String password = code.password().isBlank() ? "\u2014" : toEncodable(fontBold, code.password());
			float actualPwFontSize = fitFontSize(fontBold, password, innerWidth - FIT_FONT_MARGIN,
					BACK_PASSWORD_FONT_SIZE, MIN_FONT_SIZE);
			float blockH = BACK_LABEL_FONT_SIZE + BACK_LABEL_PW_GAP + actualPwFontSize;
			float blockBotY = passwordSectionBotY + ((rule1Y - BACK_RULE_GAP - passwordSectionBotY) - blockH) / 2f;

			cs.setNonStrokingColor(INK, INK, INK);
			String label = toEncodable(fontRegular, options.galleryPasswordLabel());
			drawCenteredText(cs, fontRegular, BACK_LABEL_FONT_SIZE, label, innerX, innerWidth,
					blockBotY + actualPwFontSize + BACK_LABEL_PW_GAP);
			drawCenteredText(cs, fontBold, actualPwFontSize, password, innerX, innerWidth, blockBotY);

			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, bottomRuleY);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, bottomRuleY);
			cs.stroke();

			if (!baseUrl.isBlank()) {
				String displayUrl = truncateUrl(baseUrl, fontBold, BACK_URL_FONT_SIZE, innerWidth - FIT_FONT_MARGIN);
				float urlY = innerY + (bottomRuleY - innerY - BACK_URL_FONT_SIZE) / 2f + MM_TO_PT;
				cs.setNonStrokingColor(INK, INK, INK);
				drawCenteredText(cs, fontBold, BACK_URL_FONT_SIZE, displayUrl, innerX, innerWidth, urlY);
			}
		}
		finally {
			cs.restoreGraphicsState();
		}
	}

	private String truncateUrl(String url, PDType1Font font, float fontSize, float maxWidth) throws IOException {
		float w = font.getStringWidth(url) / 1000f * fontSize;
		if (w <= maxWidth) {
			return url;
		}
		String display = url.replaceFirst("^https?://", "");
		w = font.getStringWidth(display) / 1000f * fontSize;
		if (w <= maxWidth) {
			return display;
		}
		while (display.length() > MIN_URL_DISPLAY_LENGTH) {
			display = display.substring(0, display.length() - 4) + "...";
			w = font.getStringWidth(display) / 1000f * fontSize;
			if (w <= maxWidth) {
				break;
			}
		}
		return display;
	}

	private float fitFontSize(PDType1Font font, String text, float maxWidth, float maxFontSize, float minFontSize)
			throws IOException {
		float size = maxFontSize;
		while (size > minFontSize) {
			float w = font.getStringWidth(text) / 1000f * size;
			if (w <= maxWidth) {
				break;
			}
			size -= 0.5f;
		}
		return size;
	}

	/**
	 * The Standard 14 fonts only cover WinAnsiEncoding, and PDFBox rejects anything else.
	 * A class name typed into the wizard must never abort the whole PDF, so unsupported
	 * characters are replaced instead.
	 */
	static String toEncodable(PDType1Font font, String text) {
		if (isEncodable(font, text)) {
			return text;
		}
		StringBuilder encodable = new StringBuilder(text.length());
		int index = 0;
		while (index < text.length()) {
			int codePoint = text.codePointAt(index);
			String character = new String(Character.toChars(codePoint));
			encodable.append(isEncodable(font, character) ? character : UNSUPPORTED_CHARACTER_REPLACEMENT);
			index += Character.charCount(codePoint);
		}
		return encodable.toString();
	}

	private static boolean isEncodable(PDType1Font font, String text) {
		try {
			font.getStringWidth(text);
			return true;
		}
		catch (IOException | IllegalArgumentException ex) {
			LOGGER.debug("Text is not representable in the PDF font: {}", ex.getMessage());
			return false;
		}
	}

	private PDImageXObject loadLogoImage(PDDocument document, String logoUrl) {
		if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
			return loadLogoFromHttp(document, logoUrl);
		}
		return loadLogoFromLocalPath(document, logoUrl);
	}

	private PDImageXObject loadLogoFromHttp(PDDocument document, String logoUrl) {
		URI uri;
		try {
			uri = URI.create(logoUrl);
		}
		catch (IllegalArgumentException ex) {
			LOGGER.error("Invalid logo URL '{}': {}", logoUrl, ex.getMessage(), ex);
			return null;
		}
		try {
			URLConnection connection = uri.toURL().openConnection();
			connection.setConnectTimeout(logoConnectTimeoutMs);
			connection.setReadTimeout(logoReadTimeoutMs);
			byte[] imageData;
			try (InputStream inputStream = connection.getInputStream()) {
				// A logo served from an unexpected endpoint must not be able to
				// exhaust the heap.
				imageData = inputStream.readNBytes(MAX_LOGO_BYTES + 1);
			}
			if (imageData.length > MAX_LOGO_BYTES) {
				LOGGER.error("Logo at '{}' is larger than {} bytes; skipping it", logoUrl, MAX_LOGO_BYTES);
				return null;
			}
			return createLogoImageFromBytes(document, imageData, logoUrl);
		}
		catch (IOException ex) {
			LOGGER.error("Could not load logo from URL '{}': {}", logoUrl, ex.getMessage(), ex);
			return null;
		}
	}

	private PDImageXObject loadLogoFromLocalPath(PDDocument document, String path) {
		Path filePath = Path.of(path);
		if (Files.isReadable(filePath)) {
			try {
				return createLogoImageFromBytes(document, Files.readAllBytes(filePath), path);
			}
			catch (IOException ex) {
				LOGGER.error("Could not read logo from file '{}': {}", path, ex.getMessage(), ex);
				return null;
			}
		}
		String resourcePath = path.startsWith(RESOURCES_PREFIX) ? "/" + path.substring(RESOURCES_PREFIX.length())
				: (path.startsWith("/") ? path : "/" + path);
		try (InputStream is = PdfGeneratorService.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				LOGGER.warn("Logo '{}' not found as file or classpath resource", path);
				return null;
			}
			return createLogoImageFromBytes(document, is.readAllBytes(), path);
		}
		catch (IOException ex) {
			LOGGER.error("Could not load logo from classpath '{}': {}", resourcePath, ex.getMessage(), ex);
			return null;
		}
	}

	private PDImageXObject createLogoImageFromBytes(PDDocument document, byte[] imageData, String source)
			throws IOException {
		try {
			return PDImageXObject.createFromByteArray(document, imageData, "logo");
		}
		catch (IOException | IllegalArgumentException ex) {
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
			if (bufferedImage == null) {
				LOGGER.warn("Could not decode logo from '{}' (unsupported format)", source);
				return null;
			}
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(bufferedImage, "PNG", baos);
				return PDImageXObject.createFromByteArray(document, baos.toByteArray(), "logo");
			}
		}
	}

	private void drawCuttingLines(PDPageContentStream content, PdfOptions options, float pageWidth, float pageHeight,
			int gridColumns, int gridRows, float cellWidth, float cellHeight) throws IOException {
		if (!options.showCuttingLines()) {
			return;
		}
		content.saveGraphicsState();
		try {
			content.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			content.setLineWidth(CUTTING_LINE_WIDTH);

			float[] xCuts = new float[gridColumns + 1];
			xCuts[0] = MARGIN;
			for (int col = 1; col < gridColumns; col++) {
				xCuts[col] = MARGIN + col * cellWidth;
			}
			xCuts[gridColumns] = pageWidth - MARGIN;

			float[] yCuts = new float[gridRows + 1];
			yCuts[0] = pageHeight - MARGIN;
			for (int row = 1; row < gridRows; row++) {
				yCuts[row] = pageHeight - MARGIN - row * cellHeight;
			}
			yCuts[gridRows] = MARGIN;

			for (float x : xCuts) {
				content.moveTo(x, pageHeight - MARGIN);
				content.lineTo(x, pageHeight - MARGIN + CUTTING_MARK_LENGTH);
				content.stroke();
				content.moveTo(x, MARGIN);
				content.lineTo(x, MARGIN - CUTTING_MARK_LENGTH);
				content.stroke();
			}

			for (float y : yCuts) {
				content.moveTo(MARGIN, y);
				content.lineTo(MARGIN - CUTTING_MARK_LENGTH, y);
				content.stroke();
				content.moveTo(pageWidth - MARGIN, y);
				content.lineTo(pageWidth - MARGIN + CUTTING_MARK_LENGTH, y);
				content.stroke();
			}
		}
		finally {
			content.restoreGraphicsState();
		}
	}

	private byte[] toByteArray(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			return baos.toByteArray();
		}
	}

}
