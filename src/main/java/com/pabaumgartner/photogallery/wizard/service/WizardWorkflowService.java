package com.pabaumgartner.photogallery.wizard.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

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

	public WizardWorkflowService(CodeGeneratorService codeGeneratorService, CsvWriterService csvWriterService,
			QrCodeGeneratorService qrCodeGeneratorService, PdfGeneratorService pdfGeneratorService,
			PicPeakService picPeakService) {
		this.codeGeneratorService = codeGeneratorService;
		this.csvWriterService = csvWriterService;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.pdfGeneratorService = pdfGeneratorService;
		this.picPeakService = picPeakService;
	}

	public WizardExecutionResult execute(WizardRequest request) throws IOException {
		List<GalleryCode> codes = codeGeneratorService.generateCodes(request.eventCode(), request.codeCount());
		codes = picPeakService.enrichWithShareLinks(codes, request.eventName(), request.picPeakEnabled(),
				request.picPeakEventDate());

		csvWriterService.writeCodes(codes, request.csvPath(), request.eventName(), request.galleryUrl());

		LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
		for (int i = 0; i < codes.size(); i++) {
			GalleryCode code = codes.get(i);
			BufferedImage qrImage = qrCodeGeneratorService.generateQrCode(code, request.galleryUrl(), request.qrSize(),
					i + 1);
			qrImages.put(code, qrImage);
		}

		PdfOptions pdfOptions = new PdfOptions(request.pdfPath(), request.gridColumns(), request.gridRows(),
				request.showCuttingLines(), request.eventName(), request.baseUrl(), request.logoUrl(),
				request.galleryCodeLabel(), request.galleryPasswordLabel());
		int pageCount = pdfGeneratorService.createPdf(codes, qrImages, pdfOptions);
		return new WizardExecutionResult(request.csvPath(), request.pdfPath(), codes.size(), pageCount,
				request.eventCode(), request.eventName());
	}

}
