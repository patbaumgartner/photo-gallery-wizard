package com.pabaumgartner.photogallery.wizard.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImagePropertiesTest {

	@Test
	void nullWatermarkPathDefaultsToConfiguration() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("configuration/watermark.png");
	}

	@Test
	void blankWatermarkPathDefaultsToConfiguration() {
		ImageProperties props = new ImageProperties("  ", 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("configuration/watermark.png");
	}

	@Test
	void customWatermarkPathPreserved() {
		ImageProperties props = new ImageProperties("custom/wm.png", 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("custom/wm.png");
	}

	@Test
	void zeroResizeMaxEdgeDefaultsTo1200() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(1200);
	}

	@Test
	void negativeResizeMaxEdgeDefaultsTo1200() {
		ImageProperties props = new ImageProperties(null, -5, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(1200);
	}

	@Test
	void positiveResizeMaxEdgePreserved() {
		ImageProperties props = new ImageProperties(null, 800, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(800);
	}

	@Test
	void zeroWatermarkOpacityDefaultsTo03() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkOpacity()).isEqualTo(0.3f);
	}

	@Test
	void customWatermarkOpacityPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0.5f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkOpacity()).isEqualTo(0.5f);
	}

	@Test
	void zeroWatermarkScaleDefaultsTo04() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkScale()).isEqualTo(0.4f);
	}

	@Test
	void customWatermarkScalePreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0.6f, 0f, 0, 0, null);
		assertThat(props.watermarkScale()).isEqualTo(0.6f);
	}

	@Test
	void zeroJpegQualityDefaultsTo09() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.jpegQuality()).isEqualTo(0.9f);
	}

	@Test
	void customJpegQualityPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0.8f, 0, 0, null);
		assertThat(props.jpegQuality()).isEqualTo(0.8f);
	}

	@Test
	void zeroLogoConnectTimeoutDefaultsTo5000() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.logoConnectTimeoutMs()).isEqualTo(5000);
	}

	@Test
	void customLogoConnectTimeoutPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 3000, 0, null);
		assertThat(props.logoConnectTimeoutMs()).isEqualTo(3000);
	}

	@Test
	void zeroLogoReadTimeoutDefaultsTo10000() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.logoReadTimeoutMs()).isEqualTo(10000);
	}

	@Test
	void customLogoReadTimeoutPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 15000, null);
		assertThat(props.logoReadTimeoutMs()).isEqualTo(15000);
	}

	@Test
	void nullFilenameStripPostfixDefaultsToNeu() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.filenameStripPostfix()).isEqualTo("_NEU");
	}

	@Test
	void customFilenameStripPostfixPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, "_FINAL");
		assertThat(props.filenameStripPostfix()).isEqualTo("_FINAL");
	}

	@Test
	void emptyFilenameStripPostfixPreserved() {
		ImageProperties props = new ImageProperties(null, 0, 0f, 0f, 0f, 0, 0, "");
		assertThat(props.filenameStripPostfix()).isEmpty();
	}

}
