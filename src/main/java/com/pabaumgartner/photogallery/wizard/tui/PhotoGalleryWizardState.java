package com.pabaumgartner.photogallery.wizard.tui;

import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.service.ImageProcessingService;
import com.pabaumgartner.photogallery.wizard.service.PicPeakService;

final class PhotoGalleryWizardState {

	private PhotoGalleryWizardStep activeStep = PhotoGalleryWizardStep.SCHULFOTOS;

	private String validationMessage = "";

	private String executionMessage = "";

	private boolean executionInProgress;

	private double executionProgress;

	private String executionStage = "";

	private boolean overwriteConfirmed;

	private boolean watermarkInProgress;

	private double watermarkProgress;

	private String watermarkStage = "";

	private boolean uploadInProgress;

	private double uploadProgress;

	private String uploadStage = "";

	private WizardExecutionResult executionResult;

	private List<Path> availableCsvFiles = List.of();

	private List<Path> createdFolders = List.of();

	private String folderEventName = "";

	private List<GalleryCode> folderCodes = List.of();

	private List<Path> availableEventFolders = List.of();

	private ImageProcessingService.ImageProcessingResult watermarkResult;

	private PicPeakService.UploadResult uploadResult;

	private String uploadMessage = "";

	private int selectedCsvIndex;

	private int selectedFolderIndex;

	PhotoGalleryWizardStep activeStep() {
		return activeStep;
	}

	void activeStep(PhotoGalleryWizardStep activeStep) {
		this.activeStep = activeStep;
	}

	boolean canGoBack() {
		return activeStep != PhotoGalleryWizardStep.SCHULFOTOS && !anyStepInProgress();
	}

	boolean anyStepInProgress() {
		return executionInProgress || watermarkInProgress || uploadInProgress;
	}

	void goBack() {
		clearValidationMessage();
		activeStep = PhotoGalleryWizardStep.values()[activeStep.ordinal() - 1];
	}

	void resetAll() {
		activeStep = PhotoGalleryWizardStep.SCHULFOTOS;
		validationMessage = "";
		executionMessage = "";
		executionInProgress = false;
		executionProgress = 0.0d;
		executionStage = "";
		overwriteConfirmed = false;
		watermarkInProgress = false;
		watermarkProgress = 0.0d;
		watermarkStage = "";
		uploadInProgress = false;
		uploadProgress = 0.0d;
		uploadStage = "";
		executionResult = null;
		availableCsvFiles = List.of();
		createdFolders = List.of();
		folderEventName = "";
		folderCodes = List.of();
		availableEventFolders = List.of();
		watermarkResult = null;
		uploadResult = null;
		uploadMessage = "";
		selectedCsvIndex = 0;
		selectedFolderIndex = 0;
	}

	void clearValidationMessage() {
		validationMessage = "";
	}

	void clearExecutionMessage() {
		executionMessage = "";
	}

	void clearUploadMessage() {
		uploadMessage = "";
	}

	void jumpToFolders() {
		clearValidationMessage();
		activeStep = PhotoGalleryWizardStep.FOLDERS;
	}

	void jumpToWatermark() {
		clearValidationMessage();
		activeStep = PhotoGalleryWizardStep.WATERMARK;
	}

	void jumpToUpload() {
		clearValidationMessage();
		activeStep = PhotoGalleryWizardStep.UPLOAD;
	}

	void advanceToReview() {
		activeStep = PhotoGalleryWizardStep.REVIEW;
	}

	void advanceToResults() {
		activeStep = PhotoGalleryWizardStep.RESULTS;
	}

	void advanceToFolders() {
		activeStep = PhotoGalleryWizardStep.FOLDERS;
	}

	void advanceToWatermark() {
		activeStep = PhotoGalleryWizardStep.WATERMARK;
	}

	void advanceToUpload() {
		activeStep = PhotoGalleryWizardStep.UPLOAD;
	}

	void advanceToDone() {
		activeStep = PhotoGalleryWizardStep.DONE;
	}

	String validationMessage() {
		return validationMessage;
	}

	void validationMessage(String validationMessage) {
		this.validationMessage = validationMessage;
	}

	String executionMessage() {
		return executionMessage;
	}

	void executionMessage(String executionMessage) {
		this.executionMessage = executionMessage;
	}

	boolean executionInProgress() {
		return executionInProgress;
	}

	void executionInProgress(boolean executionInProgress) {
		this.executionInProgress = executionInProgress;
	}

	double executionProgress() {
		return executionProgress;
	}

