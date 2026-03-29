package com.pabaumgartner.photogallery.wizard.tui;

import java.util.List;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardSchulfotosStepView {

	private PhotoGalleryWizardSchulfotosStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel, FormFieldElement classNameField,
			FormFieldElement eventCodeField, FormFieldElement shootingDateField, FormFieldElement codeCountField,
			FormFieldElement picPeakEnabledField) {
		return panel("Schulfotos Einrichtung", column(text(
				"Klassendaten und Shooting-Datum eingeben. Standardwerte sind für mel-rohrer.ch/schulfotos voreingestellt.")
			.fg(readableText(TEXT_MUTED, SURFACE_ALT))
			.length(1), text(" ").length(1), classNameField, text(" ").length(1), eventCodeField, text(" ").length(1),
				shootingDateField, text(" ").length(1), codeCountField, text(" ").length(1), picPeakEnabledField,
				text(" ").length(1),
				PhotoGalleryWizardUi.previewCard("Abgeleitete Standardwerte",
						List.of(PhotoGalleryWizardUi.previewLine("Base URL", viewModel.baseUrl(), CYAN_NEON),
								PhotoGalleryWizardUi.previewLine("Gallery URL", viewModel.galleryUrl(), CYAN_NEON),
								PhotoGalleryWizardUi.previewLine("CSV", viewModel.csvPath().toString(), TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("PDF", viewModel.pdfPath().toString(), TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("Layout",
										viewModel.gridColumns() + " x " + viewModel.gridRows() + ", Schnittlinien an, "
												+ viewModel.qrSize() + " px QR",
										TEXT_PRIMARY)),
						CYAN_NEON)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(CYAN_NEON)
			.fill();
	}

}