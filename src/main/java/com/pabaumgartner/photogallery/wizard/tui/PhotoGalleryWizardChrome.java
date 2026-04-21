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
				text("").length(1), text(stepStatus(viewModel)).fg(readableText(TEXT_PRIMARY, SURFACE_ALT)).length(1)))
			.focusable()
			.id("wizard-header-focus-anchor")
			.rounded()
			.padding(1)
			.bg(SURFACE)
			.borderColor(accent);
	}

	static Element body(PhotoGalleryWizardViewModel viewModel, Element currentStepContent) {
		return panel(() -> column(validationPanel(viewModel), currentStepContent)).rounded()
			.padding(1)
			.bg(SURFACE)
			.borderColor(viewModel.activeStep().accent())
			.fill();
	}

	static Element footer(PhotoGalleryWizardViewModel viewModel) {
		return panel(() -> column(
				row(spacer(), shortcutBadge("TAB", "Fokus", CYAN_NEON), text("  "),
						shortcutBadge("ENTER", "Weiter/Start", viewModel.activeStep().accent()), text("  "),
						shortcutBadge("F2", "Zur\u00fcck", backShortcutColor(viewModel)), text("  "),
						shortcutBadge("F3", "Neu", LIME_NEON), text("  "),
						shortcutBadge("CTRL+C", "Beenden", ERROR_GLOW))
					.length(1),
				row(spacer(), shortcutBadge("F4", "Ordner", AMBER_GLOW), text("  "),
						shortcutBadge("F5", "Wasserzeichen", PINK_NEON), text("  "),
						shortcutBadge("F6", "Hochladen", AMBER_GLOW))
					.length(1)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(viewModel.activeStep().accent());
	}

	private static Color backShortcutColor(PhotoGalleryWizardViewModel viewModel) {
		PhotoGalleryWizardStep activeStep = viewModel.activeStep();
		if (activeStep == PhotoGalleryWizardStep.SCHULFOTOS) {
			return BORDER_MUTED;
		}
		return PhotoGalleryWizardStep.values()[activeStep.ordinal() - 1].accent();
	}

	private static String stepStatus(PhotoGalleryWizardViewModel viewModel) {
		return "Schritt " + viewModel.activeStep().position() + "/" + PhotoGalleryWizardStep.values().length + ": "
				+ viewModel.activeStep().title() + " | " + viewModel.workflowStatusText();
	}

	private static Element validationPanel(PhotoGalleryWizardViewModel viewModel) {
		if (viewModel.validationMessage().isBlank()) {
			return text(" ").length(1);
		}
		return panel("Eingabeprüfung", text(viewModel.validationMessage()).fg(readableText(ERROR_GLOW, SURFACE_ALT)))
			.rounded()
			.padding(1)
			.bg(SURFACE_ALT)
			.borderColor(ERROR_GLOW);
	}

	private static Element shortcutBadge(String key, String action, Color accent) {
		return text(" " + key + " " + action + " ")
			.style(Style.create().fg(readableText(BACKGROUND, accent)).bg(accent).bold());
	}

}
