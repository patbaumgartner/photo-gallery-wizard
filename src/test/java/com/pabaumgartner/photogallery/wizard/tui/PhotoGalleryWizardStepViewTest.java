package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;
import dev.tamboui.toolkit.element.Element;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The step views only ever run inside a live terminal, so a null result or an empty
 * selection reaching them would otherwise surface as a crash during a shoot.
 */
class PhotoGalleryWizardStepViewTest {

	private static final WizardRequest REQUEST = new WizardRequest("ABCD", "Klasse 3a", 17, Path.of("codes.csv"),
			Path.of("codes.pdf"), "https://base", "https://gallery/?code=", "", 200, 3, 4, true, "CODE", "PW", true,
			"2026-03-25");

	private static PhotoGalleryWizardViewModel viewModel(PhotoGalleryWizardStep step, boolean inProgress,
			WizardExecutionResult executionResult, ImageProcessingService.ImageProcessingResult watermarkResult,
			PicPeakService.UploadResult uploadResult, List<Path> csvFiles, List<Path> eventFolders,
			List<GalleryCode> folderCodes) {
		return new PhotoGalleryWizardViewModel(step, "", "Etwas ging schief", inProgress, 0.5d, "Stage", inProgress,
				0.5d, "Stage", inProgress, 0.5d, "Stage", executionResult, csvFiles, 0, List.of(Path.of("created")),
				"Klasse 3a-ABCD", folderCodes, eventFolders, 0, watermarkResult, uploadResult, "Upload-Hinweis",
				REQUEST, "https://base", "https://gallery/?code=", Path.of("codes.csv"), Path.of("codes.pdf"), 200, 3,
				4, PhotoGalleryWizardStep.values().length, "wm.png", 1200);
	}

	private static PhotoGalleryWizardViewModel empty(PhotoGalleryWizardStep step) {
		return viewModel(step, false, null, null, null, List.of(), List.of(), List.of());
	}

	private static PhotoGalleryWizardViewModel populated(PhotoGalleryWizardStep step) {
		return viewModel(step, false,
				new WizardExecutionResult("ABCD", "Klasse 3a", 17, 2, Path.of("codes.csv"), Path.of("codes.pdf")),
				new ImageProcessingService.ImageProcessingResult(12, List.of(Path.of("klassenfoto-watermarked"))),
				new PicPeakService.UploadResult(3, 12, List.of("Ein Fehler")), List.of(Path.of("codes.csv")),
				List.of(Path.of("event")), List.of(new GalleryCode("ABCD-1234-WXYZ", "pw", "https://share", 7)));
	}

	private static PhotoGalleryWizardViewModel inProgress(PhotoGalleryWizardStep step) {
		return viewModel(step, true, null, null, null, List.of(Path.of("codes.csv")), List.of(Path.of("event")),
				List.of(new GalleryCode("ABCD-1234-WXYZ", "pw")));
	}

	@Test
	void everyStepViewRendersWhileNothingHasRunYet() {
		assertThat(PhotoGalleryWizardReviewStepView.render(empty(PhotoGalleryWizardStep.REVIEW))).isNotNull();
		assertThat(PhotoGalleryWizardResultsStepView.render(empty(PhotoGalleryWizardStep.RESULTS))).isNotNull();
		assertThat(PhotoGalleryWizardFoldersStepView.render(empty(PhotoGalleryWizardStep.FOLDERS))).isNotNull();
		assertThat(PhotoGalleryWizardWatermarkStepView.render(empty(PhotoGalleryWizardStep.WATERMARK))).isNotNull();
		assertThat(PhotoGalleryWizardUploadStepView.render(empty(PhotoGalleryWizardStep.UPLOAD))).isNotNull();
		assertThat(PhotoGalleryWizardDoneStepView.render(empty(PhotoGalleryWizardStep.DONE))).isNotNull();
	}

	@Test
	void everyStepViewRendersWithResults() {
		assertThat(PhotoGalleryWizardReviewStepView.render(populated(PhotoGalleryWizardStep.REVIEW))).isNotNull();
		assertThat(PhotoGalleryWizardResultsStepView.render(populated(PhotoGalleryWizardStep.RESULTS))).isNotNull();
		assertThat(PhotoGalleryWizardFoldersStepView.render(populated(PhotoGalleryWizardStep.FOLDERS))).isNotNull();
		assertThat(PhotoGalleryWizardWatermarkStepView.render(populated(PhotoGalleryWizardStep.WATERMARK))).isNotNull();
		assertThat(PhotoGalleryWizardUploadStepView.render(populated(PhotoGalleryWizardStep.UPLOAD))).isNotNull();
		assertThat(PhotoGalleryWizardDoneStepView.render(populated(PhotoGalleryWizardStep.DONE))).isNotNull();
	}

	@Test
	void everyStepViewRendersWhileAStepIsRunning() {
		assertThat(PhotoGalleryWizardResultsStepView.render(inProgress(PhotoGalleryWizardStep.RESULTS))).isNotNull();
		assertThat(PhotoGalleryWizardWatermarkStepView.render(inProgress(PhotoGalleryWizardStep.WATERMARK)))
			.isNotNull();
		assertThat(PhotoGalleryWizardUploadStepView.render(inProgress(PhotoGalleryWizardStep.UPLOAD))).isNotNull();
	}

	@Test
	void chromeRendersForEveryStep() {
		for (PhotoGalleryWizardStep step : PhotoGalleryWizardStep.values()) {
			PhotoGalleryWizardViewModel viewModel = populated(step);
			Element content = text("content");

			assertThat(PhotoGalleryWizardChrome.header(viewModel)).isNotNull();
			assertThat(PhotoGalleryWizardChrome.body(viewModel, content)).isNotNull();
			assertThat(PhotoGalleryWizardChrome.footer(viewModel)).isNotNull();
		}
	}

}
