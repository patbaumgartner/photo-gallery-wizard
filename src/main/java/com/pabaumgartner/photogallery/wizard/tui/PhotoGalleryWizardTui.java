package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.PicPeakProperties;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import com.pabaumgartner.photogallery.wizard.service.WizardWorkflowService;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import org.springframework.stereotype.Component;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

@Component
public class PhotoGalleryWizardTui {

    private static final String PROFILE_SCHULFOTOS = "Schulfotos";

    private static final String PROFILE_ADVANCED = "Advanced Mode";

    private static final String SCHULFOTOS_BASE_URL = "https://mel-rohrer.ch/schulfotos";

    private static final String SCHULFOTOS_GALLERY_URL = "https://mel-rohrer.ch/schulfotos/?code=";

    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String EVENT_CODE_PATTERN = "^[A-Z0-9]{4}$";

    private static final String CODE_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int LABEL_WIDTH = 19;

    private static final Color BACKGROUND = Color.hex("#050816");

    private static final Color SURFACE = Color.hex("#0B1020");

    private static final Color SURFACE_ALT = Color.hex("#111933");

    private static final Color CYAN_NEON = Color.hex("#00F5FF");

    private static final Color PINK_NEON = Color.hex("#FF4FD8");

    private static final Color LIME_NEON = Color.hex("#A7FF4F");

    private static final Color AMBER_GLOW = Color.hex("#FFC857");

    private static final Color TEXT_PRIMARY = Color.hex("#E6F1FF");

    private static final Color TEXT_MUTED = Color.hex("#7E8FB3");

    private static final Color BORDER_MUTED = Color.hex("#27324F");

    private static final Color ERROR_GLOW = Color.hex("#FF6B8B");

    private final AtomicInteger step = new AtomicInteger(1);

    private final FormState form;

    private final WizardWorkflowService workflowService;

    private WizardExecutionResult executionResult;

    private String validationMessage = "";

    private String executionMessage = "";

    public PhotoGalleryWizardTui(AppProperties appProperties, PicPeakProperties picPeakProperties,
            WizardWorkflowService workflowService) {
        this.workflowService = workflowService;
        this.form = FormState.builder()
            .selectField("workflowProfile", List.of(PROFILE_SCHULFOTOS, PROFILE_ADVANCED), 0)
            .textField("schoolClassName", defaultSchoolClass(appProperties.eventName()))
            .textField("schoolEventCode", defaultEventCode(appProperties.eventCode()))
            .textField("schoolShootingDate", LocalDate.now().format(GERMAN_DATE))
            .textField("schoolCodeCount", "17")
            .booleanField("schoolPicPeakEnabled", picPeakProperties.enabled())
            .textField("advancedEventCode", defaultEventCode(appProperties.eventCode()))
            .textField("advancedCodeCount", Integer.toString(appProperties.codeCount()))
            .textField("advancedEventName", appProperties.eventName())
            .textField("advancedCsvPath", defaultCsvPath(appProperties.csvOutputPath()))
            .textField("advancedPdfPath", defaultPdfPath(appProperties.outputPath()))
            .textField("advancedBaseUrl", appProperties.baseUrl())
            .textField("advancedGalleryUrl", appProperties.galleryUrl())
            .textField("advancedQrSize", Integer.toString(appProperties.qrSize()))
            .textField("advancedGridColumns", Integer.toString(appProperties.gridColumns()))
            .textField("advancedGridRows", Integer.toString(appProperties.gridRows()))
            .booleanField("advancedCuttingLines", appProperties.showCuttingLines())
            .textField("advancedLogoUrl", appProperties.logoUrl())
            .textField("advancedGalleryCodeLabel", appProperties.galleryCodeLabel())
            .textField("advancedGalleryPasswordLabel", appProperties.galleryPasswordLabel())
            .booleanField("advancedPicPeakEnabled", picPeakProperties.enabled())
            .textField("advancedPicPeakEventDate", picPeakProperties.eventDate())
            .build();
    }

