package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.toolkit.element.Element;
import org.junit.jupiter.api.Test;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardUiTest {

	private static List<String> options(int count) {
		List<String> options = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			options.add("option-" + i);
		}
		return options;
	}

	private static List<Element> lines(int optionCount, int selectedIndex, int terminalHeight) {
		PhotoGalleryWizardUi.updateTerminalHeight(terminalHeight);
		return PhotoGalleryWizardUi.selectablePreviewLines(options(optionCount), selectedIndex, CYAN_NEON);
	}

	@Test
	void noOptionsProduceNoLines() {
		assertThat(PhotoGalleryWizardUi.selectablePreviewLines(List.of(), 0, CYAN_NEON)).isEmpty();
	}

	@Test
	void aListShorterThanTheWindowIsShownWithoutScrollMarkers() {
		assertThat(lines(4, 0, 40)).hasSize(4);
	}

	@Test
	void theFirstPageOfALongListOnlyGetsATrailingMarker() {
		// A 40-row terminal shows 9 options, plus one "more entries below" line.
		assertThat(lines(20, 0, 40)).hasSize(10);
	}

	@Test
	void theLastPageOfALongListOnlyGetsALeadingMarker() {
		assertThat(lines(20, 19, 40)).hasSize(10);
	}

	@Test
	void aSelectionInTheMiddleGetsMarkersOnBothSides() {
		assertThat(lines(20, 10, 40)).hasSize(11);
	}

	@Test
	void anOutOfRangeSelectionIsClampedInsteadOfFailing() {
		assertThat(lines(20, 999, 40)).hasSize(10);
		assertThat(lines(20, -5, 40)).hasSize(10);
	}

	@Test
	void aShorterTerminalShowsFewerOptions() {
		assertThat(lines(20, 0, 20)).hasSize(4);
		assertThat(lines(20, 0, 26)).hasSize(6);
		assertThat(lines(20, 0, 30)).hasSize(8);
		assertThat(lines(20, 0, 60)).hasSize(12);
	}

	@Test
	void aNonPositiveTerminalHeightKeepsTheLastKnownSize() {
		PhotoGalleryWizardUi.updateTerminalHeight(40);
		PhotoGalleryWizardUi.updateTerminalHeight(0);

		assertThat(PhotoGalleryWizardUi.selectablePreviewLines(options(20), 0, CYAN_NEON)).hasSize(10);
	}

	@Test
	void sanitizeErrorFallsBackWhenThereIsNothingToShow() {
		assertThat(PhotoGalleryWizardUi.sanitizeError(null)).isEqualTo("Unbekannter Fehler");
		assertThat(PhotoGalleryWizardUi.sanitizeError("   ")).isEqualTo("Unbekannter Fehler");
		assertThat(PhotoGalleryWizardUi.sanitizeError("<html><body></body></html>")).isEqualTo("Unbekannter Fehler");
	}

	@Test
	void sanitizeErrorStripsMarkupAndCollapsesWhitespace() {
		assertThat(PhotoGalleryWizardUi.sanitizeError("<b>Upload</b>\n\tfailed  hard")).isEqualTo("Upload failed hard");
	}

	@Test
	void sanitizeErrorTruncatesLongMessages() {
		String message = "x".repeat(500);

		String sanitized = PhotoGalleryWizardUi.sanitizeError(message);

		assertThat(sanitized).hasSize(201).endsWith("…");
	}

	@Test
	void sanitizeErrorKeepsMessagesAtTheLimitIntact() {
		String message = "x".repeat(200);

		assertThat(PhotoGalleryWizardUi.sanitizeError(message)).isEqualTo(message);
	}

	@Test
	void blankFallbackReplacesOnlyMissingValues() {
		assertThat(PhotoGalleryWizardUi.blankFallback(null, "fallback")).isEqualTo("fallback");
		assertThat(PhotoGalleryWizardUi.blankFallback("  ", "fallback")).isEqualTo("fallback");
		assertThat(PhotoGalleryWizardUi.blankFallback("value", "fallback")).isEqualTo("value");
	}

	@Test
	void booleanLabelIsLocalised() {
		assertThat(PhotoGalleryWizardUi.booleanLabel(true)).isEqualTo("aktiviert");
		assertThat(PhotoGalleryWizardUi.booleanLabel(false)).isEqualTo("deaktiviert");
	}

}
