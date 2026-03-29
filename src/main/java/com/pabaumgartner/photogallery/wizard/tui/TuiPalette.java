package com.pabaumgartner.photogallery.wizard.tui;

import java.util.Map;

import dev.tamboui.style.Color;

final class TuiPalette {

	static final Color BACKGROUND = Color.hex("#050816");

	static final Color SURFACE = Color.hex("#0B1020");

	static final Color SURFACE_ALT = Color.hex("#111933");

	static final Color CYAN_NEON = Color.hex("#00F5FF");

	static final Color PINK_NEON = Color.hex("#FF4FD8");

	static final Color LIME_NEON = Color.hex("#A7FF4F");

	static final Color AMBER_GLOW = Color.hex("#FFC857");

	static final Color TEXT_PRIMARY = Color.hex("#E6F1FF");

	static final Color TEXT_MUTED = Color.hex("#A9B7D0");

	static final Color BORDER_MUTED = Color.hex("#27324F");

	static final Color ERROR_GLOW = Color.hex("#FF6B8B");

	private static final double WCAG_AA_MIN_CONTRAST = 4.5d;

	private static final Map<Color, String> COLOR_TO_HEX = Map.ofEntries(Map.entry(BACKGROUND, "#050816"),
			Map.entry(SURFACE, "#0B1020"), Map.entry(SURFACE_ALT, "#111933"), Map.entry(CYAN_NEON, "#00F5FF"),
			Map.entry(PINK_NEON, "#FF4FD8"), Map.entry(LIME_NEON, "#A7FF4F"), Map.entry(AMBER_GLOW, "#FFC857"),
			Map.entry(TEXT_PRIMARY, "#E6F1FF"), Map.entry(TEXT_MUTED, "#A9B7D0"), Map.entry(BORDER_MUTED, "#27324F"),
			Map.entry(ERROR_GLOW, "#FF6B8B"));

	private TuiPalette() {
	}

	static Color readableText(Color preferred, Color background) {
		if (contrastRatio(preferred, background) >= WCAG_AA_MIN_CONTRAST) {
			return preferred;
		}
		return bestReadableFallback(background);
	}

	static double contrastRatio(String foregroundHex, String backgroundHex) {
		double fgLuminance = relativeLuminance(foregroundHex);
		double bgLuminance = relativeLuminance(backgroundHex);
		double lighter = Math.max(fgLuminance, bgLuminance);
		double darker = Math.min(fgLuminance, bgLuminance);
		return (lighter + 0.05d) / (darker + 0.05d);
	}

	private static double contrastRatio(Color foreground, Color background) {
		return contrastRatio(hexOf(foreground), hexOf(background));
	}

	private static Color bestReadableFallback(Color background) {
		double primaryContrast = contrastRatio(TEXT_PRIMARY, background);
		double backgroundContrast = contrastRatio(BACKGROUND, background);
		return primaryContrast >= backgroundContrast ? TEXT_PRIMARY : BACKGROUND;
	}

	private static String hexOf(Color color) {
		String hex = COLOR_TO_HEX.get(color);
		if (hex == null) {
			throw new IllegalArgumentException("Unsupported color constant in WCAG contrast calculation.");
		}
		return hex;
	}

	private static double relativeLuminance(String hex) {
		int red = Integer.parseInt(hex.substring(1, 3), 16);
		int green = Integer.parseInt(hex.substring(3, 5), 16);
		int blue = Integer.parseInt(hex.substring(5, 7), 16);
		return 0.2126d * linearize(red / 255.0d) + 0.7152d * linearize(green / 255.0d)
				+ 0.0722d * linearize(blue / 255.0d);
	}

	private static double linearize(double channel) {
		return channel <= 0.03928d ? channel / 12.92d : Math.pow((channel + 0.055d) / 1.055d, 2.4d);
	}

}
