package com.pabaumgartner.photogallery.wizard.tui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardStepTest {

	@Test
	void schulfotosProperties() {
		assertThat(PhotoGalleryWizardStep.SCHULFOTOS.title()).isEqualTo("Schulfotos");
		assertThat(PhotoGalleryWizardStep.SCHULFOTOS.accent()).isEqualTo(TuiPalette.CYAN_NEON);
		assertThat(PhotoGalleryWizardStep.SCHULFOTOS.position()).isEqualTo(1);
	}

	@Test
	void reviewProperties() {
		assertThat(PhotoGalleryWizardStep.REVIEW.title()).isEqualTo("Überprüfung");
		assertThat(PhotoGalleryWizardStep.REVIEW.accent()).isEqualTo(TuiPalette.CYAN_NEON);
		assertThat(PhotoGalleryWizardStep.REVIEW.position()).isEqualTo(2);
	}

	@Test
	void resultsProperties() {
		assertThat(PhotoGalleryWizardStep.RESULTS.title()).isEqualTo("Ergebnisse");
		assertThat(PhotoGalleryWizardStep.RESULTS.accent()).isEqualTo(TuiPalette.LIME_NEON);
		assertThat(PhotoGalleryWizardStep.RESULTS.position()).isEqualTo(3);
	}

	@Test
	void foldersProperties() {
		assertThat(PhotoGalleryWizardStep.FOLDERS.title()).isEqualTo("Ordner");
		assertThat(PhotoGalleryWizardStep.FOLDERS.accent()).isEqualTo(TuiPalette.AMBER_GLOW);
		assertThat(PhotoGalleryWizardStep.FOLDERS.position()).isEqualTo(4);
	}

	@Test
	void watermarkProperties() {
		assertThat(PhotoGalleryWizardStep.WATERMARK.title()).isEqualTo("Wasserzeichen");
		assertThat(PhotoGalleryWizardStep.WATERMARK.accent()).isEqualTo(TuiPalette.PINK_NEON);
		assertThat(PhotoGalleryWizardStep.WATERMARK.position()).isEqualTo(5);
	}

	@Test
	void uploadProperties() {
		assertThat(PhotoGalleryWizardStep.UPLOAD.title()).isEqualTo("Hochladen");
		assertThat(PhotoGalleryWizardStep.UPLOAD.accent()).isEqualTo(TuiPalette.AMBER_GLOW);
		assertThat(PhotoGalleryWizardStep.UPLOAD.position()).isEqualTo(6);
	}

	@Test
	void doneProperties() {
		assertThat(PhotoGalleryWizardStep.DONE.title()).isEqualTo("Fertig");
		assertThat(PhotoGalleryWizardStep.DONE.accent()).isEqualTo(TuiPalette.LIME_NEON);
		assertThat(PhotoGalleryWizardStep.DONE.position()).isEqualTo(7);
	}

	@ParameterizedTest
	@EnumSource(PhotoGalleryWizardStep.class)
	void allStepsHaveNonBlankTitle(PhotoGalleryWizardStep step) {
		assertThat(step.title()).isNotBlank();
	}

	@ParameterizedTest
	@EnumSource(PhotoGalleryWizardStep.class)
	void allStepsHaveNonNullAccent(PhotoGalleryWizardStep step) {
		assertThat(step.accent()).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(PhotoGalleryWizardStep.class)
	void positionIsOrdinalPlusOne(PhotoGalleryWizardStep step) {
		assertThat(step.position()).isEqualTo(step.ordinal() + 1);
	}

	@Test
	void totalStepCount() {
		assertThat(PhotoGalleryWizardStep.values()).hasSize(7);
	}

}
