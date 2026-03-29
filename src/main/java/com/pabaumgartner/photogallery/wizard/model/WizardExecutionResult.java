package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

public record WizardExecutionResult(String eventCode, String eventName, int codeCount, int pageCount, Path csvPath,
		Path pdfPath) {

	public WizardExecutionResult {
		if (eventCode == null) {
			eventCode = "";
		}
		if (eventName == null) {
			eventName = "";
		}
	}

}
