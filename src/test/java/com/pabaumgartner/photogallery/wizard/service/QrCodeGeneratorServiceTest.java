package com.pabaumgartner.photogallery.wizard.service;

import java.awt.image.BufferedImage;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorServiceTest {

	private QrCodeGeneratorService service;

	@BeforeEach
	void setUp() {
		service = new QrCodeGeneratorService();
	}

	@Test
	void generateQrCodeReturnsNonNullImage() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage image = service.generateQrCode(code, "https://gallery.com/?code=", 200, 1);
		assertThat(image).isNotNull();
	}

	@Test
	void generateQrCodeReturnsScaledImage() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage image = service.generateQrCode(code, "https://gallery.com/?code=", 200, 1);
		assertThat(image.getWidth()).isEqualTo(600);
		assertThat(image.getHeight()).isEqualTo(600);
	}

	@Test
	void generateQrCodeWithDifferentSizes() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage small = service.generateQrCode(code, "https://gallery.com/?code=", 100, 1);
		BufferedImage large = service.generateQrCode(code, "https://gallery.com/?code=", 400, 1);
		assertThat(small.getWidth()).isEqualTo(300);
		assertThat(large.getWidth()).isEqualTo(1200);
	}

	@Test
	void generateQrCodeWithDifferentNumbers() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage img1 = service.generateQrCode(code, "https://gallery.com/?code=", 200, 1);
		BufferedImage img17 = service.generateQrCode(code, "https://gallery.com/?code=", 200, 17);
		assertThat(img1).isNotNull();
		assertThat(img17).isNotNull();
	}

	@Test
	void generateQrCodeCenterRegionContainsWhiteOverlayPixels() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage image = service.generateQrCode(code, "https://gallery.com/?code=", 200, 5);

		int centerX = image.getWidth() / 2;
		int centerY = image.getHeight() / 2;

		int maxBrightness = 0;
		for (int x = centerX - 12; x <= centerX + 12; x++) {
			for (int y = centerY - 12; y <= centerY + 12; y++) {
				int rgb = image.getRGB(x, y);
				int red = (rgb >> 16) & 0xFF;
				int green = (rgb >> 8) & 0xFF;
				int blue = rgb & 0xFF;
				maxBrightness = Math.max(maxBrightness, red + green + blue);
			}
		}

		assertThat(maxBrightness).as("Center overlay should contain bright (near-white) circle pixels")
			.isGreaterThan(700);
	}

	@Test
	void generateQrCodeCornerHasBlackModules() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		BufferedImage image = service.generateQrCode(code, "https://gallery.com/?code=", 300, 1);

		int topLeftRgb = image.getRGB(10, 10);
		int red = (topLeftRgb >> 16) & 0xFF;
		int green = (topLeftRgb >> 8) & 0xFF;
		int blue = topLeftRgb & 0xFF;

		assertThat(red + green + blue).as("Top-left corner should be dark (QR finder pattern)").isLessThan(200);
	}

	@Test
	void differentCodesProduceDifferentImages() {
		GalleryCode code1 = new GalleryCode("ABCD-1234-WXYZ");
		GalleryCode code2 = new GalleryCode("EFGH-5678-STUV");
		BufferedImage img1 = service.generateQrCode(code1, "https://gallery.com/?code=", 200, 1);
		BufferedImage img2 = service.generateQrCode(code2, "https://gallery.com/?code=", 200, 1);

		boolean identical = true;
		for (int x = 0; x < img1.getWidth() && identical; x += 10) {
			for (int y = 0; y < img1.getHeight() && identical; y += 10) {
				if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
					identical = false;
				}
			}
		}
		assertThat(identical).as("Different codes should produce different QR images").isFalse();
	}

}
