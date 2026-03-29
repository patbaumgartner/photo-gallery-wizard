package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.AMBER_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardUploadStepView {

	private PhotoGalleryWizardUploadStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		Color accent = viewModel.activeStep().accent();
		if (viewModel.uploadInProgress()) {
			double progress = Math.max(0.0d, Math.min(1.0d, viewModel.uploadProgress()));
			return panel("PicPeak Hochladen", column(
					text("Upload läuft...").fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1), text(" ").length(1),
					text("Status: " + viewModel.uploadStage()).fg(readableText(TEXT_MUTED, SURFACE_ALT)).length(1),
					text(" ").length(1),
					gauge(progress).gaugeColor(AMBER_GLOW)
						.gaugeStyle(dev.tamboui.style.Style.create()
							.fg(readableText(TEXT_MUTED, SURFACE_ALT))
							.bg(SURFACE_ALT))
						.length(3),
					text(" ").length(1),
					text("Bitte warten, bis alle Galerien hochgeladen sind.").fg(readableText(TEXT_MUTED, SURFACE_ALT))
						.length(1)))
				.rounded()
				.padding(1)
				.bg(SURFACE_ALT)
				.borderColor(AMBER_GLOW)
				.fill();
		}

		return panel("PicPeak Hochladen",
				column(uploadIntro(viewModel), eventFolderList(viewModel, accent), selectedEventSummary(viewModel),
						uploadSummary(viewModel), uploadPlan(viewModel)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(AMBER_GLOW)
			.fill();
	}

	private static Element uploadIntro(PhotoGalleryWizardViewModel viewModel) {
		return column(text(
				"Fotos mit Wasserzeichen in PicPeak-Galerien hochladen. Jede Galerie erhält ihre Portraitfotos plus alle Klassenfotos.")
			.fg(readableText(TEXT_MUTED, SURFACE_ALT))
			.length(1), text(" ").length(1));
	}

	private static Element eventFolderList(PhotoGalleryWizardViewModel viewModel, Color accent) {
		if (viewModel.availableEventFolders().isEmpty()) {
			return column(text("Keine Event-Ordner gefunden.").fg(readableText(AMBER_GLOW, SURFACE_ALT)).length(1),
					text(" ").length(1));
		}
		List<Element> folderOptions = new ArrayList<>();
		for (int i = 0; i < viewModel.availableEventFolders().size(); i++) {
			folderOptions
				.add(PhotoGalleryWizardUi.previewLine(i == viewModel.selectedFolderIndex() ? "▶ Ausgewählt" : " ",
						viewModel.availableEventFolders().get(i).getFileName().toString(), accent));
		}
		return column(PhotoGalleryWizardUi.previewCard("Verfügbare Event-Ordner", folderOptions, AMBER_GLOW),
				text(" ").length(1));
	}

	private static Element uploadSummary(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.watermarkResult() == null) {
			return text(" ").length(1);
		}
		return column(PhotoGalleryWizardUi.previewCard("Wasserzeichen-Zusammenfassung",
				List.of(PhotoGalleryWizardUi.previewLine("Verarbeitet",
						viewModel.watermarkResult().totalProcessed() + " Fotos", TEXT_PRIMARY),
						PhotoGalleryWizardUi.previewLine("Ausgabeordner",
								viewModel.watermarkResult().outputFolders().size() + " Ordner", TEXT_PRIMARY)),
				AMBER_GLOW), text(" ").length(1));
	}

	private static Element selectedEventSummary(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.folderEventName() == null || viewModel.folderEventName().isBlank()) {
			return text(" ").length(1);
		}
		int portraitFolders = viewModel.folderCodes() == null ? 0 : viewModel.folderCodes().size();
		return column(PhotoGalleryWizardUi.previewCard("Ausgewählter Event-Ordner", List.of(
				PhotoGalleryWizardUi.previewLine("Event", viewModel.folderEventName(), TEXT_PRIMARY),
				PhotoGalleryWizardUi.previewLine("Ordner",
						(1 + portraitFolders) + " (1 Klassenfoto + " + portraitFolders + " Portrait)", TEXT_PRIMARY)),
				AMBER_GLOW), text(" ").length(1));
	}

	private static Element uploadPlan(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.galleriesWithId() == 0) {
			return column(
					PhotoGalleryWizardUi.previewCard("Upload-Plan", List.of(
							PhotoGalleryWizardUi.previewLine("Galerien", "0 mit PicPeak Event-ID", TEXT_PRIMARY),
							PhotoGalleryWizardUi.previewLine("Aktion", "Enter drücken, um den Upload zu starten",
									TEXT_PRIMARY)),
							AMBER_GLOW),
					text(" ").length(1),
					text("Keine Galerie-Codes mit PicPeak Event-IDs vorhanden. Upload wird übersprungen.")
						.fg(readableText(AMBER_GLOW, SURFACE_ALT))
						.length(1));
		}
		return PhotoGalleryWizardUi.previewCard("Upload-Plan", List.of(
				PhotoGalleryWizardUi.previewLine("Galerien", viewModel.galleriesWithId() + " mit PicPeak Event-ID",
						TEXT_PRIMARY),
				PhotoGalleryWizardUi.previewLine("Aktion", "Enter drücken, um den Upload zu starten", TEXT_PRIMARY)),
				AMBER_GLOW);
	}

}
