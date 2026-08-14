package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.ImageProperties;
import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.service.CodeGeneratorService;
import com.pabaumgartner.photogallery.wizard.service.CsvReaderService;
import com.pabaumgartner.photogallery.wizard.service.CsvUploadService;
import com.pabaumgartner.photogallery.wizard.service.CsvWriterService;
import com.pabaumgartner.photogallery.wizard.service.FolderStructureService;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PdfGeneratorService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;
import com.pabaumgartner.photogallery.wizard.service.QrCodeGeneratorService;
import com.pabaumgartner.photogallery.wizard.service.WizardWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoGalleryWizardTuiTest {

	@TempDir
	Path outputDir;

	private PhotoGalleryWizardTui tui;

	private static PicPeakProperties picPeakProperties() {
		return new PicPeakProperties(false, true, null, null, null, null, null, null, null, false, null, 0, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, null,
				null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private PhotoGalleryWizardTui createTui() {
		SchulfotosProperties schulfotos = new SchulfotosProperties("https://example.com/schulfotos",
				"https://example.com/schulfotos/?code=", 17, 200, 3, 4, true, "GALERIE CODE", "GALERIE PASSWORT",
				outputDir.toString(), "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "configuration/logo.png", "klassenfoto",
				"portrait-", "-watermarked", 9);
		PicPeakProperties picPeak = picPeakProperties();
		ImageProperties image = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		CodeGeneratorService codeGenerator = new CodeGeneratorService(schulfotos);
		PicPeakService picPeakService = new PicPeakService(picPeak, schulfotos, codeGenerator);
		WizardWorkflowService workflow = new WizardWorkflowService(codeGenerator, new CsvWriterService(),
				new QrCodeGeneratorService(), new PdfGeneratorService(image), picPeakService,
				new CsvUploadService(schulfotos, picPeak));
		return new PhotoGalleryWizardTui(new AppProperties("WXYZ", "Klasse 1b"), image, schulfotos, picPeak, workflow,
				new CsvReaderService(), new FolderStructureService(schulfotos),
				new ImageProcessingService(image, schulfotos), picPeakService);
	}

	@BeforeEach
	void setUp() {
		tui = createTui();
	}

	@Test
	void outputFileNamesAreDerivedFromTheClassNameAndEventCode() {
		tui.formState().setTextValue("schoolClassName", "Klasse 3a");
		tui.formState().setTextValue("schoolEventCode", "AB12");

		assertThat(tui.viewModel().csvPath()).isEqualTo(outputDir.resolve("Klasse-3a-AB12-codes.csv"));
		assertThat(tui.viewModel().pdfPath()).isEqualTo(outputDir.resolve("Klasse-3a-AB12-qr-codes.pdf"));
	}

	@Test
	void theFileNamesUseTheSameEventCodeAsTheCodesInsideThem() {
		tui.formState().setTextValue("schoolClassName", "Klasse 3a");
		tui.formState().setTextValue("schoolEventCode", "ab12");

		String eventCode = tui.viewModel().requestPreview().eventCode();

		assertThat(eventCode).isEqualTo("AB12");
		assertThat(tui.viewModel().csvPath().getFileName().toString()).contains(eventCode);
		assertThat(tui.viewModel().pdfPath().getFileName().toString()).contains(eventCode);
	}

}
