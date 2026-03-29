package com.pabaumgartner.photogallery.wizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(String eventCode, String eventName) {

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
	}

}
