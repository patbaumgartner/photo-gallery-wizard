package com.pabaumgartner.photogallery.wizard.tui;

import java.util.concurrent.atomic.AtomicInteger;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

public class PhotoGalleryWizardTui {

private static final int TOTAL_STEPS = 3;

private static final int LABEL_WIDTH = 15;

private final AtomicInteger step = new AtomicInteger(1);

private final FormState form;

public PhotoGalleryWizardTui(AppProperties appProperties) {
this.form = FormState.builder()
.textField("galleryName", appProperties.galleryName())
.textField("galleryDescription", appProperties.galleryDescription())
.textField("outputPath", appProperties.outputPath())
.build();
}

public void run() throws Exception {
var config = TuiConfig.builder().noTick().build();

try (var runner = ToolkitRunner.create(config)) {
runner.run(this::renderWizard);
}
}

private Element renderWizard() {
int currentStep = step.get();
return column(header(currentStep), stepContent(currentStep), footer(currentStep)).fill()
.focusable()
.id("wizard-root")
.onKeyEvent(event -> {
if (event.code() == KeyCode.ESCAPE || event.isQuit()) {
return EventResult.UNHANDLED;
}
if (event.code() == KeyCode.F2 && step.get() > 1 && step.get() < TOTAL_STEPS) {
step.decrementAndGet();
return EventResult.HANDLED;
}
return EventResult.UNHANDLED;
});
}

private Element header(int currentStep) {
double progress = (double) currentStep / TOTAL_STEPS;
String label = currentStep < TOTAL_STEPS ? "Step " + currentStep + " of " + TOTAL_STEPS : "Complete!";
return panel(() -> column(row(text(" \uD83D\uDCF7 Photo Gallery Wizard ").bold().cyan(), spacer(),
text(" [Tab] Next field ").dim(), text(" [F2] Back ").dim(), text(" [Ctrl+C] Quit ").dim()).length(1),
gauge(progress).label(label).gaugeColor(Color.CYAN).length(1))).rounded()
.borderColor(Color.DARK_GRAY)
.length(5);
}

private Element stepContent(int currentStep) {
return switch (currentStep) {
case 1 -> renderStep1();
case 2 -> renderStep2();
default -> renderSummary();
};
}

private Element footer(int currentStep) {
if (currentStep == TOTAL_STEPS) {
return panel(() -> row(text(" Press Ctrl+C to exit ").dim())).rounded()
.borderColor(Color.DARK_GRAY)
.length(3);
}
String hint = currentStep == 1 ? " [Enter] on last field to continue "
: " [F2] Back   [Enter] on field to continue ";
return panel(() -> row(text(hint).dim())).rounded().borderColor(Color.DARK_GRAY).length(3);
}

private Element renderStep1() {
return panel("Step 1: Gallery Information",
column(
formField("Gallery Name *", form.textField("galleryName")).id("galleryName")
.labelWidth(LABEL_WIDTH)
.rounded()
.placeholder("My Photo Gallery")
.borderColor(Color.DARK_GRAY)
.focusedBorderColor(Color.CYAN)
.arrowNavigation(true),
formField("Description", form.textField("galleryDescription")).id("galleryDescription")
.labelWidth(LABEL_WIDTH)
.rounded()
.placeholder("A brief description...")
.borderColor(Color.DARK_GRAY)
.focusedBorderColor(Color.CYAN)
.arrowNavigation(true)
.onSubmit(this::advanceToStep2)))
.rounded()
.borderColor(Color.CYAN)
.fill();
}

private Element renderStep2() {
return panel("Step 2: Output Configuration",
column(text(" Directory where gallery files will be generated. ").dim().length(1),
formField("Output Directory", form.textField("outputPath")).id("outputPath")
.labelWidth(LABEL_WIDTH)
.rounded()
.placeholder("gallery-output")
.borderColor(Color.DARK_GRAY)
.focusedBorderColor(Color.CYAN)
.arrowNavigation(true)
.onSubmit(this::advanceToStep3)))
.rounded()
.borderColor(Color.CYAN)
.fill();
}

private Element renderSummary() {
String galleryName = form.textValue("galleryName");
String description = form.textValue("galleryDescription");
String outputPath = form.textValue("outputPath");
if (outputPath == null || outputPath.isBlank()) {
outputPath = "gallery-output";
}
String displayDescription = (description == null || description.isBlank()) ? "\u2014" : description;
String finalOutputPath = outputPath;
return panel("\u2705 Summary",
column(text(" Your gallery configuration has been prepared. ").dim().length(1),
panel(() -> column(row(text("  Gallery Name: ").bold(), text(galleryName)).length(1),
row(text("  Description:  ").bold(), text(displayDescription)).length(1),
row(text("  Output Path:  ").bold(), text(finalOutputPath).yellow()).length(1)))
.rounded()
.borderColor(Color.GREEN)))
.rounded()
.borderColor(Color.GREEN)
.fill();
}

private void advanceToStep2() {
String galleryName = form.textValue("galleryName");
if (galleryName != null && !galleryName.isBlank()) {
step.set(2);
}
}

private void advanceToStep3() {
String outputPath = form.textValue("outputPath");
if (outputPath == null || outputPath.isBlank()) {
form.setTextValue("outputPath", "gallery-output");
}
step.set(3);
}

}
