package com.pabaumgartner.photogallery.wizard.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPropertiesTest {

	@Test
	void validEventCodeIsUppercasedAndTrimmed() {
		AppProperties props = new AppProperties("  ab12  ", "name");
		assertThat(props.eventCode()).isEqualTo("AB12");
	}

	@Test
	void blankEventCodeStaysEmpty() {
		AppProperties props = new AppProperties("", "name");
		assertThat(props.eventCode()).isEmpty();
	}

	@Test
	void nullEventCodeDefaultsToEmpty() {
		AppProperties props = new AppProperties(null, "name");
		assertThat(props.eventCode()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC", "ABCDE", "AB-C", "AB!C", "ab c" })
	void invalidEventCodeThrows(String code) {
		assertThatThrownBy(() -> new AppProperties(code, "")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("4 alphanumeric");
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABCD", "1234", "A1B2", "abcd", "  AbCd  " })
	void validEventCodesAccepted(String code) {
		AppProperties props = new AppProperties(code, "");
		assertThat(props.eventCode()).isEqualTo(code.trim().toUpperCase());
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		AppProperties props = new AppProperties("", null);
		assertThat(props.eventName()).isEmpty();
	}

}
