package com.pabaumgartner.photogallery.wizard.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.PdfOptions;
import com.pabaumgartner.photogallery.wizard.model.WizardExecutionResult;
import com.pabaumgartner.photogallery.wizard.model.WizardRequest;
import org.springframework.stereotype.Service;

@Service
public class WizardWorkflowService {

	private final CodeGeneratorService codeGeneratorService;

	private final CsvWriterService csvWriterService;

	private final QrCodeGeneratorService qrCodeGeneratorService;

	private final PdfGeneratorService pdfGeneratorService;

	private final PicPeakService picPeakService;

	private final CsvUploadService csvUploadService;

	public WizardWorkflowService(CodeGeneratorService codeGeneratorService, CsvWriterService csvWriterService,
			QrCodeGeneratorService qrCodeGeneratorService, PdfGeneratorService pdfGeneratorService,
			PicPeakService picPeakService, CsvUploadService csvUploadService) {
		this.codeGeneratorService = codeGeneratorService;
		this.csvWriterService = csvWriterService;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.pdfGeneratorService = pdfGeneratorService;
		this.picPeakService = picPeakService;
		this.csvUploadService = csvUploadService;
	}

	public WizardExecutionResult execute(WizardRequest request) throws IOException {
		return execute(request, progress -> {
		});
	}

	public WizardExecutionResult execute(WizardRequest request, Consumer<WorkflowProgress> progressListener)
			throws IOException {
		progressListener.accept(new WorkflowProgress(0.05d, "Galerie-Codes werden vorbereitet"));
		List<GalleryCode> codes = codeGeneratorService.generateCodes(request.eventCode(), request.codeCount());

		progressListener.accept(new WorkflowProgress(0.20d, "PicPeak-Verknüpfungen werden erstellt"));
		codes = picPeakService.enrichWithShareLinks(codes, request.eventName(), request.picPeakEnabled(),
				request.picPeakEventDate());

		progressListener.accept(new WorkflowProgress(0.30d, "CSV schreiben"));
		csvWriterService.writeCodes(codes, request.csvPath(), request.eventName(), request.galleryUrl());

		progressListener.accept(new WorkflowProgress(0.38d, "CSV hochladen"));
		csvUploadService.upload(request.csvPath());

		progressListener.accept(new WorkflowProgress(0.45d, "QR-Codes erzeugen"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
		double qrStart = 0.45d;
		double qrEnd = 0.75d;
		int totalCodes = Math.max(codes.size(), 1);
		for (int i = 0; i < codes.size(); i++) {
			GalleryCode code = codes.get(i);
			BufferedImage qrImage = qrCodeGeneratorService.generateQrCode(code, request.galleryUrl(), request.qrSize(),
					i + 1);
			qrImages.put(code, qrImage);
			double ratio = (double) (i + 1) / totalCodes;
			progressListener.accept(new WorkflowProgress(qrStart + (qrEnd - qrStart) * ratio, "QR-Codes erzeugen"));
		}

		progressListener.accept(new WorkflowProgress(0.80d, "PDF erstellen"));
		PdfOptions pdfOptions = new PdfOptions(request.pdfPath(), request.gridColumns(), request.gridRows(),
				request.showCuttingLines(), request.eventName(), request.baseUrl(), request.logoUrl(),
				request.galleryCodeLabel(), request.galleryPasswordLabel());
		int pageCount = pdfGeneratorService.createPdf(codes, qrImages, pdfOptions);
		progressListener.accept(new WorkflowProgress(1.00d, "Fertig"));
		return new WizardExecutionResult(request.eventCode(), request.eventName(), codes.size(), pageCount,
				request.csvPath(), request.pdfPath());
	}

	public record WorkflowProgress(double percent, String stage) {
	}

}
