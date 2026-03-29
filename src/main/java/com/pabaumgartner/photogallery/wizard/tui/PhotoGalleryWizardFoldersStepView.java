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
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardFoldersStepView {

	private PhotoGalleryWizardFoldersStepView() {
	}

	static Element render(PhotoGalleryWizardViewModel viewModel) {
		Color accent = viewModel.activeStep().accent();
		List<Element> lines = new ArrayList<>();
		lines.add(text("CSV-Datei aus schulfotos/ auswählen, um die Ordnerstruktur für die Foto-Ablage zu erstellen.")
			.fg(readableText(TEXT_MUTED, SURFACE_ALT))
			.length(1));
		lines.add(text(" ").length(1));
		if (viewModel.availableCsvFiles().isEmpty()) {
			lines.add(text("Keine CSV-Dateien in schulfotos/ gefunden. Zuerst CSV+PDF generieren (Schritte 1-3).")
				.fg(readableText(AMBER_GLOW, SURFACE_ALT))
				.length(1));
		}
		else {
			List<Element> csvLines = new ArrayList<>();
			for (int i = 0; i < viewModel.availableCsvFiles().size(); i++) {
				csvLines.add(PhotoGalleryWizardUi.previewLine(i == viewModel.selectedCsvIndex() ? "▶ Ausgewählt" : " ",
						viewModel.availableCsvFiles().get(i).getFileName().toString(), accent));
			}
			lines.add(PhotoGalleryWizardUi.previewCard("Verfügbare CSV-Dateien", csvLines, AMBER_GLOW));
			lines.add(text(" ").length(1));
			lines.add(PhotoGalleryWizardUi.previewCard("Anleitung", List.of(
					PhotoGalleryWizardUi.previewLine("1", "Enter drücken, um die Ordnerstruktur zu erstellen",
							TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("2", "Klassenfotos in klassenfoto/ ablegen", TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("3", "Portraitfotos in portrait-{id}/ ablegen", TEXT_PRIMARY),
					PhotoGalleryWizardUi.previewLine("4", "Dann zum Wasserzeichen-Schritt fortfahren", TEXT_PRIMARY)),
					AMBER_GLOW));
		}
		return panel("Ordner-Einrichtung", column(lines.toArray(new Element[0]))).rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(AMBER_GLOW)
			.fill();
	}

}
