package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpEndpointsTest {

	@ParameterizedTest
	@ValueSource(strings = { "https://example.com/upload.php", "https://gallery.example.com:8443/api",
			"http://localhost:8080/api", "http://127.0.0.1/api", "http://[::1]/api", "http://10.0.0.5/api",
			"http://172.16.4.9/api", "http://192.168.1.10:3000/api", "http://gallery.local/api",
			"http://pics.localhost/api" })
	void credentialsMayBeSentOverHttpsOrToTheLocalNetwork(String url) {
		assertThatCode(() -> HttpEndpoints.requireCredentialSafeTransport(URI.create(url))).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = { "http://example.com/upload.php", "http://8.8.8.8/api", "http://gallery.example.com/api" })
	void credentialsAreNeverSentInClearTextToAPublicHost(String url) {
		assertThatThrownBy(() -> HttpEndpoints.requireCredentialSafeTransport(URI.create(url)))
			.isInstanceOf(IOException.class)
			.hasMessageContaining("Refusing to send credentials");
	}

	@Test
	void multipartFilenamesCannotInjectHeaderLines() {
		assertThat(HttpEndpoints.sanitizeMultipartFilename("photo.jpg")).isEqualTo("photo.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\"b.jpg")).isEqualTo("a_b.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\r\nContent-Type: evil\r\n\r\n.jpg"))
			.isEqualTo("a__Content-Type: evil____.jpg");
		assertThat(HttpEndpoints.sanitizeMultipartFilename("a\\b.jpg")).isEqualTo("a_b.jpg");
	}

	@Test
	void loggedBodiesAreCollapsedAndTruncated() {
		assertThat(HttpEndpoints.truncateForLog(null)).isEqualTo("<empty>");
		assertThat(HttpEndpoints.truncateForLog("   ")).isEqualTo("<empty>");
		assertThat(HttpEndpoints.truncateForLog("line one\n  line two")).isEqualTo("line one line two");
		assertThat(HttpEndpoints.truncateForLog("x".repeat(500))).hasSize(201).endsWith("…");
	}

	@Test
	void restoreInterruptFlagOnlyReactsToInterruption() {
		HttpEndpoints.restoreInterruptFlag(new IOException("boom"));
		assertThat(Thread.currentThread().isInterrupted()).isFalse();

		HttpEndpoints.restoreInterruptFlag(new InterruptedException("stop"));
		assertThat(Thread.interrupted()).isTrue();
	}

	@Test
	void clientIsConfiguredWithAConnectTimeout() {
		assertThat(HttpEndpoints.newClient().connectTimeout()).contains(HttpEndpoints.CONNECT_TIMEOUT);
	}

}
