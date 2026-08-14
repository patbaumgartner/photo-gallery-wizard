package com.pabaumgartner.photogallery.wizard.tui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.ImageProperties;
import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
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
import dev.tamboui.tui.event.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the wizard the way the key handler does: fill in the form, press a key, then
 * check what would be rendered. Steps that hand work to the background thread are only
 * exercised up to the point where they hand it over.
 */
class PhotoGalleryWizardTuiTest {

	private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@TempDir
	Path outputDir;

	private PhotoGalleryWizardTui tui;

	private static SchulfotosProperties schulfotosProperties(String outputDirectory) {
		return new SchulfotosProperties("https://example.com/schulfotos", "https://example.com/schulfotos/?code=", 17,
				200, 3, 4, true, "GALERIE CODE", "GALERIE PASSWORT", outputDirectory, "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
				"configuration/logo.png", "klassenfoto", "portrait-", "-watermarked", 9);
	}

	private static PicPeakProperties picPeakProperties() {
		return new PicPeakProperties(false, true, null, null, null, null, null, null, null, false, null, 0, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, null,
				null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private PhotoGalleryWizardTui createTui(AppProperties appProperties) {
		SchulfotosProperties schulfotos = schulfotosProperties(outputDir.toString());
		PicPeakProperties picPeak = picPeakProperties();
		ImageProperties image = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		CodeGeneratorService codeGenerator = new CodeGeneratorService(schulfotos);
		PicPeakService picPeakService = new PicPeakService(picPeak, schulfotos, codeGenerator);
		WizardWorkflowService workflow = new WizardWorkflowService(codeGenerator, new CsvWriterService(),
				new QrCodeGeneratorService(), new PdfGeneratorService(image), picPeakService,
				new CsvUploadService(schulfotos, picPeak));
		return new PhotoGalleryWizardTui(appProperties, image, schulfotos, picPeak, workflow, new CsvReaderService(),
				new FolderStructureService(schulfotos), new ImageProcessingService(image, schulfotos), picPeakService);
	}

	private void writeCsv(String fileName, String className, String... codes) throws IOException {
		StringBuilder csv = new StringBuilder("Number,Code,Password,Class Name,URL,PicPeak Event ID\n");
		for (int i = 0; i < codes.length; i++) {
			csv.append(i + 1)
				.append(',')
				.append(codes[i])
				.append(",pw")
				.append(i)
				.append(',')
				.append(className)
				.append(",https://gallery/?code=")
				.append(codes[i])
				.append(',')
				.append(i + 1)
				.append('\n');
		}
		Files.writeString(outputDir.resolve(fileName), csv.toString(), StandardCharsets.UTF_8);
	}

	private void fillValidForm() {
		tui.formState().setTextValue("schoolClassName", "Klasse 3a");
		tui.formState().setTextValue("schoolEventCode", "ab12");
		tui.formState().setTextValue("schoolShootingDate", "25.03.2026");
		tui.formState().setTextValue("schoolCodeCount", "5");
	}

	@BeforeEach
	void setUp() {
		tui = createTui(new AppProperties("WXYZ", "Klasse 1b"));
	}

	@Test
	void theFormStartsFromTheConfiguredEvent() {
		assertThat(tui.formState().textValue("schoolEventCode")).isEqualTo("WXYZ");
		assertThat(tui.formState().textValue("schoolClassName")).isEqualTo("Klasse 1b");
		assertThat(tui.formState().textValue("schoolCodeCount")).isEqualTo("17");
		assertThat(tui.formState().textValue("schoolShootingDate")).isEqualTo(LocalDate.now().format(GERMAN_DATE));
		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
	}

	@Test
	void anUnconfiguredEventCodeIsSeededFromTheConfiguredCharset() {
		PhotoGalleryWizardTui freshTui = createTui(new AppProperties("", ""));

		assertThat(freshTui.formState().textValue("schoolEventCode")).matches("^[A-Z]{4}$");
		assertThat(freshTui.formState().textValue("schoolClassName")).isEmpty();
	}

	@Test
	void aValidFormAdvancesToTheReviewStep() {
		fillValidForm();

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
		assertThat(tui.viewModel().validationMessage()).isEmpty();
	}

	@Test
	void aMissingClassNameIsRejected() {
		fillValidForm();
		tui.formState().setTextValue("schoolClassName", "   ");

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(tui.viewModel().validationMessage()).contains("Klassenname");
	}

	@Test
	void aMalformedEventCodeIsRejected() {
		fillValidForm();
		tui.formState().setTextValue("schoolEventCode", "AB1");

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(tui.viewModel().validationMessage()).contains("Event-Code");
	}

	@Test
	void aMalformedShootingDateIsRejected() {
		fillValidForm();
		tui.formState().setTextValue("schoolShootingDate", "2026-03-25");

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(tui.viewModel().validationMessage()).contains("TT.MM.JJJJ");
	}

	@Test
	void aCodeCountThatIsNotAPositiveNumberIsRejected() {
		fillValidForm();
		tui.formState().setTextValue("schoolCodeCount", "0");

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(tui.viewModel().validationMessage()).contains("Anzahl Codes");
	}

	@Test
	void theRequestNormalisesTheEventCodeAndTheShootingDate() {
		fillValidForm();

		assertThat(tui.viewModel().requestPreview().eventCode()).isEqualTo("AB12");
		assertThat(tui.viewModel().requestPreview().picPeakEventDate()).isEqualTo("2026-03-25");
		assertThat(tui.viewModel().requestPreview().codeCount()).isEqualTo(5);
	}

	@Test
	void anUnusableCodeCountAndDateStillProduceAPreview() {
		fillValidForm();
		tui.formState().setTextValue("schoolCodeCount", "seventeen");
		tui.formState().setTextValue("schoolShootingDate", "not a date");

		assertThat(tui.viewModel().requestPreview().codeCount()).isEqualTo(17);
		assertThat(tui.viewModel().requestPreview().picPeakEventDate()).isEmpty();
	}

	@Test
	void outputFileNamesAreDerivedFromTheClassNameAndEventCode() {
		fillValidForm();

		assertThat(tui.viewModel().csvPath()).isEqualTo(outputDir.resolve("Klasse-3a-AB12-codes.csv"));
		assertThat(tui.viewModel().pdfPath()).isEqualTo(outputDir.resolve("Klasse-3a-AB12-qr-codes.pdf"));
	}

	@Test
	void theFileNamesUseTheSameEventCodeAsTheCodesInsideThem() {
		fillValidForm();
		tui.formState().setTextValue("schoolEventCode", "ab12");

		String eventCode = tui.viewModel().requestPreview().eventCode();

		assertThat(eventCode).isEqualTo("AB12");
		assertThat(tui.viewModel().csvPath().getFileName().toString()).contains(eventCode);
		assertThat(tui.viewModel().pdfPath().getFileName().toString()).contains(eventCode);
	}

	@Test
	void aClassNameWithPathSeparatorsCannotEscapeTheOutputDirectory() {
		fillValidForm();
		tui.formState().setTextValue("schoolClassName", "../../etc");

		assertThat(tui.viewModel().csvPath().getParent()).isEqualTo(outputDir);
		assertThat(tui.viewModel().csvPath().getFileName().toString()).doesNotContain("/").doesNotContain("\\");
	}

	@Test
	void anExistingOutputFileHasToBeConfirmedBeforeItIsOverwritten() throws IOException {
		fillValidForm();
		Files.writeString(outputDir.resolve("Klasse-3a-AB12-codes.csv"), "existing");
		tui.pressKey(KeyCode.ENTER);

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
		assertThat(tui.viewModel().validationMessage()).contains("existieren bereits").contains("Enter");
	}

	@Test
	void startingOverRestoresTheConfiguredDefaults() {
		fillValidForm();
		tui.pressKey(KeyCode.ENTER);

		tui.pressKey(KeyCode.F3);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.SCHULFOTOS);
		assertThat(tui.formState().textValue("schoolEventCode")).isEqualTo("WXYZ");
		assertThat(tui.formState().textValue("schoolClassName")).isEqualTo("Klasse 1b");
	}

	@Test
	void jumpingToAStepAndBackReturnsToWhereTheJumpStarted() {
		fillValidForm();
		tui.pressKey(KeyCode.ENTER);

		tui.pressKey(KeyCode.F4);
		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.FOLDERS);

		tui.pressKey(KeyCode.F2);
		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.REVIEW);
	}

	@Test
	void theFoldersStepPreselectsTheCsvThatBelongsToTheCurrentForm() throws IOException {
		writeCsv("Klasse-3a-AB12-codes.csv", "Klasse 3a", "AB12-1111-2222", "AB12-3333-4444");
		writeCsv("aaa-other-codes.csv", "Andere Klasse", "ZZZZ-1111-2222");
		fillValidForm();

		tui.pressKey(KeyCode.F4);

		assertThat(tui.viewModel().availableCsvFiles()).hasSize(2);
		assertThat(tui.viewModel().availableCsvFiles().get(tui.viewModel().selectedCsvIndex()).getFileName())
			.hasToString("Klasse-3a-AB12-codes.csv");
	}

	@Test
	void theFoldersStepCreatesOneFolderPerCsvRowPlusTheClassPhotoFolder() throws IOException {
		writeCsv("Klasse-3a-AB12-codes.csv", "Klasse 3a", "AB12-1111-2222", "AB12-3333-4444");
		fillValidForm();
		tui.pressKey(KeyCode.F4);

		tui.pressKey(KeyCode.ENTER);

		Path eventDir = outputDir.resolve("Klasse 3a-AB12");
		assertThat(eventDir.resolve("klassenfoto")).isDirectory();
		assertThat(eventDir.resolve("portrait-1")).isDirectory();
		assertThat(eventDir.resolve("portrait-2")).isDirectory();
		assertThat(eventDir.resolve("portrait-3")).doesNotExist();
		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
		assertThat(tui.viewModel().folderEventName()).isEqualTo("Klasse 3a-AB12");
	}

	@Test
	void theFoldersStepJustMovesOnWhenThereIsNoCsvYet() {
		fillValidForm();
		tui.pressKey(KeyCode.F4);

		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
		assertThat(tui.viewModel().createdFolders()).isEmpty();
	}

	@Test
	void selectingAnEventFolderLoadsTheCodesFromItsMatchingCsv() throws IOException {
		writeCsv("Klasse-3a-AB12-codes.csv", "Klasse 3a", "AB12-1111-2222", "AB12-3333-4444");
		fillValidForm();
		tui.pressKey(KeyCode.F4);
		tui.pressKey(KeyCode.ENTER);

		tui.pressKey(KeyCode.F5);

		assertThat(tui.viewModel().availableEventFolders()).hasSize(1);
		assertThat(tui.viewModel().folderCodes()).extracting(GalleryCode::code)
			.containsExactly("AB12-1111-2222", "AB12-3333-4444");
		assertThat(tui.viewModel().galleriesWithId()).isEqualTo(2);
	}

	@Test
	void theWatermarkStepReportsThatThereIsNothingToProcess() {
		fillValidForm();

		tui.pressKey(KeyCode.F5);
		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().activeStep()).isEqualTo(PhotoGalleryWizardStep.WATERMARK);
		assertThat(tui.viewModel().validationMessage()).contains("Keine Event-Ordner");
	}

	@Test
	void theUploadStepRefusesToRunWithoutCodesForTheSelectedFolder() throws IOException {
		Files.createDirectories(outputDir.resolve("Klasse 3a-AB12/klassenfoto"));
		fillValidForm();

		tui.pressKey(KeyCode.F6);
		tui.pressKey(KeyCode.ENTER);

		assertThat(tui.viewModel().availableEventFolders()).hasSize(1);
		assertThat(tui.viewModel().uploadMessage()).contains("keine passende CSV");
	}

	@Test
	void arrowKeysMoveTheEventFolderSelectionWithinBounds() throws IOException {
		Files.createDirectories(outputDir.resolve("event-a/klassenfoto"));
		Files.createDirectories(outputDir.resolve("event-b/klassenfoto"));
		fillValidForm();
		tui.pressKey(KeyCode.F5);

		assertThat(tui.viewModel().selectedFolderIndex()).isZero();
		tui.pressKey(KeyCode.UP);
		assertThat(tui.viewModel().selectedFolderIndex()).isZero();

		tui.pressKey(KeyCode.DOWN);
		assertThat(tui.viewModel().selectedFolderIndex()).isEqualTo(1);
		tui.pressKey(KeyCode.DOWN);
		assertThat(tui.viewModel().selectedFolderIndex()).isEqualTo(1);
	}

}
