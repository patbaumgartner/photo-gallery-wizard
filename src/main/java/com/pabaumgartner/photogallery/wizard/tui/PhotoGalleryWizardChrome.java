package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.AMBER_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.BACKGROUND;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.BORDER_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.ERROR_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.LIME_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.PINK_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardChrome {

	private PhotoGalleryWizardChrome() {
	}

	static Element header(PhotoGalleryWizardViewModel viewModel) {
		Color accent = viewModel.activeStep().accent();
		return panel(() -> column(
				row(text(" SCHULFOTOS WORKFLOW WIZARD ")
					.style(Style.create().fg(readableText(BACKGROUND, accent)).bg(accent).bold())).length(1),
				text("").length(1),
				text(stepIndicator(viewModel)).fg(readableText(TEXT_PRIMARY, SURFACE_ALT)).length(1)))
			.focusable()
			.id("wizard-header-focus-anchor")
			.rounded()
			.padding(1)
			.bg(SURFACE)
			.borderColor(accent)
			.length(7);
	}

	static Element body(PhotoGalleryWizardViewModel viewModel, Element currentStepContent) {
		return panel(() -> column(validationPanel(viewModel), currentStepContent)).rounded()
			.padding(1)
			.bg(SURFACE)
			.borderColor(viewModel.activeStep().accent())
			.fill();
	}

	static Element footer(PhotoGalleryWizardViewModel viewModel) {
		return panel(() -> column(row(spacer(), shortcutBadge("TAB", "Fokus", CYAN_NEON), text("  "),
				shortcutBadge("ENTER", "Weiter/Start", viewModel.activeStep().accent()), text("  "),
				shortcutBadge("F2", "Zur\u00fcck", backShortcutColor(viewModel)), text("  "),
				shortcutBadge("F3", "Neu", LIME_NEON), text("  "), shortcutBadge("F4", "Ordner", AMBER_GLOW),
				text("  "), shortcutBadge("F5", "Wasserzeichen", PINK_NEON), text("  "),
				shortcutBadge("F6", "Hochladen", AMBER_GLOW), text("  "),
				shortcutBadge("CTRL+C", "Beenden", ERROR_GLOW))
			.length(1))).rounded().padding(1).bg(SURFACE_ALT).borderColor(viewModel.activeStep().accent()).length(5);
	}

	private static Color backShortcutColor(PhotoGalleryWizardViewModel viewModel) {
		PhotoGalleryWizardStep activeStep = viewModel.activeStep();
		if (activeStep == PhotoGalleryWizardStep.SCHULFOTOS) {
			return BORDER_MUTED;
		}
		return PhotoGalleryWizardStep.values()[activeStep.ordinal() - 1].accent();
	}

	private static String stepIndicator(PhotoGalleryWizardViewModel viewModel) {
		PhotoGalleryWizardStep[] steps = PhotoGalleryWizardStep.values();
		int active = viewModel.activeStep().ordinal();
		StringBuilder builder = new StringBuilder("  ");
		for (int i = 0; i < steps.length; i++) {
			if (i > 0) {
				builder.append("  ───  ");
			}
			if (i < active) {
				builder.append("● ").append(steps[i].title());
			}
			else if (i == active) {
				builder.append("◉ ").append(steps[i].title());
			}
			else {
				builder.append("○ ").append(steps[i].title());
			}
		}
		return builder.toString();
	}

	private static Element validationPanel(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.validationMessage().isBlank()) {
			return text(" ").length(1);
		}
		return panel("Eingabeprüfung", text(viewModel.validationMessage()).fg(readableText(ERROR_GLOW, SURFACE_ALT)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(ERROR_GLOW)
			.length(5);
	}

	private static Element shortcutBadge(String key, String action, Color accent) {
		return text(" " + key + " " + action + " ")
			.style(Style.create().fg(readableText(BACKGROUND, accent)).bg(accent).bold());
	}

}
