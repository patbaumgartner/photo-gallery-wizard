package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

public record WizardRequest(String eventCode, String eventName, int codeCount, Path csvPath, Path pdfPath,
		String baseUrl, String galleryUrl, String logoUrl, int qrSize, int gridColumns, int gridRows,
		boolean showCuttingLines, String galleryCodeLabel, String galleryPasswordLabel, boolean picPeakEnabled,
		String picPeakEventDate) {

	public WizardRequest {
		if (eventCode == null) {
			eventCode = "";
		}
		if (eventName == null) {
			eventName = "";
		}
		if (baseUrl == null) {
			baseUrl = "";
		}
		if (galleryUrl == null) {
			galleryUrl = "";
		}
		if (logoUrl == null) {
			logoUrl = "";
		}
		if (galleryCodeLabel == null) {
			galleryCodeLabel = "";
		}
		if (galleryPasswordLabel == null) {
			galleryPasswordLabel = "";
		}
		if (picPeakEventDate == null) {
			picPeakEventDate = "";
		}
	}

}