	void executionProgress(double executionProgress) {
		this.executionProgress = executionProgress;
	}

	String executionStage() {
		return executionStage;
	}

	void executionStage(String executionStage) {
		this.executionStage = executionStage;
	}

	boolean overwriteConfirmed() {
		return overwriteConfirmed;
	}

	void overwriteConfirmed(boolean overwriteConfirmed) {
		this.overwriteConfirmed = overwriteConfirmed;
	}

	boolean watermarkInProgress() {
		return watermarkInProgress;
	}

	void watermarkInProgress(boolean watermarkInProgress) {
		this.watermarkInProgress = watermarkInProgress;
	}

	double watermarkProgress() {
		return watermarkProgress;
	}

	void watermarkProgress(double watermarkProgress) {
		this.watermarkProgress = watermarkProgress;
	}

	String watermarkStage() {
		return watermarkStage;
	}

	void watermarkStage(String watermarkStage) {
		this.watermarkStage = watermarkStage;
	}

	boolean uploadInProgress() {
		return uploadInProgress;
	}

	void uploadInProgress(boolean uploadInProgress) {
		this.uploadInProgress = uploadInProgress;
	}

	double uploadProgress() {
		return uploadProgress;
	}

	void uploadProgress(double uploadProgress) {
		this.uploadProgress = uploadProgress;
	}

	String uploadStage() {
		return uploadStage;
	}

	void uploadStage(String uploadStage) {
		this.uploadStage = uploadStage;
	}

	WizardExecutionResult executionResult() {
		return executionResult;
	}

	void executionResult(WizardExecutionResult executionResult) {
		this.executionResult = executionResult;
	}

	List<Path> availableCsvFiles() {
		return availableCsvFiles;
	}

	void availableCsvFiles(List<Path> availableCsvFiles) {
		this.availableCsvFiles = availableCsvFiles;
	}

	List<Path> createdFolders() {
		return createdFolders;
	}

	void createdFolders(List<Path> createdFolders) {
		this.createdFolders = createdFolders;
	}

	String folderEventName() {
		return folderEventName;
	}

	void folderEventName(String folderEventName) {
		this.folderEventName = folderEventName;
	}

	List<GalleryCode> folderCodes() {
		return folderCodes;
	}

	void folderCodes(List<GalleryCode> folderCodes) {
		this.folderCodes = folderCodes;
	}

	List<Path> availableEventFolders() {
		return availableEventFolders;
	}

	void availableEventFolders(List<Path> availableEventFolders) {
		this.availableEventFolders = availableEventFolders;
	}

	ImageProcessingService.ImageProcessingResult watermarkResult() {
		return watermarkResult;
	}

	void watermarkResult(ImageProcessingService.ImageProcessingResult watermarkResult) {
		this.watermarkResult = watermarkResult;
	}

	PicPeakService.UploadResult uploadResult() {
		return uploadResult;
	}

	void uploadResult(PicPeakService.UploadResult uploadResult) {
		this.uploadResult = uploadResult;
	}

	String uploadMessage() {
		return uploadMessage;
	}

	void uploadMessage(String uploadMessage) {
		this.uploadMessage = uploadMessage;
	}

	int selectedCsvIndex() {
		return selectedCsvIndex;
	}

	void selectedCsvIndex(int selectedCsvIndex) {
		this.selectedCsvIndex = selectedCsvIndex;
	}

	void resetCsvSelection() {
		selectedCsvIndex = 0;
	}

	void clampCsvSelection() {
		if (selectedCsvIndex >= availableCsvFiles.size()) {
			selectedCsvIndex = 0;
		}
	}

	void selectPreviousCsv() {
		if (selectedCsvIndex > 0) {
			selectedCsvIndex--;
		}
	}

	void selectNextCsv(int total) {
		if (selectedCsvIndex < total - 1) {
			selectedCsvIndex++;
		}
	}

	int selectedFolderIndex() {
		return selectedFolderIndex;
	}

	void selectedFolderIndex(int selectedFolderIndex) {
		this.selectedFolderIndex = selectedFolderIndex;
	}

	void resetFolderSelection() {
		selectedFolderIndex = 0;
	}

	void clampFolderSelection() {
		if (selectedFolderIndex >= availableEventFolders.size()) {
			selectedFolderIndex = 0;
		}
	}

	void selectPreviousFolder() {
		if (selectedFolderIndex > 0) {
			selectedFolderIndex--;
		}
	}

	void selectNextFolder(int total) {
		if (selectedFolderIndex < total - 1) {
			selectedFolderIndex++;
		}
	}

}