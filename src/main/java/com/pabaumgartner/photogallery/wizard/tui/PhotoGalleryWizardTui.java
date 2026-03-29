package com.pabaumgartner.photogallery.wizard.tui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.CsvReadResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import com.pabaumgartner.photogallery.wizard.service.CsvReaderService;
import com.pabaumgartner.photogallery.wizard.service.FolderStructureService;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;
import com.pabaumgartner.photogallery.wizard.service.WizardWorkflowService;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import org.springframework.stereotype.Component;

import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.BACKGROUND;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.CYAN_NEON;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.TEXT_PRIMARY;
import static com.pabaumgartner.photogallery.wizard.tui.TuiPalette.readableText;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.text;

@Component
public class PhotoGalleryWizardTui {

	private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final String EVENT_CODE_PATTERN = "^[A-Z0-9]{4}$";

	private final PhotoGalleryWizardState state = new PhotoGalleryWizardState();

	private final PhotoGalleryWizardController controller = new PhotoGalleryWizardController(state);

	private final FormState form;

	private final AppProperties appProperties;

	private final SchulfotosProperties schulfotosProperties;

	private final WizardWorkflowService workflowService;

	private final CsvReaderService csvReaderService;

	private final FolderStructureService folderStructureService;

	private final ImageProcessingService imageProcessingService;

	private final PicPeakService picPeakService;

	public PhotoGalleryWizardTui(AppProperties appProperties, SchulfotosProperties schulfotosProperties,
			PicPeakProperties picPeakProperties, WizardWorkflowService workflowService,
			CsvReaderService csvReaderService, FolderStructureService folderStructureService,
			ImageProcessingService imageProcessingService, PicPeakService picPeakService) {
		this.appProperties = appProperties;
		this.schulfotosProperties = schulfotosProperties;
		this.workflowService = workflowService;
		this.csvReaderService = csvReaderService;
		this.folderStructureService = folderStructureService;
		this.imageProcessingService = imageProcessingService;
		this.picPeakService = picPeakService;
		this.form = FormState.builder()
			.textField("schoolClassName", defaultSchoolClass(appProperties.eventName()))
			.textField("schoolEventCode", defaultEventCode(appProperties.eventCode()))
			.textField("schoolShootingDate", LocalDate.now().format(GERMAN_DATE))
			.textField("schoolCodeCount", String.valueOf(schulfotosProperties.defaultCodeCount()))
			.booleanField("schoolPicPeakEnabled", picPeakProperties.enabled())
			.build();
	}

	public void run() throws Exception {
		var config = TuiConfig.builder().build();
		try (var runner = ToolkitRunner.create(config)) {
			runner.run(this::renderWizard);
		}
	}

	private Element renderWizard() {
		PhotoGalleryWizardViewModel viewModel = buildViewModel();
		Element currentStepContent = PhotoGalleryWizardStepRenderer.renderCurrentStep(viewModel, classNameField(),
				eventCodeField(), shootingDateField(), codeCountField(), picPeakEnabledField());
		return column(PhotoGalleryWizardChrome.header(viewModel),
				PhotoGalleryWizardChrome.body(viewModel, currentStepContent),
				PhotoGalleryWizardChrome.footer(viewModel))
			.fill()
			.bg(BACKGROUND)
			.fg(readableText(TEXT_PRIMARY, BACKGROUND))
			.focusable()
			.id("wizard-root")
			.onKeyEvent(event -> handleKeyEvent(event.code()));
	}

	private EventResult handleKeyEvent(KeyCode keyCode) {
		if (state.anyStepInProgress()) {
			return EventResult.HANDLED;
		}
		return controller.handleKeyEvent(keyCode, controllerActions());
	}

	private void prepareFoldersStep() {
		scanCsvFiles();
		state.resetCsvSelection();
	}

	private void prepareWatermarkStep() {
		scanEventFolders();
		state.resetFolderSelection();
	}

	private void prepareUploadStep() {
		prepareWatermarkStep();
	}

	private PhotoGalleryWizardController.Actions controllerActions() {
		return new PhotoGalleryWizardController.Actions() {
			@Override
			public void prepareFoldersStep() {
				PhotoGalleryWizardTui.this.prepareFoldersStep();
			}

			@Override
			public void prepareWatermarkStep() {
				PhotoGalleryWizardTui.this.prepareWatermarkStep();
			}

			@Override
			public void prepareUploadStep() {
				PhotoGalleryWizardTui.this.prepareUploadStep();
			}

			@Override
			public void submitSchulfotos() {
				PhotoGalleryWizardTui.this.advanceSchulfotosStep();
			}

			@Override
			public void executeWorkflow() {
				PhotoGalleryWizardTui.this.executeWorkflow();
			}

			@Override
			public void executeFolderCreation() {
				PhotoGalleryWizardTui.this.executeFolderCreation();
			}

			@Override
			public void executeWatermark() {
				PhotoGalleryWizardTui.this.executeWatermark();
			}

			@Override
			public void executeUpload() {
				PhotoGalleryWizardTui.this.executeUpload();
			}

			@Override
			public boolean hasExecutionResult() {
				return state.executionResult() != null;
			}

			@Override
			public int csvFileCount() {
				return state.availableCsvFiles().size();
			}

			@Override
			public int eventFolderCount() {
				return state.availableEventFolders().size();
			}
		};
	}

