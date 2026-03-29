package com.pabaumgartner.photogallery.wizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(String eventCode, String eventName, String watermarkPath, int resizeMaxEdge,
		float watermarkOpacity, float watermarkScale, float jpegQuality, int logoConnectTimeoutMs,
		int logoReadTimeoutMs, String filenameStripPostfix) {

	public AppProperties {
		if (eventCode == null) {
			eventCode = "";
		}
		if (!eventCode.isBlank()) {
			eventCode = eventCode.trim().toUpperCase();
			if (!eventCode.matches("^[A-Z0-9]{4}$")) {
				throw new IllegalArgumentException(
						"app.event-code must be exactly 4 alphanumeric characters, got: '" + eventCode + "'");
			}
		}
		if (eventName == null) {
			eventName = "";
		}
		if (watermarkPath == null || watermarkPath.isBlank()) {
			watermarkPath = "configuration/watermark.png";
		}
		if (resizeMaxEdge <= 0) {
			resizeMaxEdge = 1200;
		}
		if (watermarkOpacity <= 0f || watermarkOpacity > 1f) {
			watermarkOpacity = 0.3f;
		}
		if (watermarkScale <= 0f || watermarkScale > 1f) {
			watermarkScale = 0.4f;
		}
		if (jpegQuality <= 0f || jpegQuality > 1f) {
			jpegQuality = 0.9f;
		}
		if (logoConnectTimeoutMs <= 0) {
			logoConnectTimeoutMs = 5000;
		}
		if (logoReadTimeoutMs <= 0) {
			logoReadTimeoutMs = 10000;
		}
		if (filenameStripPostfix == null) {
			filenameStripPostfix = "_NEU";
		}
	}

}
