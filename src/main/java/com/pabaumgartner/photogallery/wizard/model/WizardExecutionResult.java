package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

public record WizardExecutionResult(Path csvPath, Path pdfPath, int codeCount, int pageCount, String eventCode,
		String eventName) {
}