    public void run() throws Exception {
        var config = TuiConfig.builder().noTick().build();
        try (var runner = ToolkitRunner.create(config)) {
            runner.run(this::renderWizard);
        }
    }

    private Element renderWizard() {
        Color accent = accentForProfile();
        return column(header(accent), body(accent), footer(accent)).fill()
            .bg(BACKGROUND)
            .fg(TEXT_PRIMARY)
            .focusable()
            .id("wizard-root")
            .onKeyEvent(event -> handleKeyEvent(event.code()));
    }

    private EventResult handleKeyEvent(KeyCode keyCode) {
        if (keyCode == KeyCode.ESCAPE) {
            return EventResult.UNHANDLED;
        }
        if (keyCode == KeyCode.F2 && step.get() > 1) {
            validationMessage = "";
            executionMessage = "";
            executionResult = null;
            step.set(step.get() - 1);
            return EventResult.HANDLED;
        }
        if (keyCode == KeyCode.ENTER && isReviewStep()) {
            executeWorkflow();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private Element header(Color accent) {
        double progress = (double) step.get() / totalSteps();
        return panel(() -> column(
                row(text(" PHOTO GALLERY QR CONTROL ").style(Style.create().fg(BACKGROUND).bg(accent).bold()), spacer(),
                    shortcutBadge("TAB", "focus"), text(" "), shortcutBadge("ENTER", "next/run"), text(" "),
                    shortcutBadge("F2", "back"), text(" "), shortcutBadge("CTRL+C", "quit")).length(1),
                text("Step 1 begins with the workflow profile choice: Schulfotos or Advanced Mode.")
                    .fg(TEXT_MUTED)
                    .length(1),
                text(" ").length(1),
                gauge(progress).label(currentStepTitle().toUpperCase() + "  " + step.get() + " / " + totalSteps())
                    .rounded()
                    .gaugeColor(accent)
                    .gaugeStyle(Style.create().fg(TEXT_PRIMARY).bg(SURFACE_ALT))
                    .borderColor(accent)
                    .length(3)))
            .rounded()
            .padding(1)
            .bg(SURFACE)
            .borderColor(PINK_NEON)
            .length(7);
    }

    private Element body(Color accent) {
        return panel(() -> column(stepRail(), validationPanel(), currentStepContent())).rounded()
            .padding(1)
            .bg(SURFACE)
            .borderColor(accent)
            .fill();
    }

    private Element stepRail() {
        List<String> titles = stepTitles();
        Element[] elements = new Element[titles.size() * 2 - 1];
        int writeIndex = 0;
        for (int i = 0; i < titles.size(); i++) {
            elements[writeIndex++] = stepToken(i + 1, titles.get(i));
            if (i < titles.size() - 1) {
                elements[writeIndex++] = text(" == ").fg(i + 1 < step.get() ? accentForProfile() : BORDER_MUTED);
            }
        }
        return panel("Signal Path", row(elements)).rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(BORDER_MUTED)
            .length(6);
    }

    private Element stepToken(int tokenStep, String title) {
        boolean complete = tokenStep < step.get();
        boolean active = tokenStep == step.get();
        Color accent = complete ? LIME_NEON : (active ? accentForProfile() : BORDER_MUTED);
        return panel(() -> column(
                text(" 0" + tokenStep + " ").style(Style.create().fg(BACKGROUND).bg(accent).bold()).centered().length(1),
                text(title).fg(TEXT_PRIMARY).bold().centered().length(1),
                text(complete ? "locked" : (active ? "live" : "standby"))
                    .fg(complete || active ? accent : TEXT_MUTED)
                    .centered()
                    .length(1)))
            .rounded()
            .padding(1)
            .bg(active ? Color.hex("#0F1830") : BACKGROUND)
            .borderColor(accent)
            .fill();
    }

    private Element validationPanel() {
        if (validationMessage.isBlank()) {
            return text(" ").length(1);
        }
        return panel("Input Check", text(validationMessage).fg(ERROR_GLOW)).rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(ERROR_GLOW)
            .length(4);
    }

    private Element currentStepContent() {
        if (isSchulfotos()) {
            return switch (step.get()) {
                case 1 -> renderProfileStep();
                case 2 -> renderSchulfotosStep();
                case 3 -> renderReviewStep();
                default -> renderResultStep();
            };
        }
        return switch (step.get()) {
            case 1 -> renderProfileStep();
            case 2 -> renderAdvancedCoreStep();
            case 3 -> renderAdvancedLayoutStep();
            case 4 -> renderAdvancedPicPeakStep();
            case 5 -> renderReviewStep();
            default -> renderResultStep();
        };
    }

    private Element footer(Color accent) {
        String hint;
        if (isResultStep()) {
            hint = executionResult != null ? "Generation finished. Press F2 to adjust settings and run again."
                    : "Generation failed. Press F2 to correct settings and retry.";
        }
        else if (isReviewStep()) {
            hint = "Review the snapshot, then press Enter to generate CSV and QR-code PDF.";
        }
        else if (step.get() == 1) {
            hint = "Choose the workflow profile. Schulfotos is the mel-rohrer preset; Advanced Mode exposes the generator settings.";
        }
        else {
            hint = "Edit the active fields, then press Enter on the last field to advance.";
        }
        return panel(() -> row(text(hint).fg(TEXT_MUTED), spacer(), shortcutBadge("PROFILE", selectedProfile()))).rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(accent)
            .length(4);
    }

    private Element renderProfileStep() {
        return panel("Step 1 // Workflow Profile",
                column(text("Select the operating mode first. Schulfotos follows schulfotos-mel-rohrer.sh. Advanced Mode follows generate-qrcodes.sh with the wider parameter surface.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    selectField("Workflow Profile", "workflowProfile", CYAN_NEON).onSubmit(this::advanceProfileStep),
                    text(" ").length(1),
                    profileCard(PROFILE_SCHULFOTOS, "mel-rohrer preset", List.of(
                        "Ask only for class, event code, shooting date, count, PicPeak toggle",
                        "Base URL and gallery URL fixed to mel-rohrer.ch/schulfotos",
                        "3x4 layout, 200px QR, cutting lines on, logo.png enabled"),
                        isSchulfotos() ? CYAN_NEON : BORDER_MUTED),
                    text(" ").length(1),
                    profileCard(PROFILE_ADVANCED, "full workflow", List.of(
                        "Event code, count, event name, paths, URLs, grid, QR size",
                        "Card labels, logo, cutting lines, PicPeak toggle and event date",
                        "Defaults aligned with the combined generate-codes plus PDF flow"),
                        isSchulfotos() ? BORDER_MUTED : PINK_NEON)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(accentForProfile())
            .fill();
    }

    private Element renderSchulfotosStep() {
        return panel("Step 2 // Schulfotos Setup",
                column(text("This path mirrors schulfotos-mel-rohrer.sh and keeps only the few fields that matter before relying on the known-good defaults.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    textField("Klassenname", "schoolClassName", "GS1d BA", CYAN_NEON),
                    text(" ").length(1),
                    textField("Event-Code", "schoolEventCode", "AB12", PINK_NEON),
                    text(" ").length(1),
                    textField("Shooting-Datum", "schoolShootingDate", "25.03.2026", AMBER_GLOW),
                    text(" ").length(1),
                    textField("Anzahl Codes", "schoolCodeCount", "17", CYAN_NEON),
                    text(" ").length(1),
                    booleanField("PicPeak-Events", "schoolPicPeakEnabled", LIME_NEON)
                        .onSubmit(this::advanceSchulfotosStep),
                    text(" ").length(1),
                    previewCard("Derived Defaults", List.of(
                        previewLine("Base URL", SCHULFOTOS_BASE_URL, CYAN_NEON),
                        previewLine("Gallery URL", SCHULFOTOS_GALLERY_URL, PINK_NEON),
                        previewLine("CSV", schulfotosCsvPath().toString(), TEXT_PRIMARY),
                        previewLine("PDF", schulfotosPdfPath().toString(), AMBER_GLOW),
                        previewLine("Layout", "3 x 4, cutting lines on, 200 px QR", LIME_NEON),
                        previewLine("Logo", "logo.png", TEXT_MUTED)), CYAN_NEON)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(CYAN_NEON)
            .fill();
    }

    private Element renderAdvancedCoreStep() {
        return panel("Step 2 // Advanced Core",
                column(text("This follows the generate-qrcodes.sh workflow: configure the code batch first, then tune rendering and integration details.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    textField("Event-Code", "advancedEventCode", "XY9G", CYAN_NEON),
                    text(" ").length(1),
                    textField("Code Count", "advancedCodeCount", "17", PINK_NEON),
                    text(" ").length(1),
                    textField("Event Name", "advancedEventName", "My Photo Event", AMBER_GLOW),
                    text(" ").length(1),
                    textField("CSV Output Path", "advancedCsvPath", "generated/codes.csv", CYAN_NEON)
                        .onSubmit(this::advanceAdvancedCoreStep)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(PINK_NEON)
            .fill();
    }

    private Element renderAdvancedLayoutStep() {
        return panel("Step 3 // Layout And Output",
                column(text("Tune the PDF generation settings and output targets. These map directly to the generator properties rather than to a simplified preset.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    textField("PDF Output Path", "advancedPdfPath", "generated/qr-codes.pdf", CYAN_NEON),
                    text(" ").length(1),
                    textField("Base URL", "advancedBaseUrl", "https://my.site", PINK_NEON),
                    text(" ").length(1),
                    textField("Gallery URL", "advancedGalleryUrl", "https://my.site/gallery?code=", AMBER_GLOW),
                    text(" ").length(1),
                    textField("QR Size", "advancedQrSize", "200", CYAN_NEON),
                    text(" ").length(1),
                    textField("Grid Columns", "advancedGridColumns", "3", PINK_NEON),
                    text(" ").length(1),
                    textField("Grid Rows", "advancedGridRows", "4", AMBER_GLOW),
                    text(" ").length(1),
                    booleanField("Show Cutting Lines", "advancedCuttingLines", LIME_NEON),
                    text(" ").length(1),
                    textField("Logo URL", "advancedLogoUrl", "logo.png", CYAN_NEON)
                        .onSubmit(this::advanceAdvancedLayoutStep)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(AMBER_GLOW)
            .fill();
    }

    private Element renderAdvancedPicPeakStep() {
        return panel("Step 4 // Labels And PicPeak",
                column(text("Adjust the printed labels and choose whether PicPeak should enrich the generated codes with share links.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    textField("Gallery Code Label", "advancedGalleryCodeLabel", "GALERIE CODE", CYAN_NEON),
                    text(" ").length(1),
                    textField("Password Label", "advancedGalleryPasswordLabel", "GALERIE PASSWORT", PINK_NEON),
                    text(" ").length(1),
                    booleanField("PicPeak Enabled", "advancedPicPeakEnabled", LIME_NEON),
                    text(" ").length(1),
                    textField("PicPeak Event Date", "advancedPicPeakEventDate", "YYYY-MM-DD", AMBER_GLOW)
                        .onSubmit(this::advanceAdvancedPicPeakStep),
                    text(" ").length(1),
                    previewCard("PicPeak Note", List.of(
                        previewLine("Enabled", booleanLabel(form.booleanValue("advancedPicPeakEnabled")), LIME_NEON),
                        previewLine("Event Date", blankFallback(form.textValue("advancedPicPeakEventDate"), "today"), TEXT_PRIMARY),
                        previewLine("Credentials", "taken from app.picpeak.* properties or environment", TEXT_MUTED)),
                        PINK_NEON)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(LIME_NEON)
            .fill();
    }

    private Element renderReviewStep() {
        WizardRequest request = buildRequestPreview();
        return panel(currentStepTitle(),
                column(text("The workflow is staged. Press Enter here to execute both steps and write the CSV plus the duplex-ready QR-code PDF.")
                    .fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    previewCard("Output Snapshot", List.of(
                        previewLine("Profile", selectedProfile(), accentForProfile()),
                        previewLine("Event-Code", request.eventCode(), CYAN_NEON),
                        previewLine("Event Name", blankFallback(request.eventName(), "(empty)"), TEXT_PRIMARY),
                        previewLine("Code Count", Integer.toString(request.codeCount()), PINK_NEON),
                        previewLine("CSV", request.csvPath().toString(), TEXT_PRIMARY),
                        previewLine("PDF", request.pdfPath().toString(), AMBER_GLOW)), accentForProfile()),
                    text(" ").length(1),
                    previewCard("Render Settings", List.of(
                        previewLine("Base URL", request.baseUrl(), CYAN_NEON),
                        previewLine("Gallery URL", request.galleryUrl(), PINK_NEON),
                        previewLine("Grid", request.gridColumns() + " x " + request.gridRows(), AMBER_GLOW),
                        previewLine("QR Size", request.qrSize() + " px", TEXT_PRIMARY),
                        previewLine("Cutting Lines", booleanLabel(request.showCuttingLines()), LIME_NEON),
                        previewLine("PicPeak", booleanLabel(request.picPeakEnabled()),
                                request.picPeakEnabled() ? LIME_NEON : TEXT_MUTED)), PINK_NEON),
                    text(" ").length(1),
                    panel("Run", text("Press Enter now to generate the artifacts.").fg(TEXT_MUTED)).rounded().padding(1)
                        .bg(SURFACE)
                        .borderColor(LIME_NEON)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(accentForProfile())
            .fill();
    }

    private Element renderResultStep() {
        if (executionResult != null) {
            return panel("Results",
                    column(text("Generation completed. The QR workflow finished successfully.").fg(TEXT_MUTED).length(1),
                        text(" ").length(1),
                        previewCard("Artifacts", List.of(
                            previewLine("Event-Code", executionResult.eventCode(), CYAN_NEON),
                            previewLine("Codes", Integer.toString(executionResult.codeCount()), PINK_NEON),
                            previewLine("Pages", Integer.toString(executionResult.pageCount()), LIME_NEON),
                            previewLine("CSV", executionResult.csvPath().toAbsolutePath().toString(), TEXT_PRIMARY),
                            previewLine("PDF", executionResult.pdfPath().toAbsolutePath().toString(), AMBER_GLOW)),
                            LIME_NEON)))
                .rounded()
                .padding(1)
                .bg(SURFACE_ALT)
                .borderColor(LIME_NEON)
                .fill();
        }
        return panel("Execution Error",
                column(text("The workflow did not complete. Adjust the inputs or environment and run it again.").fg(TEXT_MUTED)
                    .length(1),
                    text(" ").length(1),
                    panel("Failure Detail", text(blankFallback(executionMessage, "Unknown error")).fg(ERROR_GLOW)).rounded()
                        .padding(1)
                        .bg(SURFACE)
                        .borderColor(ERROR_GLOW)))
            .rounded()
            .padding(1)
            .bg(SURFACE_ALT)
            .borderColor(ERROR_GLOW)
            .fill();
    }

    private void advanceProfileStep() {
        validationMessage = "";
        executionMessage = "";
        executionResult = null;
        step.set(2);
    }

    private void advanceSchulfotosStep() {
        if (validateSchulfotos()) {
            step.set(3);
        }
    }

    private void advanceAdvancedCoreStep() {
        if (validateAdvancedCore()) {
            step.set(3);
        }
    }

    private void advanceAdvancedLayoutStep() {
        if (validateAdvancedLayout()) {
            step.set(4);
        }
    }

    private void advanceAdvancedPicPeakStep() {
        if (validateAdvancedPicPeak()) {
            step.set(5);
        }
    }

    private void executeWorkflow() {
        try {
            validationMessage = "";
            executionMessage = "";
            executionResult = workflowService.execute(buildRequest());
            step.set(totalSteps());
        }
        catch (Exception ex) {
            executionResult = null;
            executionMessage = ex.getMessage() == null || ex.getMessage().isBlank() ? ex.toString() : ex.getMessage();
            step.set(totalSteps());
        }
    }

    private boolean validateSchulfotos() {
        if (form.textValue("schoolClassName").trim().isBlank()) {
            validationMessage = "Klassenname is required.";
            return false;
        }
        if (!normalizeEventCode(form.textValue("schoolEventCode")).matches(EVENT_CODE_PATTERN)) {
            validationMessage = "Event-Code must be exactly 4 alphanumeric characters.";
            return false;
        }
        if (!isGermanDate(form.textValue("schoolShootingDate"))) {
            validationMessage = "Shooting-Datum must use DD.MM.YYYY.";
            return false;
        }
        if (!isPositiveInteger(form.textValue("schoolCodeCount"))) {
            validationMessage = "Anzahl Codes must be a positive integer.";
            return false;
        }
        validationMessage = "";
        return true;
    }

    private boolean validateAdvancedCore() {
        if (!normalizeEventCode(form.textValue("advancedEventCode")).matches(EVENT_CODE_PATTERN)) {
            validationMessage = "Advanced Event-Code must be exactly 4 alphanumeric characters.";
            return false;
        }
        if (!isPositiveInteger(form.textValue("advancedCodeCount"))) {
            validationMessage = "Code Count must be a positive integer.";
            return false;
        }
        if (form.textValue("advancedCsvPath").trim().isBlank()) {
            validationMessage = "CSV Output Path must not be blank.";
            return false;
        }
        validationMessage = "";
        return true;
    }

    private boolean validateAdvancedLayout() {
        if (form.textValue("advancedPdfPath").trim().isBlank()) {
            validationMessage = "PDF Output Path must not be blank.";
            return false;
        }
        if (form.textValue("advancedBaseUrl").trim().isBlank()) {
            validationMessage = "Base URL must not be blank.";
            return false;
        }
        String galleryUrl = form.textValue("advancedGalleryUrl").trim();
        if (!galleryUrl.startsWith("https://")) {
            validationMessage = "Gallery URL must start with https://.";
            return false;
        }
        if (!galleryUrl.endsWith("=") && !galleryUrl.endsWith("/")) {
            validationMessage = "Gallery URL must end with '=' or '/' so the code can be appended.";
            return false;
        }
        if (!isPositiveInteger(form.textValue("advancedQrSize")) || !isPositiveInteger(form.textValue("advancedGridColumns"))
                || !isPositiveInteger(form.textValue("advancedGridRows"))) {
            validationMessage = "QR Size, Grid Columns, and Grid Rows must be positive integers.";
            return false;
        }
        validationMessage = "";
        return true;
    }

    private boolean validateAdvancedPicPeak() {
        String eventDate = form.textValue("advancedPicPeakEventDate").trim();
        if (!eventDate.isBlank() && !isIsoDate(eventDate)) {
            validationMessage = "PicPeak Event Date must use YYYY-MM-DD or be left blank.";
            return false;
        }
        validationMessage = "";
        return true;
    }

    private WizardRequest buildRequest() {
        if (isSchulfotos()) {
            return new WizardRequest(normalizeEventCode(form.textValue("schoolEventCode")),
                    Integer.parseInt(form.textValue("schoolCodeCount").trim()), form.textValue("schoolClassName").trim(),
                    schulfotosCsvPath(), schulfotosPdfPath(), SCHULFOTOS_BASE_URL, SCHULFOTOS_GALLERY_URL, "logo.png", 200,
                    3, 4, true, "GALERIE CODE", "GALERIE PASSWORT", form.booleanValue("schoolPicPeakEnabled"),
                    toIsoDate(form.textValue("schoolShootingDate")));
        }
        return new WizardRequest(normalizeEventCode(form.textValue("advancedEventCode")),
                Integer.parseInt(form.textValue("advancedCodeCount").trim()), form.textValue("advancedEventName").trim(),
                Path.of(form.textValue("advancedCsvPath").trim()), Path.of(form.textValue("advancedPdfPath").trim()),
                form.textValue("advancedBaseUrl").trim(), form.textValue("advancedGalleryUrl").trim(),
                form.textValue("advancedLogoUrl").trim(), Integer.parseInt(form.textValue("advancedQrSize").trim()),
                Integer.parseInt(form.textValue("advancedGridColumns").trim()),
                Integer.parseInt(form.textValue("advancedGridRows").trim()), form.booleanValue("advancedCuttingLines"),
                form.textValue("advancedGalleryCodeLabel").trim(), form.textValue("advancedGalleryPasswordLabel").trim(),
                form.booleanValue("advancedPicPeakEnabled"), form.textValue("advancedPicPeakEventDate").trim());
    }

    private WizardRequest buildRequestPreview() {
        try {
            return buildRequest();
        }
        catch (RuntimeException ex) {
            if (isSchulfotos()) {
                return new WizardRequest(normalizeEventCode(form.textValue("schoolEventCode")),
                        safeParsePositive(form.textValue("schoolCodeCount"), 17), form.textValue("schoolClassName").trim(),
                        schulfotosCsvPath(), schulfotosPdfPath(), SCHULFOTOS_BASE_URL, SCHULFOTOS_GALLERY_URL, "logo.png",
                        200, 3, 4, true, "GALERIE CODE", "GALERIE PASSWORT", form.booleanValue("schoolPicPeakEnabled"),
                        toIsoDateSafe(form.textValue("schoolShootingDate")));
            }
            return new WizardRequest(normalizeEventCode(form.textValue("advancedEventCode")),
                    safeParsePositive(form.textValue("advancedCodeCount"), 17), form.textValue("advancedEventName").trim(),
                    Path.of(blankFallback(form.textValue("advancedCsvPath"), "generated/codes.csv")),
                    Path.of(blankFallback(form.textValue("advancedPdfPath"), "generated/qr-codes.pdf")),
                    blankFallback(form.textValue("advancedBaseUrl"), "https://my.site"),
                    blankFallback(form.textValue("advancedGalleryUrl"), "https://my.site/gallery?code="),
                    form.textValue("advancedLogoUrl").trim(), safeParsePositive(form.textValue("advancedQrSize"), 200),
                    safeParsePositive(form.textValue("advancedGridColumns"), 3),
                    safeParsePositive(form.textValue("advancedGridRows"), 4), form.booleanValue("advancedCuttingLines"),
                    form.textValue("advancedGalleryCodeLabel").trim(), form.textValue("advancedGalleryPasswordLabel").trim(),
                    form.booleanValue("advancedPicPeakEnabled"), form.textValue("advancedPicPeakEventDate").trim());
        }
    }

    private FormFieldElement textField(String label, String fieldId, String placeholder, Color accent) {
        return formField(label, form.textField(fieldId)).id(fieldId)
            .labelWidth(LABEL_WIDTH)
            .labelStyle(Style.create().fg(accent).bold())
            .errorStyle(Style.create().fg(ERROR_GLOW).bold())
            .rounded()
            .placeholder(placeholder)
            .borderColor(BORDER_MUTED)
            .focusedBorderColor(accent)
            .errorBorderColor(ERROR_GLOW)
            .arrowNavigation(true);
    }

    private FormFieldElement booleanField(String label, String fieldId, Color accent) {
        return formField(label, form.booleanField(fieldId)).id(fieldId)
            .labelWidth(LABEL_WIDTH)
            .labelStyle(Style.create().fg(accent).bold())
            .rounded()
            .borderColor(BORDER_MUTED)
            .focusedBorderColor(accent)
            .checkedColor(accent)
            .uncheckedColor(TEXT_MUTED)
            .checkedSymbol("■")
            .uncheckedSymbol("□");
    }

    private FormFieldElement selectField(String label, String fieldId, Color accent) {
        return formField(label, form.selectField(fieldId)).id(fieldId)
            .labelWidth(LABEL_WIDTH)
            .labelStyle(Style.create().fg(accent).bold())
            .rounded()
            .borderColor(BORDER_MUTED)
            .focusedBorderColor(accent);
    }

    private Element profileCard(String title, String subtitle, List<String> bulletLines, Color accent) {
        Element[] items = new Element[bulletLines.size() + 2];
        items[0] = text(title).fg(TEXT_PRIMARY).bold().length(1);
        items[1] = text(subtitle).fg(accent).length(1);
        for (int i = 0; i < bulletLines.size(); i++) {
            items[i + 2] = text(" • " + bulletLines.get(i)).fg(TEXT_MUTED).length(1);
        }
        return panel(() -> column(items)).rounded()
            .padding(1)
            .bg(SURFACE)
            .borderColor(accent);
    }

    private Element previewCard(String title, List<Element> lines, Color accent) {
        return panel(title, column(lines.toArray(new Element[0]))).rounded()
            .padding(1)
            .bg(SURFACE)
            .borderColor(accent);
    }

    private Element previewLine(String label, String value, Color valueColor) {
        return row(text(label + ": ").fg(TEXT_MUTED).bold(), text(value).fg(valueColor)).length(1);
    }

    private Element shortcutBadge(String key, String action) {
        return text(" " + key + " " + action + " ").style(Style.create().fg(BACKGROUND).bg(SURFACE_ALT).bold());
    }

    private int totalSteps() {
        return isSchulfotos() ? 4 : 6;
    }

    private boolean isReviewStep() {
        return step.get() == totalSteps() - 1;
    }

    private boolean isResultStep() {
        return step.get() == totalSteps();
    }

    private boolean isSchulfotos() {
        return PROFILE_SCHULFOTOS.equals(selectedProfile());
    }

    private String selectedProfile() {
        return form.selectValue("workflowProfile");
    }

    private Color accentForProfile() {
        return isSchulfotos() ? CYAN_NEON : PINK_NEON;
    }

    private List<String> stepTitles() {
        return isSchulfotos() ? List.of("Profile", "Schulfotos", "Review", "Results")
                : List.of("Profile", "Core", "Layout", "PicPeak", "Review", "Results");
    }

    private String currentStepTitle() {
        return stepTitles().get(step.get() - 1);
    }

    private Path schulfotosCsvPath() {
        return Path.of("schulfotos", sanitizedClassName() + "-codes.csv");
    }

    private Path schulfotosPdfPath() {
        return Path.of("schulfotos", sanitizedClassName() + "-qr-codes.pdf");
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

    private boolean isIsoDate(String value) {
        try {
            LocalDate.parse(value.trim(), ISO_DATE);
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

    private String booleanLabel(boolean value) {
        return value ? "enabled" : "disabled";
    }

    private String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultSchoolClass(String eventName) {
        return eventName == null || eventName.isBlank() ? "" : eventName;
    }

    private String defaultCsvPath(String csvPath) {
        return "codes.csv".equals(csvPath) ? "generated/codes.csv" : csvPath;
    }

    private String defaultPdfPath(String pdfPath) {
        return "qr-codes.pdf".equals(pdfPath) ? "generated/qr-codes.pdf" : pdfPath;
    }

    private String defaultEventCode(String configuredEventCode) {
        if (configuredEventCode != null && !configuredEventCode.isBlank()) {
            return configuredEventCode;
        }
        StringBuilder builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(CODE_CHARSET.charAt(ThreadLocalRandom.current().nextInt(CODE_CHARSET.length())));
        }
        return builder.toString();
    }

}
