package com.pabaumgartner.photogallery.wizard.model;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WizardExecutionResultTest {

	@Test
	void allFieldsPreservedWhenProvided() {
		WizardExecutionResult result = new WizardExecutionResult("AB12", "Klasse 1a", 17, 2, Path.of("codes.csv"),
				Path.of("qr.pdf"));
		assertThat(result.eventCode()).isEqualTo("AB12");
		assertThat(result.eventName()).isEqualTo("Klasse 1a");
		assertThat(result.codeCount()).isEqualTo(17);
		assertThat(result.pageCount()).isEqualTo(2);
		assertThat(result.csvPath()).isEqualTo(Path.of("codes.csv"));
		assertThat(result.pdfPath()).isEqualTo(Path.of("qr.pdf"));
	}

	@Test
	void nullEventCodeDefaultsToEmpty() {
		WizardExecutionResult result = new WizardExecutionResult(null, "Event", 1, 1, Path.of("a.csv"),
				Path.of("a.pdf"));
		assertThat(result.eventCode()).isEmpty();
	}

	@Test
	void nullEventNameDefaultsToEmpty() {
		WizardExecutionResult result = new WizardExecutionResult("AB12", null, 1, 1, Path.of("a.csv"),
				Path.of("a.pdf"));
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void bothNullsCoercedToEmpty() {
		WizardExecutionResult result = new WizardExecutionResult(null, null, 0, 0, Path.of("a.csv"), Path.of("a.pdf"));
		assertThat(result.eventCode()).isEmpty();
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void recordEquality() {
		WizardExecutionResult a = new WizardExecutionResult("AB12", "Event", 5, 1, Path.of("a.csv"), Path.of("a.pdf"));
		WizardExecutionResult b = new WizardExecutionResult("AB12", "Event", 5, 1, Path.of("a.csv"), Path.of("a.pdf"));
		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

}
