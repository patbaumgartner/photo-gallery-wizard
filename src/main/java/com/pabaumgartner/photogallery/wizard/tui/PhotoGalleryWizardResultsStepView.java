package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.ERROR_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.LIME_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardResultsStepView {

	private PhotoGalleryWizardResultsStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.executionInProgress()) {
			double progress = Math.max(0.0d, Math.min(1.0d, viewModel.executionProgress()));
			return panel("Generierung läuft", column(
					text("CSV und PDF werden erstellt. Bitte warten...").fg(readableText(TEXT_MUTED, SURFACE_ALT))
						.length(1),
					text(" ").length(1),
					text("Status: " + viewModel.executionStage()).fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1),
					gauge(progress).gaugeColor(CYAN_NEON)
						.gaugeStyle(
								dev.tamboui.style.Style.create().fg(readableText(TEXT_PRIMARY, SURFACE)).bg(SURFACE))
						.length(3),
					text("Der Fortschritt aktualisiert sich live.").fg(readableText(TEXT_MUTED, SURFACE_ALT))
						.length(1)))
				.rounded()
				.padding(1)
				.bg(SURFACE_ALT)
				.borderColor(CYAN_NEON)
				.fill();
		}

		if (viewModel.executionResult() != null) {
			List<Element> items = new ArrayList<>();
			items.add(text("Generierung abgeschlossen. Der QR-Workflow wurde erfolgreich ausgeführt.")
				.fg(readableText(TEXT_MUTED, SURFACE_ALT))
				.length(1));
			items.add(text(" ").length(1));
			items.add(PhotoGalleryWizardUi.previewCard("Erstellte Dateien", List.of(
					PhotoGalleryWizardUi.previewLine("Event-Code", viewModel.executionResult().eventCode(), LIME_NEON),
					PhotoGalleryWizardUi.previewLine("Codes", Integer.toString(viewModel.executionResult().codeCount()),
							TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("Seiten",
							Integer.toString(viewModel.executionResult().pageCount()), TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("CSV",
							viewModel.executionResult().csvPath().toAbsolutePath().toString(), TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("PDF",
							viewModel.executionResult().pdfPath().toAbsolutePath().toString(), TEXT_PRIMARY)),
					LIME_NEON));
			items.add(text(" ").length(1));
			items.add(panel("Weiter",
					text("Enter drücken, um mit der Ordnererstellung für die Foto-Ablage fortzufahren.")
						.fg(readableText(TEXT_MUTED, SURFACE)))
				.rounded()
				.padding(1)
				.bg(SURFACE)
				.borderColor(CYAN_NEON));
			return panel("Ergebnisse", column(items.toArray(new Element[0]))).rounded()
				.padding(1)
				.bg(SURFACE_ALT)
				.borderColor(LIME_NEON)
				.fill();
		}
		return panel("Ausführungsfehler", column(
				text("Der Workflow wurde nicht abgeschlossen. Eingaben oder Umgebung anpassen und erneut ausführen.")
					.fg(readableText(TEXT_MUTED, SURFACE_ALT))
					.length(1),
				text(" ").length(1),
				panel("Fehlerdetails",
						text(PhotoGalleryWizardUi.blankFallback(viewModel.executionMessage(), "Unbekannter Fehler"))
							.fg(readableText(ERROR_GLOW, SURFACE)))
					.rounded()
					.padding(1)
					.bg(SURFACE)
					.borderColor(ERROR_GLOW)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(ERROR_GLOW)
			.fill();
	}

}