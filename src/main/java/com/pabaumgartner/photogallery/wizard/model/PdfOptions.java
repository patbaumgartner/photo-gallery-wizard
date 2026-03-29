package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

public record PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName,
		String baseUrl, String logoUrl, String galleryCodeLabel, String galleryPasswordLabel) {

	public PdfOptions {
		if (eventName == null) {
			eventName = "";
		}
		if (baseUrl == null) {
			baseUrl = "";
		}
		if (logoUrl == null) {
			logoUrl = "";
		}
		if (galleryCodeLabel == null || galleryCodeLabel.isBlank()) {
			galleryCodeLabel = "GALERIE CODE";
		}
		if (galleryPasswordLabel == null || galleryPasswordLabel.isBlank()) {
			galleryPasswordLabel = "GALERIE PASSWORT";
		}
	}

}
