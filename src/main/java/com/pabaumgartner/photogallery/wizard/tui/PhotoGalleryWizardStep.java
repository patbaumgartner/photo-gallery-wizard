package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.style.Color;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.AMBER_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.LIME_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.PINK_NEON;

enum PhotoGalleryWizardStep {

	SCHULFOTOS("Schulfotos", CYAN_NEON), REVIEW("Überprüfung", CYAN_NEON), RESULTS("Ergebnisse", LIME_NEON),
	FOLDERS("Ordner", AMBER_GLOW), WATERMARK("Wasserzeichen", PINK_NEON), UPLOAD("Hochladen", AMBER_GLOW),
	DONE("Fertig", LIME_NEON);

	private final String title;

	private final Color accent;

	PhotoGalleryWizardStep(String title, Color accent) {
		this.title = title;
		this.accent = accent;
	}

	String title() {
		return title;
	}

	Color accent() {
		return accent;
	}

	int position() {
		return ordinal() + 1;
	}

}