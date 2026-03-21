package com.pabaumgartner.photogallery.wizard.verification;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;

class TaikaiVerificationTest {

	@Test
	void should_satisfy_architecture_rules() {
		Taikai.builder()
			.namespace("com.pabaumgartner.photogallery.wizard")
			.java(java -> java.noUsageOf("java.util.Date", "Use java.time instead of java.util.Date")
				.noUsageOf("java.util.Calendar", "Use java.time instead of java.util.Calendar"))
			.build()
			.check();
	}

}
