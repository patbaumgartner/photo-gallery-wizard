package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.input.TextInputState;

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

	private static final int MIN_VISIBLE_OPTIONS = 3;

	private static final int DEFAULT_VISIBLE_OPTIONS = 7;

	private static final int MAX_VISIBLE_OPTIONS = 11;

	private static volatile int terminalHeight;

	private PhotoGalleryWizardUi() {
	}

	static FormFieldElement textField(FormFieldElement field, Color accent, TextInputState state) {
		return field.labelWidth(LABEL_WIDTH)
			.labelStyle(Style.create().fg(readableText(accent, SURFACE_ALT)).bold())
			.errorStyle(Style.create().fg(readableText(ERROR_GLOW, SURFACE_ALT)).bold())
			.rounded()
			.borderColor(BORDER_MUTED)
			.focusedBorderColor(accent)
			.errorBorderColor(ERROR_GLOW)
			.arrowNavigation(true)
			.onKeyEvent(event -> windowsBackspaceWorkaround(event, state));
	}

	// Workaround for https://github.com/tamboui/tamboui/issues/302
	// On Windows, Backspace sends BS (char 8) which TamboUI maps to Ctrl+H
	// instead of KeyCode.BACKSPACE. Intercept Ctrl+H and treat it as backspace.
	private static EventResult windowsBackspaceWorkaround(dev.tamboui.tui.event.KeyEvent event, TextInputState state) {
		if (event.code() == KeyCode.CHAR && event.hasCtrl() && event.character() == 'h') {
			state.deleteBackward();
			return EventResult.HANDLED;
		}
		return EventResult.UNHANDLED;
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

	static void updateTerminalHeight(int height) {
		if (height > 0) {
			terminalHeight = height;
		}
	}

	static List<Element> selectablePreviewLines(List<String> options, int selectedIndex, Color accent) {
		if (options.isEmpty()) {
			return List.of();
		}
		int safeSelectedIndex = Math.max(0, Math.min(selectedIndex, options.size() - 1));
		int visibleCount = Math.min(adaptiveVisibleOptionsCount(), options.size());
		int startIndex = Math.max(0, safeSelectedIndex - (visibleCount / 2));
		int endIndex = Math.min(options.size(), startIndex + visibleCount);
		startIndex = Math.max(0, endIndex - visibleCount);

		List<Element> visibleLines = new ArrayList<>();
		if (startIndex > 0) {
			visibleLines.add(previewLine("↑", startIndex + " weitere Einträge", TEXT_MUTED));
		}
		for (int i = startIndex; i < endIndex; i++) {
			visibleLines.add(previewLine(i == safeSelectedIndex ? "▶ Ausgewählt" : " ", options.get(i), accent));
		}
		if (endIndex < options.size()) {
			visibleLines.add(previewLine("↓", (options.size() - endIndex) + " weitere Einträge", TEXT_MUTED));
		}
		return visibleLines;
	}

	private static int adaptiveVisibleOptionsCount() {
		if (terminalHeight <= 0) {
			return DEFAULT_VISIBLE_OPTIONS;
		}
		int computed;
		if (terminalHeight <= 22) {
			computed = 3;
		}
		else if (terminalHeight <= 28) {
			computed = 5;
		}
		else if (terminalHeight <= 34) {
			computed = 7;
		}
		else if (terminalHeight <= 42) {
			computed = 9;
		}
		else {
			computed = 11;
		}
		return Math.max(MIN_VISIBLE_OPTIONS, Math.min(MAX_VISIBLE_OPTIONS, computed));
	}

	static String booleanLabel(boolean value) {
		return value ? "aktiviert" : "deaktiviert";
	}

	static String blankFallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	static String sanitizeError(String message) {
		if (message == null || message.isBlank()) {
			return "Unbekannter Fehler";
		}
		String clean = message.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
		if (clean.isBlank()) {
			return "Unbekannter Fehler";
		}
		return clean.length() > 200 ? clean.substring(0, 200) + "…" : clean;
	}

}