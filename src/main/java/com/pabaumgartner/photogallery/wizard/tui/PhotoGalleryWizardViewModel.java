package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;

record PhotoGalleryWizardViewModel(PhotoGalleryWizardStep activeStep, String validationMessage, String executionMessage,
		boolean executionInProgress, double executionProgress, String executionStage, boolean watermarkInProgress,
		double watermarkProgress, String watermarkStage, boolean uploadInProgress, double uploadProgress,
		String uploadStage, WizardExecutionResult executionResult, List<Path> availableCsvFiles, int selectedCsvIndex,
		List<Path> createdFolders, String folderEventName, List<GalleryCode> folderCodes,
		List<Path> availableEventFolders, int selectedFolderIndex,
		ImageProcessingService.ImageProcessingResult watermarkResult, PicPeakService.UploadResult uploadResult,
		String uploadMessage, WizardRequest requestPreview, String baseUrl, String galleryUrl, Path csvPath,
		Path pdfPath, int qrSize, int gridColumns, int gridRows, int totalSteps, String watermarkPath,
		int resizeMaxEdge) {

	String currentStepTitle() {
		return activeStep.title();
	}

	String workflowStatusText() {
		if (executionInProgress) {
			return executionStage == null || executionStage.isBlank() ? "Generierung läuft" : executionStage;
		}
		if (watermarkInProgress) {
			return watermarkStage == null || watermarkStage.isBlank() ? "Wasserzeichen läuft" : watermarkStage;
		}
		if (uploadInProgress) {
			return uploadStage == null || uploadStage.isBlank() ? "Upload läuft" : uploadStage;
		}
		if (activeStep == PhotoGalleryWizardStep.DONE) {
			return "Workflow abgeschlossen";
		}
		return "Bereit für: " + activeStep.title();
	}

	double workflowProgress() {
		double total = Math.max(totalSteps, 1);
		double base = Math.max(activeStep.position() - 1, 0);
		double stepProgress = switch (activeStep) {
			case RESULTS -> executionInProgress ? executionProgress : (executionResult != null ? 1.0d : 0.0d);
			case WATERMARK -> watermarkInProgress ? watermarkProgress : (watermarkResult != null ? 1.0d : 0.0d);
			case UPLOAD -> uploadInProgress ? uploadProgress : (uploadResult != null ? 1.0d : 0.0d);
			case DONE -> 1.0d;
			default -> 0.0d;
		};
		double overall = (base + Math.max(0.0d, Math.min(1.0d, stepProgress))) / total;
		return Math.max(0.0d, Math.min(1.0d, overall));
	}

	long galleriesWithId() {
		return folderCodes.stream().filter(code -> code.picPeakEventId() > 0).count();
	}

	String footerHint() {
		return switch (activeStep) {
			case SCHULFOTOS -> "Felder ausfüllen und mit Enter zur Überprüfung wechseln.";
			case REVIEW -> "Zusammenfassung prüfen und mit Enter die Generierung starten.";
			case RESULTS -> executionInProgress ? "Generierung läuft."
					: executionResult != null ? "Generierung abgeschlossen. Mit Enter zur Ordner-Erstellung."
							: "Generierung fehlgeschlagen. Mit F2 zurück und Eingaben korrigieren.";
			case FOLDERS -> "CSV mit Hoch/Runter wählen und mit Enter Ordnerstruktur erstellen.";
			case WATERMARK -> watermarkInProgress ? "Wasserzeichen-Verarbeitung läuft."
					: "Event-Ordner mit Hoch/Runter wählen und mit Enter verarbeiten.";
			case UPLOAD ->
				uploadInProgress ? "Upload läuft." : "Mit Enter den Upload der Wasserzeichen-Bilder starten.";
			case DONE -> "Workflow abgeschlossen. Mit F2 zurück oder mit CTRL+C beenden.";
		};
	}
}