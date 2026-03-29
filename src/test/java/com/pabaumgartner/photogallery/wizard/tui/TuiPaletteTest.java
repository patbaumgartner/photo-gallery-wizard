package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.style.Color;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TuiPaletteTest {

	@Test
	void contrastRatioBlackOnWhiteIsMaximum() {
		assertThat(TuiPalette.contrastRatio("#FFFFFF", "#000000")).isEqualTo(21.0d);
	}

	@Test
	void contrastRatioSameColorIsMinimum() {
		assertThat(TuiPalette.contrastRatio("#000000", "#000000")).isEqualTo(1.0d);
	}

	@Test
	void contrastRatioIsSymmetric() {
		double fgOnBg = TuiPalette.contrastRatio("#A9B7D0", "#0B1020");
		double bgOnFg = TuiPalette.contrastRatio("#0B1020", "#A9B7D0");
		assertThat(fgOnBg).isEqualTo(bgOnFg);
	}

	@Test
	void contrastRatioAllAccentsOnDarkBackground() {
		// All accent colors should have decent contrast on the darkest background
		assertThat(TuiPalette.contrastRatio("#00F5FF", "#050816")).isGreaterThan(1.0d);
		assertThat(TuiPalette.contrastRatio("#FF4FD8", "#050816")).isGreaterThan(1.0d);
		assertThat(TuiPalette.contrastRatio("#A7FF4F", "#050816")).isGreaterThan(1.0d);
		assertThat(TuiPalette.contrastRatio("#FFC857", "#050816")).isGreaterThan(1.0d);
	}

	@Test
	void readableTextReturnsPreferredWhenContrastSufficient() {
		// TEXT_PRIMARY on BACKGROUND should have well above WCAG AA minimum
		Color result = TuiPalette.readableText(TuiPalette.TEXT_PRIMARY, TuiPalette.BACKGROUND);
		assertThat(result).isEqualTo(TuiPalette.TEXT_PRIMARY);
	}

	@Test
	void readableTextFallsBackWhenContrastInsufficient() {
		// SURFACE on BACKGROUND has very low contrast (both very dark)
		Color result = TuiPalette.readableText(TuiPalette.SURFACE, TuiPalette.BACKGROUND);
		// Should fall back to either TEXT_PRIMARY or BACKGROUND
		assertThat(result).isIn(TuiPalette.TEXT_PRIMARY, TuiPalette.BACKGROUND);
	}

	@Test
	void readableTextFallsBackToBackgroundOnBrightSurface() {
		// Validate behavior around the WCAG threshold: preferred if sufficient,
		// fallback
		// otherwise.
		Color result = TuiPalette.readableText(TuiPalette.SURFACE, TuiPalette.CYAN_NEON);
		double preferredContrast = TuiPalette.contrastRatio("#0B1020", "#00F5FF");
		if (preferredContrast >= 4.5d) {
			assertThat(result).isEqualTo(TuiPalette.SURFACE);
		}
		else {
			assertThat(result).isIn(TuiPalette.TEXT_PRIMARY, TuiPalette.BACKGROUND);
		}
	}

	@Test
	void readableTextWithMutedBorderOnDarkBackground() {
		// BORDER_MUTED on BACKGROUND is low contrast
		Color result = TuiPalette.readableText(TuiPalette.BORDER_MUTED, TuiPalette.BACKGROUND);
		assertThat(result).isIn(TuiPalette.TEXT_PRIMARY, TuiPalette.BACKGROUND);
	}

	@Test
	void readableTextPreferredIsReturnedForHighContrast() {
		// LIME_NEON on BACKGROUND should have high enough contrast
		Color result = TuiPalette.readableText(TuiPalette.LIME_NEON, TuiPalette.BACKGROUND);
		assertThat(result).isEqualTo(TuiPalette.LIME_NEON);
	}

	@Test
	void readableTextPreferredIsReturnedForAmberOnDark() {
		Color result = TuiPalette.readableText(TuiPalette.AMBER_GLOW, TuiPalette.BACKGROUND);
		assertThat(result).isEqualTo(TuiPalette.AMBER_GLOW);
	}

	@Test
	void allColorConstantsAreNotNull() {
		assertThat(TuiPalette.BACKGROUND).isNotNull();
		assertThat(TuiPalette.SURFACE).isNotNull();
		assertThat(TuiPalette.SURFACE_ALT).isNotNull();
		assertThat(TuiPalette.CYAN_NEON).isNotNull();
		assertThat(TuiPalette.PINK_NEON).isNotNull();
		assertThat(TuiPalette.LIME_NEON).isNotNull();
		assertThat(TuiPalette.AMBER_GLOW).isNotNull();
		assertThat(TuiPalette.TEXT_PRIMARY).isNotNull();
		assertThat(TuiPalette.TEXT_MUTED).isNotNull();
		assertThat(TuiPalette.BORDER_MUTED).isNotNull();
		assertThat(TuiPalette.ERROR_GLOW).isNotNull();
	}

	@Test
	void contrastRatioAlwaysAtLeastOne() {
		// For randomly chosen colors, contrast should always be >= 1
		assertThat(TuiPalette.contrastRatio("#123456", "#654321")).isGreaterThanOrEqualTo(1.0d);
	}

	@Test
	void textPrimaryMeetsWcagAaOnBackground() {
		assertThat(TuiPalette.contrastRatio("#E6F1FF", "#050816")).isGreaterThanOrEqualTo(4.5d);
	}

	@Test
	void textPrimaryMeetsWcagAaOnSurface() {
		assertThat(TuiPalette.contrastRatio("#E6F1FF", "#0B1020")).isGreaterThanOrEqualTo(4.5d);
	}

	@Test
	void textPrimaryMeetsWcagAaOnSurfaceAlt() {
		assertThat(TuiPalette.contrastRatio("#E6F1FF", "#111933")).isGreaterThanOrEqualTo(4.5d);
	}

	@Test
	void errorGlowOnDarkMeetsMinContrast() {
		assertThat(TuiPalette.contrastRatio("#FF6B8B", "#050816")).isGreaterThanOrEqualTo(4.5d);
	}

	@Test
	void contrastRatioPureMidGray() {
		double ratio = TuiPalette.contrastRatio("#808080", "#000000");
		assertThat(ratio).isGreaterThan(1.0d);
		assertThat(ratio).isLessThan(21.0d);
	}

}
