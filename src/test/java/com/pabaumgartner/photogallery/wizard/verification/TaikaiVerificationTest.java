package com.pabaumgartner.photogallery.wizard.verification;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;

class TaikaiVerificationTest {

	@Test
	void should_satisfy_architecture_rules() {
		Taikai.builder()
				.namespace("com.pabaumgartner.photogallery.wizard")
				.java(java -> java.noUsageOf(java.util.Date.class).noUsageOf(java.util.Calendar.class))
				.build()
				.check();
	}

}
