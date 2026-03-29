package com.pabaumgartner.photogallery.wizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.picpeak")
public record PicPeakProperties(boolean enabled, boolean createEvents, String apiUrl, String username, String password,
		String eventType, String eventDate, String customerEmail, String adminEmail, boolean requirePassword,
		String welcomeMessage, int expirationDays, boolean allowUserUploads, boolean feedbackEnabled,
		boolean allowRatings, boolean allowLikes, boolean allowComments, boolean allowFavorites, boolean allowDownloads,
		boolean disableRightClick, boolean enableDevtoolsProtection, boolean useCanvasRendering,
		boolean watermarkDownloads, boolean heroLogoVisible, boolean requireNameEmail, boolean moderateComments,
		boolean showFeedbackToGuests, String headerStyle, String heroDividerStyle, int cssTemplateId, String colorTheme,
		String protectionLevel, String sourceMode, String heroImageAnchor, String heroLogoSize, String heroLogoPosition,
		Integer uploadCategoryId, Integer klassenfotoCategoryId, Integer portraitCategoryId, String externalPath,
		Integer heroPhotoId, int maxPasswordRetries) {

	public PicPeakProperties {
		if (apiUrl == null) {
			apiUrl = "";
		}
		if (apiUrl.endsWith("/")) {
			apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
		}
		if (username == null) {
			username = "";
		}
		if (password == null) {
			password = "";
		}
		if (enabled) {
			if (apiUrl.isBlank()) {
				throw new IllegalArgumentException("app.picpeak.api-url must be set when app.picpeak.enabled=true");
			}
			if (username.isBlank()) {
				throw new IllegalArgumentException("app.picpeak.username must be set when app.picpeak.enabled=true");
			}
			if (password.isBlank()) {
				throw new IllegalArgumentException("app.picpeak.password must be set when app.picpeak.enabled=true");
			}
		}
		if (eventType == null) {
			eventType = "schulfotos";
		}
		if (eventDate == null) {
			eventDate = "";
		}
		if (customerEmail == null) {
			customerEmail = "";
		}
		if (adminEmail == null) {
			adminEmail = "";
		}
		if (welcomeMessage == null) {
			welcomeMessage = "";
		}
		if (expirationDays <= 0) {
			expirationDays = 30;
		}
		if (headerStyle == null) {
			headerStyle = "standard";
		}
		if (heroDividerStyle == null) {
			heroDividerStyle = "wave";
		}
		if (cssTemplateId <= 0) {
			cssTemplateId = 1;
		}
		if (colorTheme == null) {
			colorTheme = "default";
		}
		if (protectionLevel == null) {
			protectionLevel = "standard";
		}
		if (sourceMode == null) {
			sourceMode = "managed";
		}
		if (heroImageAnchor == null) {
			heroImageAnchor = "center";
		}
		if (heroLogoSize == null) {
			heroLogoSize = "medium";
		}
		if (heroLogoPosition == null) {
			heroLogoPosition = "top";
		}
		if (maxPasswordRetries <= 0) {
			maxPasswordRetries = 3;
		}
	}

}
