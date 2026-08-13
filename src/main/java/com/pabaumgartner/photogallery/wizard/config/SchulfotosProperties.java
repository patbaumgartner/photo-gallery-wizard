package com.pabaumgartner.photogallery.wizard.config;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.schulfotos")
public record SchulfotosProperties(String baseUrl, String galleryUrl, int defaultCodeCount, int qrSize, int gridColumns,
		int gridRows, boolean showCuttingLines, String galleryCodeLabel, String galleryPasswordLabel, String outputDir,
		String codeCharset, String logoPath, String klassenfotoFolder, String portraitPrefix, String watermarkedSuffix,
		int passwordLength) {

	private static final Pattern PATH_SEPARATORS = Pattern.compile("[\\\\/]|(^|[\\\\/])\\.{1,2}([\\\\/]|$)");

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
		requireSingleSegment("app.schulfotos.klassenfoto-folder", klassenfotoFolder);
		requireSingleSegment("app.schulfotos.portrait-prefix", portraitPrefix);
		requireSingleSegment("app.schulfotos.watermarked-suffix", watermarkedSuffix);
	}

	private static void requireSingleSegment(String property, String value) {
		if (PATH_SEPARATORS.matcher(value).find()) {
			throw new IllegalArgumentException(
					property + " must be a plain folder-name fragment without path separators, got: '" + value + "'");
		}
	}

}
