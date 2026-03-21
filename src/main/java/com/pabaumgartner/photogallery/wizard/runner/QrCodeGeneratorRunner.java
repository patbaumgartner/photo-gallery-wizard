package com.pabaumgartner.photogallery.wizard.runner;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.model.CsvReadResult;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.PdfOptions;
import com.pabaumgartner.photogallery.wizard.service.CsvReaderService;
import com.pabaumgartner.photogallery.wizard.service.PdfGeneratorService;
import com.pabaumgartner.photogallery.wizard.service.QrCodeGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class QrCodeGeneratorRunner implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(QrCodeGeneratorRunner.class);

	private final CsvReaderService csvReaderService;

	private final QrCodeGeneratorService qrCodeGeneratorService;

	private final PdfGeneratorService pdfGeneratorService;

	private final AppProperties appProperties;

	public QrCodeGeneratorRunner(CsvReaderService csvReaderService, QrCodeGeneratorService qrCodeGeneratorService,
			PdfGeneratorService pdfGeneratorService, AppProperties appProperties) {
		this.csvReaderService = csvReaderService;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.pdfGeneratorService = pdfGeneratorService;
		this.appProperties = appProperties;
	}

	@Override
	public void run(String... args) throws IOException {
		if (!"generate-pdf".equals(appProperties.mode())) {
			return;
		}

		Path inputPath = Path.of(appProperties.csvInputPath());
		Path outputPath = Path.of(appProperties.outputPath());

		LOGGER.atInfo().addArgument(() -> inputPath.toAbsolutePath()).log("Reading gallery codes from: {}");

		CsvReadResult csvResult = csvReaderService.readCodes(inputPath);
		List<GalleryCode> codes = csvResult.codes();
		String eventName = csvResult.eventName();

		if (codes.isEmpty()) {
			LOGGER.warn("No valid gallery codes found in {}. No PDF generated.", inputPath);
			return;
		}

		LOGGER.atInfo().addArgument(() -> codes.size()).log("Generating QR codes for {} gallery codes...");

		LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
		for (int i = 0; i < codes.size(); i++) {
			GalleryCode code = codes.get(i);
			BufferedImage qrImage = qrCodeGeneratorService.generateQrCode(code, appProperties.galleryUrl(),
					appProperties.qrSize(), i + 1);
			qrImages.put(code, qrImage);
		}

		PdfOptions pdfOptions = new PdfOptions(outputPath, appProperties.gridColumns(), appProperties.gridRows(),
				appProperties.showCuttingLines(), eventName, appProperties.baseUrl(), appProperties.logoUrl(),
				appProperties.galleryCodeLabel(), appProperties.galleryPasswordLabel());

		int pages = pdfGeneratorService.createPdf(codes, qrImages, pdfOptions);

		LOGGER.atInfo()
				.addArgument(() -> codes.size())
				.addArgument(pages)
				.addArgument(() -> outputPath.toAbsolutePath())
				.log("Done! Generated PDF with {} QR codes on {} page(s): {}");
	}

}
