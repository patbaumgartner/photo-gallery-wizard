package com.pabaumgartner.photogallery.wizard.tui;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;

final class PhotoGalleryWizardController {

	interface Actions {

		void prepareFoldersStep();

		void prepareWatermarkStep();

		void prepareUploadStep();

		void submitSchulfotos();

		void executeWorkflow();

		void executeFolderCreation();

		void executeWatermark();

		void executeUpload();

		boolean hasExecutionResult();

		int csvFileCount();

		int eventFolderCount();

	}

	private final PhotoGalleryWizardState state;

	PhotoGalleryWizardController(PhotoGalleryWizardState state) {
		this.state = state;
	}

	EventResult handleKeyEvent(KeyCode keyCode, Actions actions) {
		if (keyCode == KeyCode.ESCAPE) {
			return EventResult.UNHANDLED;
		}
		if (keyCode == KeyCode.F2 && state.canGoBack()) {
			state.goBack();
			return EventResult.HANDLED;
		}
		if (keyCode == KeyCode.ENTER) {
			return handleEnter(actions);
		}
		if (keyCode == KeyCode.UP || keyCode == KeyCode.DOWN) {
			return handleArrowSelection(keyCode, actions);
		}
		if (keyCode == KeyCode.F4) {
			actions.prepareFoldersStep();
			state.jumpToFolders();
			return EventResult.HANDLED;
		}
		if (keyCode == KeyCode.F5) {
			actions.prepareWatermarkStep();
			state.jumpToWatermark();
			return EventResult.HANDLED;
		}
		if (keyCode == KeyCode.F6) {
			actions.prepareUploadStep();
			state.jumpToUpload();
			return EventResult.HANDLED;
		}
		return EventResult.UNHANDLED;
	}

	private EventResult handleEnter(Actions actions) {
		return switch (state.activeStep()) {
			case SCHULFOTOS -> {
				actions.submitSchulfotos();
				yield EventResult.HANDLED;
			}
			case REVIEW -> {
				actions.executeWorkflow();
				yield EventResult.HANDLED;
			}
			case RESULTS -> {
				if (actions.hasExecutionResult()) {
					actions.prepareFoldersStep();
					state.advanceToFolders();
				}
				yield EventResult.HANDLED;
			}
			case FOLDERS -> {
				actions.executeFolderCreation();
				yield EventResult.HANDLED;
			}
			case WATERMARK -> {
				actions.executeWatermark();
				yield EventResult.HANDLED;
			}
			case UPLOAD -> {
				actions.executeUpload();
				yield EventResult.HANDLED;
			}
			default -> EventResult.UNHANDLED;
		};
	}

	private EventResult handleArrowSelection(KeyCode keyCode, Actions actions) {
		if (state.activeStep() == PhotoGalleryWizardStep.FOLDERS && actions.csvFileCount() > 0) {
			if (keyCode == KeyCode.UP) {
				state.selectPreviousCsv();
			}
			else {
				state.selectNextCsv(actions.csvFileCount());
			}
			return EventResult.HANDLED;
		}
		if (state.activeStep() == PhotoGalleryWizardStep.WATERMARK && actions.eventFolderCount() > 0) {
			if (keyCode == KeyCode.UP) {
				state.selectPreviousFolder();
			}
			else {
				state.selectNextFolder(actions.eventFolderCount());
			}
			return EventResult.HANDLED;
		}
		return EventResult.UNHANDLED;
	}

}