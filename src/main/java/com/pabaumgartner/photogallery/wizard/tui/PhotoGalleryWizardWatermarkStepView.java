package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.AMBER_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.PINK_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardWatermarkStepView {

	private PhotoGalleryWizardWatermarkStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		Color accent = viewModel.activeStep().accent();
		List<Element> lines = new ArrayList<>();
		if (viewModel.watermarkInProgress()) {
			double progress = Math.max(0.0d, Math.min(1.0d, viewModel.watermarkProgress()));
			return panel("Skalieren & Wasserzeichen", column(
					text("Wasserzeichen-Verarbeitung läuft...").fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1),
					text(" ").length(1),
					text("Status: " + viewModel.watermarkStage()).fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1),
					text(" ").length(1),
					gauge(progress).gaugeColor(PINK_NEON)
						.gaugeStyle(dev.tamboui.style.Style.create()
							.fg(readableText(TEXT_MUTED, SURFACE_ALT))
							.bg(SURFACE_ALT))
						.length(3),
					text(" ").length(1),
					text("Bitte warten, bis alle Bilder verarbeitet sind.").fg(readableText(TEXT_MUTED, SURFACE_ALT))
						.length(1)))
				.rounded()
				.padding(1)
				.bg(SURFACE_ALT)
				.borderColor(PINK_NEON)
				.fill();
		}

		lines.add(text(
				"Event-Ordner auswählen, um alle Fotos in den Unterordnern zu skalieren und mit Wasserzeichen zu versehen.")
			.fg(readableText(TEXT_MUTED, SURFACE_ALT))
			.length(1));
		lines.add(text(" ").length(1));
		if (viewModel.availableEventFolders().isEmpty()) {
			lines.add(text("Keine Event-Ordner mit Foto-Unterordnern gefunden. Zuerst den Ordner-Schritt ausführen.")
				.fg(readableText(AMBER_GLOW, SURFACE_ALT))
				.length(1));
		}
		else {
			List<Element> folderOptions = new ArrayList<>();
			for (int i = 0; i < viewModel.availableEventFolders().size(); i++) {
				folderOptions
					.add(PhotoGalleryWizardUi.previewLine(i == viewModel.selectedFolderIndex() ? "▶ Ausgewählt" : " ",
							viewModel.availableEventFolders().get(i).getFileName().toString(), accent));
			}
			lines.add(PhotoGalleryWizardUi.previewCard("Verfügbare Event-Ordner", folderOptions, PINK_NEON));
			lines.add(text(" ").length(1));
			lines.add(PhotoGalleryWizardUi.previewCard("Ausgewählter Event-Ordner", selectedFolderSummary(viewModel),
					PINK_NEON));
			lines.add(text(" ").length(1));
			lines.add(PhotoGalleryWizardUi.previewCard("Verarbeitung",
					List.of(PhotoGalleryWizardUi.previewLine("Skalierung",
							viewModel.resizeMaxEdge() + " px längste Kante", TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("Wasserzeichen", viewModel.watermarkPath(), TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("Ausgabe", "JPEG mit 90% Qualität in -watermarked Ordnern",
									TEXT_PRIMARY)),
					PINK_NEON));
		}
		return panel("Skalieren & Wasserzeichen", column(lines.toArray(new Element[0]))).rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(PINK_NEON)
			.fill();
	}

	private static List<Element> selectedFolderSummary(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.folderEventName() == null || viewModel.folderEventName().isBlank()) {
			return List.of(PhotoGalleryWizardUi.previewLine("Event", "Kein Ordner ausgewählt", TEXT_PRIMARY));
		}
		int portraitFolders = viewModel.folderCodes() == null ? 0 : viewModel.folderCodes().size();
		return List.of(PhotoGalleryWizardUi.previewLine("Event", viewModel.folderEventName(), TEXT_PRIMARY),
				PhotoGalleryWizardUi.previewLine("Ordner",
						(1 + portraitFolders) + " (1 Klassenfoto + " + portraitFolders + " Portrait)", TEXT_PRIMARY));
	}

}
