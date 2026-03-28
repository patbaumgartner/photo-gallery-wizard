package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardViewModelTest {

	private PhotoGalleryWizardViewModel createViewModel(PhotoGalleryWizardStep activeStep, boolean executionInProgress,
			double executionProgress, String executionStage, boolean watermarkInProgress, double watermarkProgress,
			String watermarkStage, boolean uploadInProgress, double uploadProgress, String uploadStage,
			WizardExecutionResult executionResult, ImageProcessingService.ImageProcessingResult watermarkResult,
			PicPeakService.UploadResult uploadResult, List<GalleryCode> folderCodes, int totalSteps) {
		return new PhotoGalleryWizardViewModel(activeStep, "", "", executionInProgress, executionProgress,
				executionStage, watermarkInProgress, watermarkProgress, watermarkStage, uploadInProgress,
				uploadProgress, uploadStage, executionResult, List.of(), 0, List.of(), "", folderCodes, List.of(), 0,
				watermarkResult, uploadResult, "", null, "https://base", "https://gallery", Path.of("csv"),
				Path.of("pdf"), 200, 3, 4, totalSteps, "wm.png", 1200);
	}

	@Test
	void currentStepTitleReturnsActiveStepTitle() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.SCHULFOTOS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.currentStepTitle()).isEqualTo("Schulfotos");
	}

	@Test
	void workflowStatusTextDuringExecution() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, true, 0.5, "Generating QR",
				false, 0, "", false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Generating QR");
	}

	@Test
	void workflowStatusTextDuringExecutionBlankStage() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, true, 0.5, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Generierung läuft");
	}

	@Test
	void workflowStatusTextDuringWatermark() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.WATERMARK, false, 0, "", true, 0.5,
				"Processing", false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Processing");
	}

	@Test
	void workflowStatusTextDuringWatermarkBlankStage() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.WATERMARK, false, 0, "", true, 0.5, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Wasserzeichen läuft");
	}

	@Test
	void workflowStatusTextDuringUpload() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.UPLOAD, false, 0, "", false, 0, "",
				true, 0.9, "Syncing", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Syncing");
	}

	@Test
	void workflowStatusTextDuringUploadBlankStage() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.UPLOAD, false, 0, "", false, 0, "",
				true, 0.9, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Upload läuft");
	}

	@Test
	void workflowStatusTextAtDone() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.DONE, false, 0, "", false, 0, "", false,
				0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Workflow abgeschlossen");
	}

	@Test
	void workflowStatusTextIdle() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.SCHULFOTOS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowStatusText()).isEqualTo("Bereit für: Schulfotos");
	}

	@Test
	void workflowProgressAtSchulfotos() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.SCHULFOTOS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowProgress()).isEqualTo(0.0d);
	}

	@Test
	void workflowProgressDuringExecution() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, true, 0.5, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		double expected = (2 + 0.5) / 7;
		assertThat(vm.workflowProgress()).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01d));
	}

	@Test
	void workflowProgressAfterResultsCompleted() {
		WizardExecutionResult result = new WizardExecutionResult("A", "B", 1, 1, Path.of("c"), Path.of("p"));
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, false, 0, "", false, 0, "",
				false, 0, "", result, null, null, List.of(), 7);
		double expected = (2 + 1.0) / 7;
		assertThat(vm.workflowProgress()).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01d));
	}

	@Test
	void workflowProgressAtDone() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.DONE, false, 0, "", false, 0, "", false,
				0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowProgress()).isEqualTo(1.0d);
	}

	@Test
	void workflowProgressClamped() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, true, 1.5, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.workflowProgress()).isLessThanOrEqualTo(1.0d);
	}

	@Test
	void galleriesWithIdCountsCodesWithPicPeakEventId() {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "p", "url", 42),
				new GalleryCode("EFGH-5678-STUV", "p", "url", 0), new GalleryCode("IJKL-9012-MNOP", "p", "url", 99));
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.FOLDERS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, codes, 7);
		assertThat(vm.galleriesWithId()).isEqualTo(2);
	}

	@Test
	void galleriesWithIdZeroWhenNoneHaveIds() {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "p"),
				new GalleryCode("EFGH-5678-STUV", "p"));
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.FOLDERS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, codes, 7);
		assertThat(vm.galleriesWithId()).isEqualTo(0);
	}

	@Test
	void footerHintForSchulfotos() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.SCHULFOTOS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("Enter");
	}

	@Test
	void footerHintForReview() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.REVIEW, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("Enter");
	}

	@Test
	void footerHintForResultsDuringExecution() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, true, 0.5, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("läuft");
	}

	@Test
	void footerHintForResultsWithResult() {
		WizardExecutionResult result = new WizardExecutionResult("A", "B", 1, 1, Path.of("c"), Path.of("p"));
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, false, 0, "", false, 0, "",
				false, 0, "", result, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("abgeschlossen");
	}

	@Test
	void footerHintForResultsWithoutResult() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.RESULTS, false, 0, "", false, 0, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("fehlgeschlagen");
	}

	@Test
	void footerHintForDone() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.DONE, false, 0, "", false, 0, "", false,
				0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("abgeschlossen");
	}

	@Test
	void footerHintForWatermarkInProgress() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.WATERMARK, false, 0, "", true, 0.5, "",
				false, 0, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("läuft");
	}

	@Test
	void footerHintForUploadInProgress() {
		PhotoGalleryWizardViewModel vm = createViewModel(PhotoGalleryWizardStep.UPLOAD, false, 0, "", false, 0, "",
				true, 0.5, "", null, null, null, List.of(), 7);
		assertThat(vm.footerHint()).contains("läuft");
	}

}
