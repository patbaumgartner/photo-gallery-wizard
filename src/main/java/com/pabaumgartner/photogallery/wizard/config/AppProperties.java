package com.pabaumgartner.photogallery.wizard.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
		@NotBlank String galleryName,
		String galleryDescription,
		String outputPath) {

	public AppProperties {
		if (outputPath == null || outputPath.isBlank()) {
			outputPath = "gallery-output";
		}
		if (galleryDescription == null) {
			galleryDescription = "";
		}
	}

}
