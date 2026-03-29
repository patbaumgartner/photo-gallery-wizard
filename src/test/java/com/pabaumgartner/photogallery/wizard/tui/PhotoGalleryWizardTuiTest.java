package com.pabaumgartner.photogallery.wizard.tui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardTuiTest {

	@Test
	void contrastRatioMatchesWcagReferenceExample() {
		assertThat(TuiPalette.contrastRatio("#FFFFFF", "#000000")).isEqualTo(21.0d);
	}

	@Test
	void mutedTextPaletteMeetsAaContrastOnDarkPanels() {
		assertThat(TuiPalette.contrastRatio("#A9B7D0", "#0B1020")).isGreaterThanOrEqualTo(4.5d);
		assertThat(TuiPalette.contrastRatio("#A9B7D0", "#111933")).isGreaterThanOrEqualTo(4.5d);
	}

	@Test
	void badgeTextMeetsAaContrastOnBrightAccentBackgrounds() {
		assertThat(TuiPalette.contrastRatio("#050816", "#00F5FF")).isGreaterThanOrEqualTo(4.5d);
		assertThat(TuiPalette.contrastRatio("#050816", "#FF4FD8")).isGreaterThanOrEqualTo(4.5d);
	}

}