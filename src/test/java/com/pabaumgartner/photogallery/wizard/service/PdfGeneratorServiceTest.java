package com.pabaumgartner.photogallery.wizard.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.pabaumgartner.photogallery.wizard.config.ImageProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import com.pabaumgartner.photogallery.wizard.model.PdfOptions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfGeneratorServiceTest {

	@TempDir
	Path tempDir;

	private PdfGeneratorService pdfService;

	private QrCodeGeneratorService qrService;

	private static ImageProperties defaultImageProperties() {
		return new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
	}

	@BeforeEach
	void setUp() {
		pdfService = new PdfGeneratorService(defaultImageProperties());
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
	void frontPageShowsCodeLabelAndEventNameAndBackPageShowsPasswordAndUrl() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "Pw9!kLm2x"));
		Path output = tempDir.resolve("content.pdf");
		PdfOptions options = new PdfOptions(output, 3, 4, true, "Klasse 3a", "https://gallery.example.com", "",
				"GALERIE CODE", "GALERIE PASSWORT");

		pdfService.createPdf(codes, generateQrImages(codes), options);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(1);
			String front = stripper.getText(doc);
			stripper.setStartPage(2);
			stripper.setEndPage(2);
			String back = stripper.getText(doc);

			assertThat(front).contains("ABCD-1234-WXYZ").contains("GALERIE CODE").contains("Klasse 3a");
			assertThat(back).contains("GALERIE PASSWORT").contains("Pw9!kLm2x").contains("gallery.example.com");
			assertThat(back).doesNotContain("ABCD-1234-WXYZ");
		}
	}

	@Test
	void everyPageIsWrittenAsASingleContentStream() throws IOException {
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			codes.add(new GalleryCode("ABCD-1234-WXY" + i, "pw" + i));
		}
		Path output = tempDir.resolve("streams.pdf");
		PdfOptions options = new PdfOptions(output, 3, 4, true, "Event", "https://gallery.example.com", "", "CODE",
				"PW");

		pdfService.createPdf(codes, generateQrImages(codes), options);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
			for (PDPage page : doc.getPages()) {
				assertThat(page.getCOSObject().getDictionaryObject(COSName.CONTENTS)).isInstanceOf(COSStream.class);
			}
		}
	}

	@Test
	void cuttingLinesAreOmittedWhenDisabled() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		Path withLines = tempDir.resolve("with-lines.pdf");
		Path withoutLines = tempDir.resolve("without-lines.pdf");

		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(withLines, 3, 4, true, "Event", "https://example.com", "", "CODE", "PW"));
		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(withoutLines, 3, 4, false, "Event", "https://example.com", "", "CODE", "PW"));

		assertThat(Files.size(withoutLines)).isLessThan(Files.size(withLines));
	}

	@Test
	void createPdfGeneratesValidPdf() throws IOException {
		Path output = tempDir.resolve("test.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw1"),
				new GalleryCode("EFGH-5678-STUV", "pw2"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, true, "Test Event", "https://base.com", "", "CODE", "PW");

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(output).exists();
		assertThat(sheetCount).isEqualTo(1);

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

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
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

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(2);
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

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
		assertThat(output).exists();
	}

	@Test
	void createPdfWithoutEventName() throws IOException {
		Path output = tempDir.resolve("noname.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "", "https://base.com", "", "C", "P");

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
		assertThat(output).exists();
	}

	@Test
	void createPdfWithNoCuttingLines() throws IOException {
		Path output = tempDir.resolve("nocutting.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Event", "https://base.com", "", "C", "P");

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
	}

	@Test
	void createPdfWithDifferentGridSizes() throws IOException {
		Path output = tempDir.resolve("grid.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"),
				new GalleryCode("EFGH-5678-STUV", "pw2"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 2, 2, true, "Grid", "https://base.com", "", "C", "P");

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
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

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
	}

	@Test
	void createPdfWithBlankBaseUrl() throws IOException {
		Path output = tempDir.resolve("nobase.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		PdfOptions options = new PdfOptions(output, 3, 4, false, "Event", "", "", "C", "P");

		int sheetCount = pdfService.createPdf(codes, qrImages, options);

		assertThat(sheetCount).isEqualTo(1);
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

	@Test
	void charactersOutsideTheFontEncodingDoNotAbortThePdf() throws IOException {
		Path output = tempDir.resolve("unicode.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "Pw9!kLm2x"));
		PdfOptions options = new PdfOptions(output, 3, 4, true, "Klasse 3ä Ćwiczenia \uD83D\uDE00",
				"https://gallery.example.com", "", "GALERIE CODE", "GALERIE PASSWORT");

		pdfService.createPdf(codes, generateQrImages(codes), options);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(1);
			assertThat(stripper.getText(doc)).contains("Klasse 3ä ?wiczenia ?");
		}
	}

	@Test
	void toEncodableKeepsSupportedCharactersAndReplacesTheRest() {
		PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

		assertThat(PdfGeneratorService.toEncodable(font, "Klasse 3ä – ok")).isEqualTo("Klasse 3ä – ok");
		assertThat(PdfGeneratorService.toEncodable(font, "Ćwiczenia")).isEqualTo("?wiczenia");
		assertThat(PdfGeneratorService.toEncodable(font, "a\uD83D\uDE00b")).isEqualTo("a?b");
	}

	@Test
	void aMissingQrImageIsReportedWithTheCodeItBelongsTo() {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));
		PdfOptions options = new PdfOptions(tempDir.resolve("missing.pdf"), 3, 4, false, "Event", "https://base.com",
				"", "C", "P");

		assertThatThrownBy(() -> pdfService.createPdf(codes, new LinkedHashMap<>(), options))
			.isInstanceOf(IOException.class)
			.hasMessageContaining("ABCD-1234-WXYZ");
	}

	@Test
	void aLocalLogoIsDrawnOnTheCardBack() throws IOException {
		Path logo = writePng(tempDir.resolve("logo.png"));
		Path withLogo = tempDir.resolve("with-logo.pdf");
		Path withoutLogo = tempDir.resolve("without-logo.pdf");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));

		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(withLogo, 3, 4, false, "Event", "https://base.com", logo.toString(), "C", "P"));
		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(withoutLogo, 3, 4, false, "Event", "https://base.com", "", "C", "P"));

		try (PDDocument doc = Loader.loadPDF(withLogo.toFile())) {
			assertThat(imageCount(doc.getPage(1))).isEqualTo(1);
		}
		try (PDDocument doc = Loader.loadPDF(withoutLogo.toFile())) {
			assertThat(imageCount(doc.getPage(1))).isZero();
		}
	}

	@Test
	void aLogoThatCannotBeFoundOrDecodedStillProducesAPdf() throws IOException {
		Path broken = tempDir.resolve("broken.png");
		Files.writeString(broken, "this is not an image");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));

		for (String logoUrl : List.of(tempDir.resolve("does-not-exist.png").toString(), broken.toString())) {
			Path output = tempDir.resolve("logo-" + logoUrl.hashCode() + ".pdf");

			pdfService.createPdf(codes, generateQrImages(codes),
					new PdfOptions(output, 3, 4, false, "Event", "https://base.com", logoUrl, "C", "P"));

			try (PDDocument doc = Loader.loadPDF(output.toFile())) {
				assertThat(doc.getNumberOfPages()).isEqualTo(2);
				assertThat(imageCount(doc.getPage(1))).isZero();
			}
		}
	}

	@Test
	void aBaseUrlTooLongForTheCardIsShortened() throws IOException {
		Path output = tempDir.resolve("long-url.pdf");
		String baseUrl = "https://gallery.example.com/" + "a-very-long-path-segment/".repeat(8);
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));

		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(output, 3, 4, false, "Event", baseUrl, "", "C", "P"));

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(2);
			stripper.setEndPage(2);
			String back = stripper.getText(doc);
			assertThat(back).contains("...").doesNotContain(baseUrl);
		}
	}

	@Test
	void aPasswordTooLongForTheCardIsStillPrintedInFull() throws IOException {
		Path output = tempDir.resolve("long-password.pdf");
		String password = "Abcdefghijklmnopqrstuvwxyz0123456789";
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", password));

		pdfService.createPdf(codes, generateQrImages(codes),
				new PdfOptions(output, 3, 4, false, "Event", "https://base.com", "", "C", "P"));

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(2);
			stripper.setEndPage(2);
			assertThat(stripper.getText(doc)).contains(password);
		}
	}

	private Path writePng(Path file) throws IOException {
		BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.RED);
		graphics.fillRect(0, 0, 120, 60);
		graphics.dispose();
		ImageIO.write(image, "PNG", file.toFile());
		return file;
	}

	private long imageCount(PDPage page) {
		PDResources resources = page.getResources();
		long count = 0;
		for (COSName name : resources.getXObjectNames()) {
			if (resources.isImageXObject(name)) {
				count++;
			}
		}
		return count;
	}

}
