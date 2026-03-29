package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class WizardWorkflowServiceTest {

	@TempDir
	Path tempDir;

	private WizardWorkflowService wizardService;

	private static PicPeakProperties disabledPicPeakProperties() {
		return new PicPeakProperties(false, null, null, null, null, null, null, null, false, null, 0, false, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, null, null,
				0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private static SchulfotosProperties defaultSchulfotosProperties() {
		return new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null, null,
				0);
	}

	private static AppProperties defaultAppProperties() {
		return new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
	}

	@BeforeEach
	void setUp() {
		CodeGeneratorService codeGen = new CodeGeneratorService(defaultSchulfotosProperties());
		CsvWriterService csvWriter = new CsvWriterService();
		QrCodeGeneratorService qrCodeGen = new QrCodeGeneratorService();
		PdfGeneratorService pdfGen = new PdfGeneratorService(defaultAppProperties());
		PicPeakProperties picPeakProps = disabledPicPeakProperties();
		PicPeakService picPeak = new PicPeakService(picPeakProps, defaultSchulfotosProperties(), codeGen);
		wizardService = new WizardWorkflowService(codeGen, csvWriter, qrCodeGen, pdfGen, picPeak);
	}

	@Test
	void executeProducesResultWithCorrectFields() throws IOException {
		Path csvPath = tempDir.resolve("test.csv");
		Path pdfPath = tempDir.resolve("test.pdf");

		WizardRequest request = new WizardRequest("ABCD", "Klasse 1a", 3, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, true, "CODE", "PW", false, "");

		WizardExecutionResult result = wizardService.execute(request);

		assertThat(result.eventCode()).isEqualTo("ABCD");
		assertThat(result.eventName()).isEqualTo("Klasse 1a");
		assertThat(result.codeCount()).isEqualTo(3);
		assertThat(result.pageCount()).isEqualTo(1);
		assertThat(result.csvPath()).isEqualTo(csvPath);
		assertThat(result.pdfPath()).isEqualTo(pdfPath);
	}

	@Test
	void executeCreatesOutputFiles() throws IOException {
		Path csvPath = tempDir.resolve("output.csv");
		Path pdfPath = tempDir.resolve("output.pdf");

		WizardRequest request = new WizardRequest("TEST", "TestEvent", 5, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, false, "C", "P", false, "");

		wizardService.execute(request);

		assertThat(csvPath).exists();
		assertThat(pdfPath).exists();
	}

	@Test
	void executeWith17CodesGenerates2Pages() throws IOException {
		Path csvPath = tempDir.resolve("17codes.csv");
		Path pdfPath = tempDir.resolve("17codes.pdf");

		WizardRequest request = new WizardRequest("ABCD", "BigClass", 17, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, true, "C", "P", false, "");

		WizardExecutionResult result = wizardService.execute(request);

		assertThat(result.codeCount()).isEqualTo(17);
		assertThat(result.pageCount()).isEqualTo(2);
	}

	@Test
	void executeWithProgressListenerReportsProgress() throws IOException {
		Path csvPath = tempDir.resolve("progress.csv");
		Path pdfPath = tempDir.resolve("progress.pdf");

		WizardRequest request = new WizardRequest("ABCD", "Test", 3, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, false, "C", "P", false, "");

		List<WizardWorkflowService.WorkflowProgress> events = new ArrayList<>();
		wizardService.execute(request, events::add);

		assertThat(events).isNotEmpty();
		assertThat(events.getFirst().percent()).isGreaterThan(0.0d);

		WizardWorkflowService.WorkflowProgress last = events.getLast();
		assertThat(last.percent()).isEqualTo(1.0d);
		assertThat(last.stage()).isEqualTo("Fertig");
	}

	@Test
	void executeWithSingleCode() throws IOException {
		Path csvPath = tempDir.resolve("single.csv");
		Path pdfPath = tempDir.resolve("single.pdf");

		WizardRequest request = new WizardRequest("SOLO", "Solo Event", 1, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, false, "C", "P", false, "");

		WizardExecutionResult result = wizardService.execute(request);

		assertThat(result.codeCount()).isEqualTo(1);
		assertThat(result.pageCount()).isEqualTo(1);
	}

	@Test
	void generatedCsvCanBeReadBack() throws IOException {
		Path csvPath = tempDir.resolve("readable.csv");
		Path pdfPath = tempDir.resolve("readable.pdf");

		WizardRequest request = new WizardRequest("ABCD", "Roundtrip", 5, csvPath, pdfPath, "https://base.com",
				"https://gallery.com/?code=", "", 200, 3, 4, false, "C", "P", false, "");

		wizardService.execute(request);

		CsvReaderService reader = new CsvReaderService();
		var readResult = reader.readCodes(csvPath);
		assertThat(readResult.eventName()).isEqualTo("Roundtrip");
		assertThat(readResult.codes()).hasSize(5);
	}

}
