package com.pabaumgartner.photogallery.wizard.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.PdfOptions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorServiceTest {

	@TempDir
	Path tempDir;

	private PdfGeneratorService pdfService;

	private QrCodeGeneratorService qrService;

	private static AppProperties defaultAppProperties() {
		return new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
	}

	@BeforeEach
	void setUp() {
		pdfService = new PdfGeneratorService(defaultAppProperties());
		qrService = new QrCodeGeneratorService();
	}

	private LinkedHashMap<GalleryCode, BufferedImage> generateQrImages(List<GalleryCode> codes) {
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
		for (int i = 0; i < codes.size(); i++) {
			GalleryCode code = codes.get(i);
			qrImages.put(code, qrService.generateQrCode(code, "https://gallery.com/?code=", 200, i + 1));
		}
		return qrImages;
	}

	@Test
	void createPdfGeneratesValidPdf() throws IOException {
		Path output = tempDir.resolve("test.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw1"),
				new GalleryCode("EFGH-5678-STUV", "pw2"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, true, "Test Event", "https://base.com", "", "CODE", "PW");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(output).exists();
		assertThat(pageCount).isEqualTo(1);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void createPdfWithExactlyOnePageOfCodes() throws IOException {
		Path output = tempDir.resolve("exact.pdf");
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			codes.add(new GalleryCode(String.format("AB%02d-%04d-WXYZ", i, i), "pw" + i));
		}
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Full Page", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void createPdfWithMoreThanOnePageOfCodes() throws IOException {
		Path output = tempDir.resolve("multi.pdf");
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < 17; i++) {
			codes.add(new GalleryCode(String.format("AB%02d-%04d-WXYZ", i, i), "pw" + i));
		}
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, true, "MultiPage", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(2);
		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(4);
		}
	}

	@Test
	void createPdfWithSingleCode() throws IOException {
		Path output = tempDir.resolve("single.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "password123"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
		assertThat(output).exists();
	}

	@Test
	void createPdfWithoutEventName() throws IOException {
		Path output = tempDir.resolve("noname.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
		assertThat(output).exists();
	}

	@Test
	void createPdfWithNoCuttingLines() throws IOException {
		Path output = tempDir.resolve("nocutting.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Event", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
	}

	@Test
	void createPdfWithDifferentGridSizes() throws IOException {
		Path output = tempDir.resolve("grid.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"),
				new GalleryCode("EFGH-5678-STUV", "pw2"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 2, 2, true, "Grid", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void createPdfCreatesParentDirectories() throws IOException {
		Path output = tempDir.resolve("sub/dir/output.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "E", "https://base.com", "", "C", "P");

		pdfService.createPdf(codes, qrImages, options);

		assertThat(output).exists();
	}

	@Test
	void createPdfWithEmptyPassword() throws IOException {
		Path output = tempDir.resolve("emptypw.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", ""));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Event", "https://base.com", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
	}

	@Test
	void createPdfWithBlankBaseUrl() throws IOException {
		Path output = tempDir.resolve("nobase.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Event", "", "", "C", "P");

		int pageCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(pageCount).isEqualTo(1);
	}

	@Test
	void createPdfFileSizeIsNonTrivial() throws IOException {
		Path output = tempDir.resolve("filesize.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, true, "E", "https://base.com", "", "C", "P");

		pdfService.createPdf(codes, qrImages, options);

		long fileSize = Files.size(output);
		assertThat(fileSize).isGreaterThan(1000);
	}

}