	private void advanceSchulfotosStep() {
		if (validateSchulfotos()) {
			state.overwriteConfirmed(false);
			state.advanceToReview();
		}
	}

	private void executeWorkflow() {
		if (state.executionInProgress()) {
			return;
		}
		WizardRequest request = buildRequest();
		if (!confirmOverwriteIfNeeded(request)) {
			return;
		}

		state.clearValidationMessage();
		state.clearExecutionMessage();
		state.executionResult(null);
		state.executionInProgress(true);
		state.executionProgress(0.0d);
		state.executionStage("Startet");
		state.advanceToResults();

		Thread workflowThread = new Thread(() -> runWorkflow(request), "wizard-workflow");
		workflowThread.setDaemon(true);
		workflowThread.start();
	}

	private void runWorkflow(WizardRequest request) {
		try {
			state.executionResult(workflowService.execute(request, progress -> {
				state.executionProgress(progress.percent());
				state.executionStage(progress.stage());
			}));
			state.executionStage("Fertig");
			state.executionProgress(1.0d);
			state.overwriteConfirmed(false);
		}
		catch (Exception ex) {
			state.executionResult(null);
			state.executionMessage(PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
			state.overwriteConfirmed(false);
		}
		finally {
			state.executionInProgress(false);
		}
	}

	private boolean confirmOverwriteIfNeeded(WizardRequest request) {
		List<Path> existingFiles = new ArrayList<>();
		if (Files.exists(request.csvPath())) {
			existingFiles.add(request.csvPath());
		}
		if (Files.exists(request.pdfPath())) {
			existingFiles.add(request.pdfPath());
		}

		if (existingFiles.isEmpty()) {
			state.overwriteConfirmed(false);
			return true;
		}

		if (!state.overwriteConfirmed()) {
			state.overwriteConfirmed(true);
			state.validationMessage("Dateien existieren bereits: " + formatPaths(existingFiles)
					+ ". Enter erneut drücken, um zu überschreiben.");
			return false;
		}

		state.overwriteConfirmed(false);
		return true;
	}

	private String formatPaths(List<Path> paths) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < paths.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(paths.get(i));
		}
		return builder.toString();
	}

	private void executeFolderCreation() {
		state.clearValidationMessage();
		if (state.availableCsvFiles().isEmpty()) {
			scanEventFolders();
			state.advanceToWatermark();
			return;
		}
		state.clampCsvSelection();
		Path csvFile = state.availableCsvFiles().get(state.selectedCsvIndex());
		try {
			CsvReadResult readResult = csvReaderService.readCodes(csvFile);
			state.folderEventName(readResult.eventName());
			state.folderCodes(readResult.codes());
			state.createdFolders(folderStructureService.createFolderStructure(schulfotosRootDir(),
					state.folderEventName(), state.folderCodes()));
			scanEventFolders();
			state.advanceToWatermark();
		}
		catch (Exception ex) {
			state.validationMessage(
					"Ordnererstellung fehlgeschlagen: " + PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
		}
	}

	private void executeWatermark() {
		if (state.watermarkInProgress()) {
			return;
		}
		state.clearValidationMessage();
		if (state.availableEventFolders().isEmpty()) {
			state.advanceToUpload();
			return;
		}
		state.clampFolderSelection();
		Path eventDir = state.availableEventFolders().get(state.selectedFolderIndex());
		state.watermarkInProgress(true);
		state.watermarkProgress(0.0d);
		state.watermarkStage("Startet");

		Thread watermarkThread = new Thread(() -> runWatermark(eventDir), "wizard-watermark");
		watermarkThread.setDaemon(true);
		watermarkThread.start();
	}

	private void runWatermark(Path eventDir) {
		try {
			state.watermarkResult(imageProcessingService.processEventFolders(eventDir,
					Path.of(appProperties.watermarkPath()), appProperties.resizeMaxEdge(), progress -> {
						state.watermarkProgress(progress.percent());
						state.watermarkStage(progress.stage());
					}));
			state.watermarkProgress(1.0d);
			state.watermarkStage("Wasserzeichen fertig");
			state.advanceToUpload();
		}
		catch (Exception ex) {
			state.validationMessage(
					"Wasserzeichen/Skalierung fehlgeschlagen: " + PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
		}
		finally {
			state.watermarkInProgress(false);
		}
	}

	private void executeUpload() {
		if (state.uploadInProgress()) {
			return;
		}
		state.clearValidationMessage();
		state.clearUploadMessage();
		if (state.availableEventFolders().isEmpty() || state.folderCodes().isEmpty()) {
			state.advanceToDone();
			return;
		}
		state.clampFolderSelection();
		Path eventDir = state.availableEventFolders().get(state.selectedFolderIndex());
		state.uploadInProgress(true);
		state.uploadProgress(0.0d);
		state.uploadStage("Startet");

		Thread uploadThread = new Thread(() -> runUpload(eventDir), "wizard-upload");
		uploadThread.setDaemon(true);
		uploadThread.start();
	}

	private void runUpload(Path eventDir) {
		try {
			state.uploadResult(picPeakService.uploadEventPhotos(eventDir, state.folderCodes(), progress -> {
				state.uploadProgress(progress.percent());
				state.uploadStage(progress.stage());
			}));
			state.uploadProgress(1.0d);
			state.uploadStage("Upload fertig");
			state.advanceToDone();
		}
		catch (Exception ex) {
			state.uploadResult(null);
			state.uploadMessage(PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
			state.advanceToDone();
		}
		finally {
			state.uploadInProgress(false);
		}
	}

	private void scanCsvFiles() {
		try {
			state.availableCsvFiles(folderStructureService.listCsvFiles(schulfotosRootDir()));
		}
		catch (IOException ex) {
			state.validationMessage(
					"CSV-Dateien konnten nicht gelesen werden: " + PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
		}
	}

	private void scanEventFolders() {
		try {
			state.availableEventFolders(folderStructureService.listEventFolders(schulfotosRootDir()));
		}
		catch (IOException ex) {
			state.validationMessage("Event-Ordner konnten nicht gelesen werden: "
					+ PhotoGalleryWizardUi.sanitizeError(ex.getMessage()));
		}
	}

	private boolean validateSchulfotos() {
		if (form.textValue("schoolClassName").trim().isBlank()) {
			state.validationMessage("Klassenname ist erforderlich.");
			return false;
		}
		if (!normalizeEventCode(form.textValue("schoolEventCode")).matches(EVENT_CODE_PATTERN)) {
			state.validationMessage("Event-Code muss genau 4 alphanumerische Zeichen haben.");
			return false;
		}
		if (!isGermanDate(form.textValue("schoolShootingDate"))) {
			state.validationMessage("Shooting-Datum muss im Format TT.MM.JJJJ sein.");
			return false;
		}
		if (!isPositiveInteger(form.textValue("schoolCodeCount"))) {
			state.validationMessage("Anzahl Codes muss eine positive Ganzzahl sein.");
			return false;
		}
		state.clearValidationMessage();
		return true;
	}

	private WizardRequest buildRequest() {
		return new WizardRequest(normalizeEventCode(form.textValue("schoolEventCode")),
				form.textValue("schoolClassName").trim(), Integer.parseInt(form.textValue("schoolCodeCount").trim()),
				schulfotosCsvPath(), schulfotosPdfPath(), schulfotosProperties.baseUrl(),
				schulfotosProperties.galleryUrl(), schulfotosProperties.logoPath(), schulfotosProperties.qrSize(),
				schulfotosProperties.gridColumns(), schulfotosProperties.gridRows(),
				schulfotosProperties.showCuttingLines(), schulfotosProperties.galleryCodeLabel(),
				schulfotosProperties.galleryPasswordLabel(), form.booleanValue("schoolPicPeakEnabled"),
				toIsoDate(form.textValue("schoolShootingDate")));
	}

	private WizardRequest buildRequestPreview() {
		try {
			return buildRequest();
		}
		catch (RuntimeException ex) {
			return new WizardRequest(normalizeEventCode(form.textValue("schoolEventCode")),
					form.textValue("schoolClassName").trim(),
					safeParsePositive(form.textValue("schoolCodeCount"), schulfotosProperties.defaultCodeCount()),
					schulfotosCsvPath(), schulfotosPdfPath(), schulfotosProperties.baseUrl(),
					schulfotosProperties.galleryUrl(), schulfotosProperties.logoPath(), schulfotosProperties.qrSize(),
					schulfotosProperties.gridColumns(), schulfotosProperties.gridRows(),
					schulfotosProperties.showCuttingLines(), schulfotosProperties.galleryCodeLabel(),
					schulfotosProperties.galleryPasswordLabel(), form.booleanValue("schoolPicPeakEnabled"),
					toIsoDateSafe(form.textValue("schoolShootingDate")));
		}
	}

	private PhotoGalleryWizardViewModel buildViewModel() {
		return new PhotoGalleryWizardViewModel(state.activeStep(), state.validationMessage(), state.executionMessage(),
				state.executionInProgress(), state.executionProgress(), state.executionStage(),
				state.watermarkInProgress(), state.watermarkProgress(), state.watermarkStage(),
				state.uploadInProgress(), state.uploadProgress(), state.uploadStage(), state.executionResult(),
				state.availableCsvFiles(), state.selectedCsvIndex(), state.createdFolders(), state.folderEventName(),
				state.folderCodes(), state.availableEventFolders(), state.selectedFolderIndex(),
				state.watermarkResult(), state.uploadResult(), state.uploadMessage(), buildRequestPreview(),
				schulfotosProperties.baseUrl(), schulfotosProperties.galleryUrl(), schulfotosCsvPath(),
				schulfotosPdfPath(), schulfotosProperties.qrSize(), schulfotosProperties.gridColumns(),
				schulfotosProperties.gridRows(), totalSteps(), appProperties.watermarkPath(),
				appProperties.resizeMaxEdge());
	}

	private FormFieldElement classNameField() {
		return PhotoGalleryWizardUi
			.textField(formField(" Klassenname", form.textField("schoolClassName")).id("schoolClassName")
				.placeholder("GS1d BA")
				.onSubmit(this::advanceSchulfotosStep), CYAN_NEON);
	}

	private FormFieldElement eventCodeField() {
		return PhotoGalleryWizardUi
			.textField(formField(" Event-Code", form.textField("schoolEventCode")).id("schoolEventCode")
				.placeholder("AB12")
				.onSubmit(this::advanceSchulfotosStep), CYAN_NEON);
	}

	private FormFieldElement shootingDateField() {
		return PhotoGalleryWizardUi
			.textField(formField(" Shooting-Datum", form.textField("schoolShootingDate")).id("schoolShootingDate")
				.placeholder("25.03.2026")
				.onSubmit(this::advanceSchulfotosStep), CYAN_NEON);
	}

	private FormFieldElement codeCountField() {
		return PhotoGalleryWizardUi
			.textField(formField(" Anzahl Codes", form.textField("schoolCodeCount")).id("schoolCodeCount")
				.placeholder("17")
				.onSubmit(this::advanceSchulfotosStep), CYAN_NEON);
	}

	private FormFieldElement picPeakEnabledField() {
		return PhotoGalleryWizardUi
			.booleanField(
					formField(" PicPeak-Events", form.booleanField("schoolPicPeakEnabled")).id("schoolPicPeakEnabled"),
					CYAN_NEON)
			.checkedSymbol("■ erzeugen")
			.uncheckedSymbol("□ erzeugen");
	}

	private int totalSteps() {
		return PhotoGalleryWizardStep.values().length;
	}

	private Path schulfotosCsvPath() {
		return schulfotosRootDir().resolve(sanitizedClassName() + "-codes.csv");
	}

	private Path schulfotosPdfPath() {
		return schulfotosRootDir().resolve(sanitizedClassName() + "-qr-codes.pdf");
	}

	private Path schulfotosRootDir() {
		return Path.of(schulfotosProperties.outputDir());
	}

	private String sanitizedClassName() {
		return blankFallback(form.textValue("schoolClassName").trim(), "klasse").replace(" ", "-").replace("/", "-");
	}

	private String normalizeEventCode(String value) {
		return blankFallback(value, "").trim().toUpperCase();
	}

	private boolean isPositiveInteger(String value) {
		try {
			return Integer.parseInt(value.trim()) > 0;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	private int safeParsePositive(String value, int fallback) {
		return isPositiveInteger(value) ? Integer.parseInt(value.trim()) : fallback;
	}

	private boolean isGermanDate(String value) {
		try {
			LocalDate.parse(value.trim(), GERMAN_DATE);
			return true;
		}
		catch (DateTimeParseException ex) {
			return false;
		}
	}

	private String toIsoDate(String germanDate) {
		return LocalDate.parse(germanDate.trim(), GERMAN_DATE).format(ISO_DATE);
	}

	private String toIsoDateSafe(String germanDate) {
		return isGermanDate(germanDate) ? toIsoDate(germanDate) : "";
	}

	private String defaultSchoolClass(String eventName) {
		return eventName == null || eventName.isBlank() ? "" : eventName;
	}

	private String defaultEventCode(String configuredEventCode) {
		if (configuredEventCode != null && !configuredEventCode.isBlank()) {
			return configuredEventCode;
		}
		StringBuilder builder = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			builder.append(schulfotosProperties.codeCharset()
				.charAt(ThreadLocalRandom.current().nextInt(schulfotosProperties.codeCharset().length())));
		}
		return builder.toString();
	}

	private String blankFallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

}
