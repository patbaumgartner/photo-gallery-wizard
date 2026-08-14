package com.pabaumgartner.photogallery.wizard.config;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.schulfotos")
public record SchulfotosProperties(String baseUrl, String galleryUrl, int defaultCodeCount, int qrSize, int gridColumns,
		int gridRows, boolean showCuttingLines, String galleryCodeLabel, String galleryPasswordLabel, String outputDir,
		String codeCharset, String logoPath, String klassenfotoFolder, String portraitPrefix, String watermarkedSuffix,
		int passwordLength) {

	private static final Pattern PATH_SEPARATORS = Pattern.compile("[\\\\/]|(^|[\\\\/])\\.{1,2}([\\\\/]|$)");

	private static final Pattern CODE_CHARSET_ALPHABET = Pattern.compile("[A-Z0-9]+");

	private static final int MIN_CODE_CHARSET_SIZE = 2;

	private static final int MIN_PASSWORD_LENGTH = 4;

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
		codeCharset = normalizeCodeCharset(codeCharset);
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
		if (passwordLength < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("app.schulfotos.password-length must be at least " + MIN_PASSWORD_LENGTH
					+ " to fit one upper-case, lower-case, digit and special character, got: " + passwordLength);
		}
		requireSingleSegment("app.schulfotos.klassenfoto-folder", klassenfotoFolder);
		requireSingleSegment("app.schulfotos.portrait-prefix", portraitPrefix);
		requireSingleSegment("app.schulfotos.watermarked-suffix", watermarkedSuffix);
	}

	private static String normalizeCodeCharset(String codeCharset) {
		String upperCase = codeCharset.trim().toUpperCase(Locale.ROOT);
		String distinct = upperCase.codePoints()
			.distinct()
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
		if (!CODE_CHARSET_ALPHABET.matcher(distinct).matches()) {
			throw new IllegalArgumentException(
					"app.schulfotos.code-charset must only contain A-Z and 0-9, got: '" + codeCharset + "'");
		}
		if (distinct.length() < MIN_CODE_CHARSET_SIZE) {
			throw new IllegalArgumentException("app.schulfotos.code-charset must contain at least "
					+ MIN_CODE_CHARSET_SIZE + " distinct characters, got: '" + codeCharset + "'");
		}
		return distinct;
	}

	private static void requireSingleSegment(String property, String value) {
		if (PATH_SEPARATORS.matcher(value).find()) {
			throw new IllegalArgumentException(
					property + " must be a plain folder-name fragment without path separators, got: '" + value + "'");
		}
	}

}
