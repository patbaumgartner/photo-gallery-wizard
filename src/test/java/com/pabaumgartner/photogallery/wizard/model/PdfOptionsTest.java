package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfOptionsTest {

	@Test
	void allFieldsPreservedWhenProvided() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, true, "Event", "https://base", "https://logo",
				"CODE", "PASSWORD");
		assertThat(opts.outputPath()).isEqualTo(Path.of("out.pdf"));
		assertThat(opts.gridColumns()).isEqualTo(3);
		assertThat(opts.gridRows()).isEqualTo(4);
		assertThat(opts.showCuttingLines()).isTrue();
		assertThat(opts.eventName()).isEqualTo("Event");
		assertThat(opts.baseUrl()).isEqualTo("https://base");
		assertThat(opts.logoUrl()).isEqualTo("https://logo");
		assertThat(opts.galleryCodeLabel()).isEqualTo("CODE");
		assertThat(opts.galleryPasswordLabel()).isEqualTo("PASSWORD");
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, null, "base", "logo", "C", "P");
		assertThat(opts.eventName()).isEmpty();
	}

	@Test
	void nullBaseUrlDefaultsToEmpty() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", null, "logo", "C", "P");
		assertThat(opts.baseUrl()).isEmpty();
	}

	@Test
	void nullLogoUrlDefaultsToEmpty() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", "base", null, "C", "P");
		assertThat(opts.logoUrl()).isEmpty();
	}

	@Test
	void nullGalleryCodeLabelDefaultsToGalerieCode() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", "base", "logo", null, "P");
		assertThat(opts.galleryCodeLabel()).isEqualTo("GALERIE CODE");
	}

	@Test
	void blankGalleryCodeLabelDefaultsToGalerieCode() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", "base", "logo", "   ", "P");
		assertThat(opts.galleryCodeLabel()).isEqualTo("GALERIE CODE");
	}

	@Test
	void nullGalleryPasswordLabelDefaultsToGaleriePasswort() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", "base", "logo", "C", null);
		assertThat(opts.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
	}

	@Test
	void blankGalleryPasswordLabelDefaultsToGaleriePasswort() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 3, 4, false, "E", "base", "logo", "C", "  ");
		assertThat(opts.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
	}

	@Test
	void allNullsCoercedToDefaults() {
		PdfOptions opts = new PdfOptions(Path.of("out.pdf"), 1, 1, false, null, null, null, null, null);
		assertThat(opts.eventName()).isEmpty();
		assertThat(opts.baseUrl()).isEmpty();
		assertThat(opts.logoUrl()).isEmpty();
		assertThat(opts.galleryCodeLabel()).isEqualTo("GALERIE CODE");
		assertThat(opts.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
	}

}
