package com.pabaumgartner.photogallery.wizard.tui;

import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.BORDER_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.ERROR_GLOW;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.SURFACE_ALT;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_MUTED;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

final class PhotoGalleryWizardUi {

	private static final int LABEL_WIDTH = 20;

	private PhotoGalleryWizardUi() {
	}

	static FormFieldElement textField(FormFieldElement field, Color accent) {
		return field.labelWidth(LABEL_WIDTH)
			.labelStyle(Style.create().fg(readableText(accent, SURFACE_ALT)).bold())
			.errorStyle(Style.create().fg(readableText(ERROR_GLOW, SURFACE_ALT)).bold())
			.rounded()
			.borderColor(BORDER_MUTED)
			.focusedBorderColor(accent)
			.errorBorderColor(ERROR_GLOW)
			.arrowNavigation(true);
	}

	static FormFieldElement booleanField(FormFieldElement field, Color accent) {
		return field.labelWidth(LABEL_WIDTH)
			.labelStyle(Style.create().fg(readableText(accent, SURFACE_ALT)).bold())
			.rounded()
			.borderColor(BORDER_MUTED)
			.focusedBorderColor(accent)
			.checkedColor(readableText(accent, SURFACE_ALT))
			.uncheckedColor(readableText(TEXT_MUTED, SURFACE_ALT))
			.checkedSymbol("■")
			.uncheckedSymbol("□");
	}

	static Element previewCard(String title, List<Element> lines, Color accent) {
		return panel(title, column(lines.toArray(new Element[0]))).rounded().padding(1).bg(SURFACE).borderColor(accent);
	}

	static Element previewLine(String label, String value, Color valueColor) {
		return row(text(label + ": ").fg(readableText(TEXT_MUTED, SURFACE)).bold(),
				text(value).fg(readableText(valueColor, SURFACE)))
			.length(1);
	}

	static String booleanLabel(boolean value) {
		return value ? "aktiviert" : "deaktiviert";
	}

	static String blankFallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

}