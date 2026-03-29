package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WizardRequestTest {

	@Test
	void allFieldsPreservedWhenProvided() {
		WizardRequest request = new WizardRequest("AB12", "Klasse 1a", 17, Path.of("codes.csv"), Path.of("qr.pdf"),
				"https://base", "https://gallery?code=", "https://logo.png", 200, 3, 4, true, "GALERIE CODE",
				"GALERIE PASSWORT", true, "2026-03-29");
		assertThat(request.eventCode()).isEqualTo("AB12");
		assertThat(request.eventName()).isEqualTo("Klasse 1a");
		assertThat(request.codeCount()).isEqualTo(17);
		assertThat(request.csvPath()).isEqualTo(Path.of("codes.csv"));
		assertThat(request.pdfPath()).isEqualTo(Path.of("qr.pdf"));
		assertThat(request.baseUrl()).isEqualTo("https://base");
		assertThat(request.galleryUrl()).isEqualTo("https://gallery?code=");
		assertThat(request.logoUrl()).isEqualTo("https://logo.png");
		assertThat(request.qrSize()).isEqualTo(200);
		assertThat(request.gridColumns()).isEqualTo(3);
		assertThat(request.gridRows()).isEqualTo(4);
		assertThat(request.showCuttingLines()).isTrue();
		assertThat(request.galleryCodeLabel()).isEqualTo("GALERIE CODE");
		assertThat(request.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
		assertThat(request.picPeakEnabled()).isTrue();
		assertThat(request.picPeakEventDate()).isEqualTo("2026-03-29");
	}

	@Test
	void nullEventCodeDefaultsToEmpty() {
		WizardRequest request = createWithNullField("eventCode");
		assertThat(request.eventCode()).isEmpty();
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		WizardRequest request = createWithNullField("eventName");
		assertThat(request.eventName()).isEmpty();
	}

	@Test
	void nullBaseUrlDefaultsToEmpty() {
		WizardRequest request = createWithNullField("baseUrl");
		assertThat(request.baseUrl()).isEmpty();
	}

	@Test
	void nullGalleryUrlDefaultsToEmpty() {
		WizardRequest request = createWithNullField("galleryUrl");
		assertThat(request.galleryUrl()).isEmpty();
	}

	@Test
	void nullLogoUrlDefaultsToEmpty() {
		WizardRequest request = createWithNullField("logoUrl");
		assertThat(request.logoUrl()).isEmpty();
	}

	@Test
	void nullGalleryCodeLabelDefaultsToEmpty() {
		WizardRequest request = createWithNullField("galleryCodeLabel");
		assertThat(request.galleryCodeLabel()).isEmpty();
	}

	@Test
	void nullGalleryPasswordLabelDefaultsToEmpty() {
		WizardRequest request = createWithNullField("galleryPasswordLabel");
		assertThat(request.galleryPasswordLabel()).isEmpty();
	}

	@Test
	void nullPicPeakEventDateDefaultsToEmpty() {
		WizardRequest request = createWithNullField("picPeakEventDate");
		assertThat(request.picPeakEventDate()).isEmpty();
	}

	@Test
	void allNullsCoercedToDefaults() {
		WizardRequest request = new WizardRequest(null, null, 0, Path.of("a.csv"), Path.of("a.pdf"), null, null, null,
				0, 0, 0, false, null, null, false, null);
		assertThat(request.eventCode()).isEmpty();
		assertThat(request.eventName()).isEmpty();
		assertThat(request.baseUrl()).isEmpty();
		assertThat(request.galleryUrl()).isEmpty();
		assertThat(request.logoUrl()).isEmpty();
		assertThat(request.galleryCodeLabel()).isEmpty();
		assertThat(request.galleryPasswordLabel()).isEmpty();
		assertThat(request.picPeakEventDate()).isEmpty();
	}

	@Test
	void recordEquality() {
		WizardRequest a = new WizardRequest("AB12", "Event", 5, Path.of("a.csv"), Path.of("a.pdf"), "base", "gallery",
				"logo", 200, 3, 4, true, "C", "P", false, "2026-01-01");
		WizardRequest b = new WizardRequest("AB12", "Event", 5, Path.of("a.csv"), Path.of("a.pdf"), "base", "gallery",
				"logo", 200, 3, 4, true, "C", "P", false, "2026-01-01");
		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	private WizardRequest createWithNullField(String fieldName) {
		return new WizardRequest("eventCode".equals(fieldName) ? null : "AB12",
				"eventName".equals(fieldName) ? null : "Event", 1, Path.of("a.csv"), Path.of("a.pdf"),
				"baseUrl".equals(fieldName) ? null : "base", "galleryUrl".equals(fieldName) ? null : "gallery",
				"logoUrl".equals(fieldName) ? null : "logo", 200, 3, 4, true,
				"galleryCodeLabel".equals(fieldName) ? null : "C",
				"galleryPasswordLabel".equals(fieldName) ? null : "P", false,
				"picPeakEventDate".equals(fieldName) ? null : "2026-01-01");
	}

}
