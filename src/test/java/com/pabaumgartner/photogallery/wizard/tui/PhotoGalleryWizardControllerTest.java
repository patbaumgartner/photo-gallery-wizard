package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardControllerTest {

	private PhotoGalleryWizardState state;

	private PhotoGalleryWizardController controller;

	private TestActions actions;

	@BeforeEach
	void setUp() {
		state = new PhotoGalleryWizardState();
		controller = new PhotoGalleryWizardController(state);
		actions = new TestActions();
	}

	@Test
	void escapeIsAlwaysUnhandled() {
		EventResult result = controller.handleKeyEvent(KeyCode.ESCAPE, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	@Test
	void f2GoesBackWhenNotOnFirstStep() {
		state.activeStep(PhotoGalleryWizardStep.REVIEW);
		EventResult result = controller.handleKeyEvent(KeyCode.F2, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
	}

	@Test
	void f2DoesNothingOnFirstStep() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		EventResult result = controller.handleKeyEvent(KeyCode.F2, actions);
		assertThat(result).isNotEqualTo(EventResult.HANDLED);
	}

	@Test
	void f2DoesNothingDuringExecution() {
		state.activeStep(PhotoGalleryWizardStep.RESULTS);
		state.executionInProgress(true);
		EventResult result = controller.handleKeyEvent(KeyCode.F2, actions);
		assertThat(result).isNotEqualTo(EventResult.HANDLED);
	}

	@Test
	void f2DoesNothingDuringWatermark() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		state.watermarkInProgress(true);
		EventResult result = controller.handleKeyEvent(KeyCode.F2, actions);
		assertThat(result).isNotEqualTo(EventResult.HANDLED);
	}

	@Test
	void f2DoesNothingDuringUpload() {
		state.activeStep(PhotoGalleryWizardStep.UPLOAD);
		state.uploadInProgress(true);
		EventResult result = controller.handleKeyEvent(KeyCode.F2, actions);
		assertThat(result).isNotEqualTo(EventResult.HANDLED);
	}

	@Test
	void enterOnSchulfotosSubmits() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.submitSchulfotosCalled).isTrue();
	}

	@Test
	void enterOnReviewExecutesWorkflow() {
		state.activeStep(PhotoGalleryWizardStep.REVIEW);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.executeWorkflowCalled).isTrue();
	}

	@Test
	void enterOnResultsAdvancesWhenHasResult() {
		state.activeStep(PhotoGalleryWizardStep.RESULTS);
		actions.hasResult = true;
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.prepareFoldersCalled).isTrue();
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.FOLDERS);
	}

	@Test
	void enterOnResultsDoesNotAdvanceWithoutResult() {
		state.activeStep(PhotoGalleryWizardStep.RESULTS);
		actions.hasResult = false;
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.RESULTS);
	}

	@Test
	void enterOnFoldersExecutesFolderCreation() {
		state.activeStep(PhotoGalleryWizardStep.FOLDERS);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.executeFolderCreationCalled).isTrue();
	}

	@Test
	void enterOnWatermarkExecutesWatermark() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.executeWatermarkCalled).isTrue();
	}

	@Test
	void enterOnUploadExecutesUpload() {
		state.activeStep(PhotoGalleryWizardStep.UPLOAD);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(actions.executeUploadCalled).isTrue();
	}

	@Test
	void enterOnDoneIsUnhandled() {
		state.activeStep(PhotoGalleryWizardStep.DONE);
		EventResult result = controller.handleKeyEvent(KeyCode.ENTER, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	@Test
	void arrowUpOnFoldersSelectsPreviousCsv() {
		state.activeStep(PhotoGalleryWizardStep.FOLDERS);
		actions.csvCount = 3;
		state.selectedCsvIndex(2);
		EventResult result = controller.handleKeyEvent(KeyCode.UP, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.selectedCsvIndex()).isEqualTo(1);
	}

	@Test
	void arrowDownOnFoldersSelectsNextCsv() {
		state.activeStep(PhotoGalleryWizardStep.FOLDERS);
		actions.csvCount = 3;
		state.selectedCsvIndex(0);
		EventResult result = controller.handleKeyEvent(KeyCode.DOWN, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.selectedCsvIndex()).isEqualTo(1);
	}

	@Test
	void arrowOnFoldersWithZeroCsvCountIsUnhandled() {
		state.activeStep(PhotoGalleryWizardStep.FOLDERS);
		actions.csvCount = 0;
		EventResult result = controller.handleKeyEvent(KeyCode.UP, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	@Test
	void arrowUpOnWatermarkSelectsPreviousFolder() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		actions.folderCount = 3;
		state.selectedFolderIndex(2);
		EventResult result = controller.handleKeyEvent(KeyCode.UP, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.selectedFolderIndex()).isEqualTo(1);
	}

	@Test
	void arrowDownOnWatermarkSelectsNextFolder() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		actions.folderCount = 3;
		state.selectedFolderIndex(0);
		EventResult result = controller.handleKeyEvent(KeyCode.DOWN, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.selectedFolderIndex()).isEqualTo(1);
	}

	@Test
	void arrowOnWatermarkWithZeroFoldersIsUnhandled() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		actions.folderCount = 0;
		EventResult result = controller.handleKeyEvent(KeyCode.DOWN, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	@Test
	void arrowOnOtherStepsIsUnhandled() {
		state.activeStep(PhotoGalleryWizardStep.REVIEW);
		EventResult result = controller.handleKeyEvent(KeyCode.UP, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	@Test
	void f4JumpsToFolders() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		EventResult result = controller.handleKeyEvent(KeyCode.F4, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.FOLDERS);
		assertThat(actions.prepareFoldersCalled).isTrue();
	}

	@Test
	void f5JumpsToWatermark() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		EventResult result = controller.handleKeyEvent(KeyCode.F5, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
		assertThat(actions.prepareWatermarkCalled).isTrue();
	}

	@Test
	void f6JumpsToUpload() {
		state.activeStep(PhotoGalleryWizardStep.SCHULFOTOS);
		EventResult result = controller.handleKeyEvent(KeyCode.F6, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.UPLOAD);
		assertThat(actions.prepareUploadCalled).isTrue();
	}

	@Test
	void f3ResetsToSchulfotos() {
		state.activeStep(PhotoGalleryWizardStep.DONE);
		EventResult result = controller.handleKeyEvent(KeyCode.F3, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(actions.resetFlowCalled).isTrue();
	}

	@Test
	void f3ResetsFromAnyStep() {
		state.activeStep(PhotoGalleryWizardStep.WATERMARK);
		EventResult result = controller.handleKeyEvent(KeyCode.F3, actions);
		assertThat(result).isEqualTo(EventResult.HANDLED);
		assertThat(state.activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(actions.resetFlowCalled).isTrue();
	}

	@Test
	void unknownKeyIsUnhandled() {
		EventResult result = controller.handleKeyEvent(KeyCode.TAB, actions);
		assertThat(result).isEqualTo(EventResult.UNHANDLED);
	}

	static class TestActions implements PhotoGalleryWizardController.Actions {

		boolean resetFlowCalled;

		boolean prepareFoldersCalled;

		boolean prepareWatermarkCalled;

		boolean prepareUploadCalled;

		boolean submitSchulfotosCalled;

		boolean executeWorkflowCalled;

		boolean executeFolderCreationCalled;

		boolean executeWatermarkCalled;

		boolean executeUploadCalled;

		boolean hasResult;

		int csvCount;

		int folderCount;

		@Override
		public void resetFlow() {
			resetFlowCalled = true;
		}

		@Override
		public void prepareFoldersStep() {
			prepareFoldersCalled = true;
		}

		@Override
		public void prepareWatermarkStep() {
			prepareWatermarkCalled = true;
		}

		@Override
		public void prepareUploadStep() {
			prepareUploadCalled = true;
		}

		@Override
		public void submitSchulfotos() {
			submitSchulfotosCalled = true;
		}

		@Override
		public void executeWorkflow() {
			executeWorkflowCalled = true;
		}

		@Override
		public void executeFolderCreation() {
			executeFolderCreationCalled = true;
		}

		@Override
		public void executeWatermark() {
			executeWatermarkCalled = true;
		}

		@Override
		public void executeUpload() {
			executeUploadCalled = true;
		}

		@Override
		public boolean hasExecutionResult() {
			return hasResult;
		}

		@Override
		public int csvFileCount() {
			return csvCount;
		}

		@Override
		public int eventFolderCount() {
			return folderCount;
		}

	}

}
