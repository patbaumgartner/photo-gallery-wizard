package com.pabaumgartner.photogallery.wizard.tui;

import java.util.List;

import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardReviewStepView {

	private PhotoGalleryWizardReviewStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		return panel(viewModel.currentStepTitle(), column(
				text("Der Workflow ist bereit. Enter drücken, um CSV und doppelseitiges QR-Code-PDF zu erstellen.")
					.fg(readableText(TEXT_MUTED, SURFACE_ALT))
					.length(1),
				text(" ").length(1),
				PhotoGalleryWizardUi.previewCard("Konfigurationsübersicht",
						List.of(PhotoGalleryWizardUi.previewLine("Event-Code", viewModel.requestPreview().eventCode(),
								CYAN_NEON),
								PhotoGalleryWizardUi.previewLine(
										"Event Name",
										PhotoGalleryWizardUi.blankFallback(viewModel.requestPreview().eventName(),
												"(leer)"),
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("Code Count",
										Integer.toString(viewModel.requestPreview().codeCount()), TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("CSV", viewModel.requestPreview().csvPath().toString(),
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("PDF", viewModel.requestPreview().pdfPath().toString(),
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("Base URL", viewModel.requestPreview().baseUrl(),
										CYAN_NEON),
								PhotoGalleryWizardUi.previewLine("Gallery URL", viewModel.requestPreview().galleryUrl(),
										CYAN_NEON),
								PhotoGalleryWizardUi.previewLine("Grid",
										viewModel.requestPreview().gridColumns() + " x "
												+ viewModel.requestPreview().gridRows(),
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("QR Size", viewModel.requestPreview().qrSize() + " px",
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine(
										"Schnittlinien",
										PhotoGalleryWizardUi
											.booleanLabel(viewModel.requestPreview().showCuttingLines()),
										TEXT_PRIMARY),
								PhotoGalleryWizardUi.previewLine("PicPeak", PhotoGalleryWizardUi.booleanLabel(
										viewModel.requestPreview().picPeakEnabled()), TEXT_PRIMARY)),
						CYAN_NEON),
				text(" ").length(1),
				panel("Ausführen",
						text("Jetzt Enter drücken, um die Dateien zu generieren.")
							.fg(readableText(TEXT_MUTED, SURFACE)))
					.rounded()
					.padding(1)
					.bg(SURFACE)
					.borderColor(CYAN_NEON)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(CYAN_NEON)
			.fill();
	}

}