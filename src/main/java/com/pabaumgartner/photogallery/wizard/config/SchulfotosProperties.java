package com.pabaumgartner.photogallery.wizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.schulfotos")
public record SchulfotosProperties(String baseUrl, String galleryUrl, int defaultCodeCount, int qrSize, int gridColumns,
		int gridRows, boolean showCuttingLines, String galleryCodeLabel, String galleryPasswordLabel, String outputDir,
		String codeCharset, String logoPath, String klassenfotoFolder, String portraitPrefix, String watermarkedSuffix,
		int passwordLength) {

	public SchulfotosProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://example.com/schulfotos";
		}
		if (galleryUrl == null || galleryUrl.isBlank()) {
			galleryUrl = "https://example.com/schulfotos/?code=";
		}
		if (defaultCodeCount <= 0) {
			defaultCodeCount = 17;
		}
		if (qrSize <= 0) {
			qrSize = 200;
		}
		if (gridColumns <= 0) {
			gridColumns = 3;
		}
		if (gridRows <= 0) {
			gridRows = 4;
		}
		if (galleryCodeLabel == null || galleryCodeLabel.isBlank()) {
			galleryCodeLabel = "GALERIE CODE";
		}
		if (galleryPasswordLabel == null || galleryPasswordLabel.isBlank()) {
			galleryPasswordLabel = "GALERIE PASSWORT";
		}
		if (outputDir == null || outputDir.isBlank()) {
			outputDir = "schulfotos";
		}
		if (codeCharset == null || codeCharset.isBlank()) {
			codeCharset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		}
		if (logoPath == null || logoPath.isBlank()) {
			logoPath = "configuration/logo.png";
		}
		if (klassenfotoFolder == null || klassenfotoFolder.isBlank()) {
			klassenfotoFolder = "klassenfoto";
		}
		if (portraitPrefix == null || portraitPrefix.isBlank()) {
			portraitPrefix = "portrait-";
		}
		if (watermarkedSuffix == null || watermarkedSuffix.isBlank()) {
			watermarkedSuffix = "-watermarked";
		}
		if (passwordLength <= 0) {
			passwordLength = 9;
		}
	}

}
