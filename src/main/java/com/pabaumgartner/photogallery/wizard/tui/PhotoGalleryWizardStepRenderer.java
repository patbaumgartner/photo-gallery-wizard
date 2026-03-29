package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;

final class PhotoGalleryWizardStepRenderer {

	private PhotoGalleryWizardStepRenderer() {
	}

	static Element renderCurrentStep(PhotoGalleryWizardViewModel viewModel, FormFieldElement classNameField,
			FormFieldElement eventCodeField, FormFieldElement shootingDateField, FormFieldElement codeCountField,
			FormFieldElement picPeakEnabledField) {
		return switch (viewModel.activeStep()) {
			case SCHULFOTOS -> PhotoGalleryWizardSchulfotosStepView.render(viewModel, classNameField, eventCodeField,
					shootingDateField, codeCountField, picPeakEnabledField);
			case REVIEW -> PhotoGalleryWizardReviewStepView.render(viewModel);
			case RESULTS -> PhotoGalleryWizardResultsStepView.render(viewModel);
			case FOLDERS -> PhotoGalleryWizardFoldersStepView.render(viewModel);
			case WATERMARK -> PhotoGalleryWizardWatermarkStepView.render(viewModel);
			case UPLOAD -> PhotoGalleryWizardUploadStepView.render(viewModel);
			case DONE -> PhotoGalleryWizardDoneStepView.render(viewModel);
		};
	}

}