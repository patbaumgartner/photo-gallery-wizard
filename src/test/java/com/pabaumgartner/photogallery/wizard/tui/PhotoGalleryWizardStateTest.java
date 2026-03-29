package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardStateTest {

	private PhotoGalleryWizardState state;

	@BeforeEach
	void setUp() {
		state = new PhotoGalleryWizardState();
	}

	@Test
	void initialStepIsSchulfotos() {
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
	}

	@Test
	void cannotGoBackFromSchulfotos() {
		assertThat(state.canGoBack()).isFalse();
	}

	@Test
	void canGoBackFromReview() {
		state.advanceToReview();
		assertThat(state.canGoBack()).isTrue();
	}

	@Test
	void cannotGoBackDuringExecution() {
		state.advanceToResults();
		state.executionInProgress(true);
		assertThat(state.canGoBack()).isFalse();
	}

	@Test
	void cannotGoBackDuringWatermark() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		state.watermarkInProgress(true);
		assertThat(state.canGoBack()).isFalse();
	}

	@Test
	void cannotGoBackDuringUpload() {
		state.activeStep(PhotoGalleryWizardStep.UPLOAD);
		state.uploadInProgress(true);
		assertThat(state.canGoBack()).isFalse();
	}

	@Test
	void goBackMovesToPreviousStep() {
		state.activeStep(PhotoGalleryWizardStep.RESULTS);
		state.goBack();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
	}

	@Test
	void goBackReturnsToOriginAfterJump() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		state.jumpToFolders();
		state.goBack();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
	}

	@Test
	void goBackReturnsToOriginAfterJumpFromReview() {
		state.activeStep(PhotoGalleryWizardStep.REVIEW);
		state.jumpToUpload();
		state.goBack();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
	}

	@Test
	void goBackRemainsLinearAfterNormalAdvance() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		state.jumpToFolders();
		state.advanceToReview();
		state.goBack();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
	}

	@Test
	void goBackClearsValidationMessage() {
		state.activeStep(PhotoGalleryWizardStep.REVIEW);
		state.validationMessage("Some error");
		state.goBack();
		assertThat(state.validationMessage()).isEmpty();
	}

	@Test
	void advanceToReview() {
		state.advanceToReview();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
	}

	@Test
	void advanceToResults() {
		state.advanceToResults();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.RESULTS);
	}

	@Test
	void advanceToFolders() {
		state.advanceToFolders();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.FOLDERS);
	}

	@Test
	void advanceToWatermark() {
		state.advanceToWatermark();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
	}

	@Test
	void advanceToUpload() {
		state.advanceToUpload();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.UPLOAD);
	}

	@Test
	void advanceToDone() {
		state.advanceToDone();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.DONE);
	}

	@Test
	void jumpToFoldersClearsValidation() {
		state.validationMessage("error");
		state.jumpToFolders();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.FOLDERS);
		assertThat(state.validationMessage()).isEmpty();
	}

	@Test
	void jumpToWatermarkClearsValidation() {
		state.validationMessage("error");
		state.jumpToWatermark();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
		assertThat(state.validationMessage()).isEmpty();
	}

	@Test
	void jumpToUploadClearsValidation() {
		state.validationMessage("error");
		state.jumpToUpload();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.UPLOAD);
		assertThat(state.validationMessage()).isEmpty();
	}

	@Test
	void validationMessageSetAndClear() {
		state.validationMessage("Error occurred");
		assertThat(state.validationMessage()).isEqualTo("Error occurred");
		state.clearValidationMessage();
		assertThat(state.validationMessage()).isEmpty();
	}

	@Test
	void executionMessageSetAndClear() {
		state.executionMessage("Running");
		assertThat(state.executionMessage()).isEqualTo("Running");
		state.clearExecutionMessage();
		assertThat(state.executionMessage()).isEmpty();
	}

	@Test
	void uploadMessageSetAndClear() {
		state.uploadMessage("Uploading");
		assertThat(state.uploadMessage()).isEqualTo("Uploading");
		state.clearUploadMessage();
		assertThat(state.uploadMessage()).isEmpty();
	}

	@Test
	void executionProgressTracking() {
		state.executionInProgress(true);
		state.executionProgress(0.5d);
		state.executionStage("Generating QR codes");
		assertThat(state.executionInProgress()).isTrue();
		assertThat(state.executionProgress()).isEqualTo(0.5d);
		assertThat(state.executionStage()).isEqualTo("Generating QR codes");
	}

	@Test
	void watermarkProgressTracking() {
		state.watermarkInProgress(true);
		state.watermarkProgress(0.75d);
		state.watermarkStage("Processing images");
		assertThat(state.watermarkInProgress()).isTrue();
		assertThat(state.watermarkProgress()).isEqualTo(0.75d);
		assertThat(state.watermarkStage()).isEqualTo("Processing images");
	}

	@Test
	void uploadProgressTracking() {
		state.uploadInProgress(true);
		state.uploadProgress(0.9d);
		state.uploadStage("Syncing galleries");
		assertThat(state.uploadInProgress()).isTrue();
		assertThat(state.uploadProgress()).isEqualTo(0.9d);
		assertThat(state.uploadStage()).isEqualTo("Syncing galleries");
	}

	@Test
	void executionResultStoredAndRetrieved() {
		WizardExecutionResult result = new WizardExecutionResult("ABCD", "Test", 5, 1, Path.of("csv"), Path.of("pdf"));
		state.executionResult(result);
		assertThat(state.executionResult()).isEqualTo(result);
	}

	@Test
	void csvFileSelectionNavigation() {
		state.selectedCsvIndex(0);

		state.selectNextCsv(3);
		assertThat(state.selectedCsvIndex()).isEqualTo(1);

		state.selectNextCsv(3);
		assertThat(state.selectedCsvIndex()).isEqualTo(2);

		state.selectNextCsv(3);
		assertThat(state.selectedCsvIndex()).isEqualTo(2);

		state.selectPreviousCsv();
		assertThat(state.selectedCsvIndex()).isEqualTo(1);

		state.selectPreviousCsv();
		assertThat(state.selectedCsvIndex()).isEqualTo(0);

		state.selectPreviousCsv();
		assertThat(state.selectedCsvIndex()).isEqualTo(0);
	}

	@Test
	void folderSelectionNavigation() {
		state.selectedFolderIndex(0);

		state.selectNextFolder(3);
		assertThat(state.selectedFolderIndex()).isEqualTo(1);

		state.selectNextFolder(3);
		assertThat(state.selectedFolderIndex()).isEqualTo(2);

		state.selectNextFolder(3);
		assertThat(state.selectedFolderIndex()).isEqualTo(2);

		state.selectPreviousFolder();
		assertThat(state.selectedFolderIndex()).isEqualTo(1);

		state.selectPreviousFolder();
		assertThat(state.selectedFolderIndex()).isEqualTo(0);

		state.selectPreviousFolder();
		assertThat(state.selectedFolderIndex()).isEqualTo(0);
	}

	@Test
	void resetCsvSelection() {
		state.selectedCsvIndex(5);
		state.resetCsvSelection();
		assertThat(state.selectedCsvIndex()).isEqualTo(0);
	}

	@Test
	void resetFolderSelection() {
		state.selectedFolderIndex(5);
		state.resetFolderSelection();
		assertThat(state.selectedFolderIndex()).isEqualTo(0);
	}

	@Test
	void clampCsvSelectionWhenBeyondSize() {
		state.selectedCsvIndex(5);
		state.availableCsvFiles(List.of(Path.of("a.csv"), Path.of("b.csv")));
		state.clampCsvSelection();
		assertThat(state.selectedCsvIndex()).isEqualTo(0);
	}

	@Test
	void clampCsvSelectionWhenWithinRange() {
		state.selectedCsvIndex(1);
		state.availableCsvFiles(List.of(Path.of("a.csv"), Path.of("b.csv")));
		state.clampCsvSelection();
		assertThat(state.selectedCsvIndex()).isEqualTo(1);
	}

	@Test
	void clampFolderSelectionWhenBeyondSize() {
		state.selectedFolderIndex(5);
		state.availableEventFolders(List.of(Path.of("event1")));
		state.clampFolderSelection();
		assertThat(state.selectedFolderIndex()).isEqualTo(0);
	}

	@Test
	void clampFolderSelectionWhenWithinRange() {
		state.selectedFolderIndex(0);
		state.availableEventFolders(List.of(Path.of("event1")));
		state.clampFolderSelection();
		assertThat(state.selectedFolderIndex()).isEqualTo(0);
	}

	@Test
	void overwriteConfirmed() {
		assertThat(state.overwriteConfirmed()).isFalse();
		state.overwriteConfirmed(true);
		assertThat(state.overwriteConfirmed()).isTrue();
	}

	@Test
	void folderCodesSetting() {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"));
		state.folderCodes(codes);
		assertThat(state.folderCodes()).isEqualTo(codes);
	}

	@Test
	void folderEventNameSetting() {
		state.folderEventName("Klasse 5a");
		assertThat(state.folderEventName()).isEqualTo("Klasse 5a");
	}

	@Test
	void createdFoldersSetting() {
		List<Path> folders = List.of(Path.of("a"), Path.of("b"));
		state.createdFolders(folders);
		assertThat(state.createdFolders()).isEqualTo(folders);
	}

	@Test
	void anyStepInProgressReflectsAllFlags() {
		assertThat(state.anyStepInProgress()).isFalse();

		state.executionInProgress(true);
		assertThat(state.anyStepInProgress()).isTrue();
		state.executionInProgress(false);

		state.watermarkInProgress(true);
		assertThat(state.anyStepInProgress()).isTrue();
		state.watermarkInProgress(false);

		state.uploadInProgress(true);
		assertThat(state.anyStepInProgress()).isTrue();
	}

	@Test
	void resetAllResetsToInitialState() {
		state.activeStep(PhotoGalleryWizardStep.DONE);
		state.validationMessage("error");
		state.executionMessage("msg");
		state.executionInProgress(true);
		state.executionProgress(0.5d);
		state.executionStage("stage");
		state.overwriteConfirmed(true);
		state.watermarkInProgress(true);
		state.watermarkProgress(0.75d);
		state.watermarkStage("wm-stage");
		state.uploadInProgress(true);
		state.uploadProgress(0.9d);
		state.uploadStage("up-stage");
		state.executionResult(new WizardExecutionResult("ABCD", "Test", 5, 1, Path.of("csv"), Path.of("pdf")));
		state.availableCsvFiles(List.of(Path.of("a.csv")));
		state.createdFolders(List.of(Path.of("folder")));
		state.folderEventName("Event");
		state.folderCodes(List.of(new GalleryCode("ABCD-1234-WXYZ")));
		state.availableEventFolders(List.of(Path.of("event")));
		state.uploadMessage("upload-msg");
		state.selectedCsvIndex(3);
		state.selectedFolderIndex(2);

		state.resetAll();

		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(state.validationMessage()).isEmpty();
		assertThat(state.executionMessage()).isEmpty();
		assertThat(state.executionInProgress()).isFalse();
		assertThat(state.executionProgress()).isEqualTo(0.0d);
		assertThat(state.executionStage()).isEmpty();
		assertThat(state.overwriteConfirmed()).isFalse();
		assertThat(state.watermarkInProgress()).isFalse();
		assertThat(state.watermarkProgress()).isEqualTo(0.0d);
		assertThat(state.watermarkStage()).isEmpty();
		assertThat(state.uploadInProgress()).isFalse();
		assertThat(state.uploadProgress()).isEqualTo(0.0d);
		assertThat(state.uploadStage()).isEmpty();
		assertThat(state.executionResult()).isNull();
		assertThat(state.availableCsvFiles()).isEmpty();
		assertThat(state.createdFolders()).isEmpty();
		assertThat(state.folderEventName()).isEmpty();
		assertThat(state.folderCodes()).isEmpty();
		assertThat(state.availableEventFolders()).isEmpty();
		assertThat(state.watermarkResult()).isNull();
		assertThat(state.uploadResult()).isNull();
		assertThat(state.uploadMessage()).isEmpty();
		assertThat(state.selectedCsvIndex()).isEqualTo(0);
		assertThat(state.selectedFolderIndex()).isEqualTo(0);
	}

}
