package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

public record WizardRequest(String eventCode, String eventName, int codeCount, Path csvPath, Path pdfPath,
		String baseUrl, String galleryUrl, String logoUrl, int qrSize, int gridColumns, int gridRows,
		boolean showCuttingLines, String galleryCodeLabel, String galleryPasswordLabel, boolean picPeakEnabled,
		String picPeakEventDate) {
}
