package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.AMBER_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.ERROR_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.LIME_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardDoneStepView {

	private PhotoGalleryWizardDoneStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		List<Element> lines = new ArrayList<>();
		if (viewModel.uploadResult() != null) {
			lines.add(text("Upload abgeschlossen. Alle Phasen des Schulfotos-Workflows sind erledigt.")
				.fg(readableText(TEXT_MUTED, SURFACE_ALT))
				.length(1));
			lines.add(text(" ").length(1));
			List<Element> summaryLines = new ArrayList<>();
			summaryLines.add(PhotoGalleryWizardUi.previewLine("Galerien aktualisiert",
					Integer.toString(viewModel.uploadResult().galleriesUpdated()), TEXT_PRIMARY));
			summaryLines.add(PhotoGalleryWizardUi.previewLine("Fotos hochgeladen",
					Integer.toString(viewModel.uploadResult().totalFilesUploaded()), TEXT_PRIMARY));
			if (!viewModel.uploadResult().errors().isEmpty()) {
				summaryLines.add(PhotoGalleryWizardUi.previewLine("Fehler",
						viewModel.uploadResult().errors().size() + " Upload-Vorgänge fehlgeschlagen", ERROR_GLOW));
				summaryLines.add(PhotoGalleryWizardUi.previewLine("Hinweis",
						"Details im Log prüfen (keine Rohfehler im UI)", TEXT_MUTED));
			}
			lines.add(PhotoGalleryWizardUi.previewCard("Upload-Ergebnisse", summaryLines,
					viewModel.uploadResult().errors().isEmpty() ? LIME_NEON : AMBER_GLOW));
		}
		else if (!viewModel.uploadMessage().isBlank()) {
			lines.add(text("Upload nicht abgeschlossen.").fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1));
			lines.add(text(" ").length(1));
			lines.add(panel("Details",
					text("Der Upload konnte nicht abgeschlossen werden. Details im Log prüfen.")
						.fg(readableText(ERROR_GLOW, SURFACE)))
				.rounded()
				.padding(1)
				.bg(SURFACE)
				.borderColor(ERROR_GLOW));
		}
		else {
			lines.add(text("Upload übersprungen. Keine Galerie-Codes mit PicPeak Event-IDs vorhanden.")
				.fg(readableText(TEXT_MUTED, SURFACE_ALT))
				.length(1));
		}
		if (viewModel.executionResult() != null) {
			lines.add(text(" ").length(1));
			lines.add(PhotoGalleryWizardUi.previewCard("Generierte Dateien",
					List.of(PhotoGalleryWizardUi.previewLine("Klassen-Name", viewModel.executionResult().eventName(),
							TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("Event-Code", viewModel.executionResult().eventCode(),
									TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("Codes",
									Integer.toString(viewModel.executionResult().codeCount()), TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("CSV",
									viewModel.executionResult().csvPath().toAbsolutePath().toString(), TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("PDF",
									viewModel.executionResult().pdfPath().toAbsolutePath().toString(), TEXT_PRIMARY)),
					LIME_NEON));
		}
		return panel("Fertig", column(lines.toArray(new Element[0]))).rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(LIME_NEON)
			.fill();
	}

}