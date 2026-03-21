package com.pabaumgartner.photogallery.wizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		String galleryName,
		String galleryDescription,
		String outputPath) {

	public AppProperties {
		if (outputPath == null || outputPath.isBlank()) {
			outputPath = "gallery-output";
		}
		if (galleryDescription == null) {
			galleryDescription = "";
		}
		if (galleryName == null) {
			galleryName = "";
		}
	}

}
