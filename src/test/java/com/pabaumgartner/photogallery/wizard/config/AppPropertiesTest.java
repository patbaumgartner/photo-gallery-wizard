package com.pabaumgartner.photogallery.wizard.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPropertiesTest {

	@Test
	void validEventCodeIsUppercasedAndTrimmed() {
		AppProperties props = new AppProperties("  ab12  ", "name", "wm.png", 800, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.eventCode()).isEqualTo("AB12");
	}

	@Test
	void blankEventCodeStaysEmpty() {
		AppProperties props = new AppProperties("", "name", "wm.png", 800, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.eventCode()).isEmpty();
	}

	@Test
	void nullEventCodeDefaultsToEmpty() {
		AppProperties props = new AppProperties(null, "name", "wm.png", 800, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.eventCode()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC", "ABCDE", "AB-C", "AB!C", "ab c" })
	void invalidEventCodeThrows(String code) {
		assertThatThrownBy(() -> new AppProperties(code, "", null, 0, 0f, 0f, 0f, 0, 0, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("4 alphanumeric");
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABCD", "1234", "A1B2", "abcd", "  AbCd  " })
	void validEventCodesAccepted(String code) {
		AppProperties props = new AppProperties(code, "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.eventCode()).isEqualTo(code.trim().toUpperCase());
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		AppProperties props = new AppProperties("", null, null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.eventName()).isEmpty();
	}

	@Test
	void nullWatermarkPathDefaultsToConfiguration() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("configuration/watermark.png");
	}

	@Test
	void blankWatermarkPathDefaultsToConfiguration() {
		AppProperties props = new AppProperties("", "", "  ", 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("configuration/watermark.png");
	}

	@Test
	void customWatermarkPathPreserved() {
		AppProperties props = new AppProperties("", "", "custom/wm.png", 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkPath()).isEqualTo("custom/wm.png");
	}

	@Test
	void zeroResizeMaxEdgeDefaultsTo1200() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(1200);
	}

	@Test
	void negativeResizeMaxEdgeDefaultsTo1200() {
		AppProperties props = new AppProperties("", "", null, -5, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(1200);
	}

	@Test
	void positiveResizeMaxEdgePreserved() {
		AppProperties props = new AppProperties("", "", null, 800, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.resizeMaxEdge()).isEqualTo(800);
	}

	@Test
	void zeroWatermarkOpacityDefaultsTo03() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkOpacity()).isEqualTo(0.3f);
	}

	@Test
	void customWatermarkOpacityPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0.5f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkOpacity()).isEqualTo(0.5f);
	}

	@Test
	void zeroWatermarkScaleDefaultsTo04() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.watermarkScale()).isEqualTo(0.4f);
	}

	@Test
	void customWatermarkScalePreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0.6f, 0f, 0, 0, null);
		assertThat(props.watermarkScale()).isEqualTo(0.6f);
	}

	@Test
	void zeroJpegQualityDefaultsTo09() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.jpegQuality()).isEqualTo(0.9f);
	}

	@Test
	void customJpegQualityPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0.8f, 0, 0, null);
		assertThat(props.jpegQuality()).isEqualTo(0.8f);
	}

	@Test
	void zeroLogoConnectTimeoutDefaultsTo5000() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.logoConnectTimeoutMs()).isEqualTo(5000);
	}

	@Test
	void customLogoConnectTimeoutPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 3000, 0, null);
		assertThat(props.logoConnectTimeoutMs()).isEqualTo(3000);
	}

	@Test
	void zeroLogoReadTimeoutDefaultsTo10000() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.logoReadTimeoutMs()).isEqualTo(10000);
	}

	@Test
	void customLogoReadTimeoutPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 15000, null);
		assertThat(props.logoReadTimeoutMs()).isEqualTo(15000);
	}

	@Test
	void nullFilenameStripPostfixDefaultsToNeu() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, null);
		assertThat(props.filenameStripPostfix()).isEqualTo("_NEU");
	}

	@Test
	void customFilenameStripPostfixPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, "_FINAL");
		assertThat(props.filenameStripPostfix()).isEqualTo("_FINAL");
	}

	@Test
	void emptyFilenameStripPostfixPreserved() {
		AppProperties props = new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0, "");
		assertThat(props.filenameStripPostfix()).isEmpty();
	}

}
