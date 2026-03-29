package com.pabaumgartner.photogallery.wizard.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReadResultTest {

	@Test
	void preservesEventNameAndCodes() {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"));
		CsvReadResult result = new CsvReadResult("Klasse 1a", codes);
		assertThat(result.eventName()).isEqualTo("Klasse 1a");
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().code()).isEqualTo("ABCD-1234-WXYZ");
	}

	@Test
	void emptyListAndName() {
		CsvReadResult result = new CsvReadResult("", List.of());
		assertThat(result.eventName()).isEmpty();
		assertThat(result.codes()).isEmpty();
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		CsvReadResult result = new CsvReadResult(null, List.of());
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void nullCodesDefaultsToEmptyList() {
		CsvReadResult result = new CsvReadResult("Event", null);
		assertThat(result.codes()).isEmpty();
	}

	@Test
	void bothNullsCoercedToDefaults() {
		CsvReadResult result = new CsvReadResult(null, null);
		assertThat(result.eventName()).isEmpty();
		assertThat(result.codes()).isEmpty();
	}

}
