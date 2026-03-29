package com.pabaumgartner.photogallery.wizard.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PicPeakPropertiesTest {

	private PicPeakProperties withAllNulls() {
		return new PicPeakProperties(false, true, null, null, null, null, null, null, null, false, null, 0, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, null,
				null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private PicPeakProperties withApiUrl(String apiUrl) {
		return new PicPeakProperties(false, true, apiUrl, null, null, null, null, null, null, false, null, 0, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, null,
				null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private PicPeakProperties enabledWith(String apiUrl, String username, String password) {
		return new PicPeakProperties(true, true, apiUrl, username, password, null, null, null, null, false, null, 0,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
				null, null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	private PicPeakProperties withValues(int expirationDays, int cssTemplateId) {
		return new PicPeakProperties(false, true, null, null, null, null, null, null, null, false, null, expirationDays,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
				null, null, cssTemplateId, null, null, null, null, null, null, null, null, null, null, null, 0);
	}

	@Test
	void allNullsDefaultToSensibleValues() {
		PicPeakProperties props = withAllNulls();
		assertThat(props.apiUrl()).isEmpty();
		assertThat(props.username()).isEmpty();
		assertThat(props.password()).isEmpty();
		assertThat(props.createEvents()).isTrue();
		assertThat(props.eventType()).isEqualTo("schulfotos");
		assertThat(props.eventDate()).isEmpty();
		assertThat(props.customerEmail()).isEmpty();
		assertThat(props.adminEmail()).isEmpty();
		assertThat(props.welcomeMessage()).isEmpty();
		assertThat(props.expirationDays()).isEqualTo(30);
		assertThat(props.headerStyle()).isEqualTo("standard");
		assertThat(props.heroDividerStyle()).isEqualTo("wave");
		assertThat(props.cssTemplateId()).isEqualTo(1);
		assertThat(props.colorTheme()).isEqualTo("default");
		assertThat(props.protectionLevel()).isEqualTo("standard");
		assertThat(props.sourceMode()).isEqualTo("managed");
		assertThat(props.heroImageAnchor()).isEqualTo("center");
		assertThat(props.heroLogoSize()).isEqualTo("medium");
		assertThat(props.heroLogoPosition()).isEqualTo("top");
		assertThat(props.maxPasswordRetries()).isEqualTo(3);
	}

	@Test
	void apiUrlTrailingSlashIsStripped() {
		PicPeakProperties props = withApiUrl("https://api.picpeak.com/");
		assertThat(props.apiUrl()).isEqualTo("https://api.picpeak.com");
	}

	@Test
	void apiUrlWithoutTrailingSlashPreserved() {
		PicPeakProperties props = withApiUrl("https://api.picpeak.com");
		assertThat(props.apiUrl()).isEqualTo("https://api.picpeak.com");
	}

	@Test
	void createEventsTrueIsPreserved() {
		PicPeakProperties props = withAllNulls();
		assertThat(props.createEvents()).isTrue();
	}

	@Test
	void createEventsFalseIsPreserved() {
		PicPeakProperties props = new PicPeakProperties(false, false, null, null, null, null, null, null, null, false,
				null, 0, false, false, false, false, false, false, false, false, false, false, false, false, false,
				false, false, null, null, 0, null, null, null, null, null, null, null, null, null, null, null, 0);
		assertThat(props.createEvents()).isFalse();
	}

	@Test
	void negativeExpirationDaysDefaultsTo30() {
		PicPeakProperties props = withValues(-5, 1);
		assertThat(props.expirationDays()).isEqualTo(30);
	}

	@Test
	void negativeCssTemplateIdDefaultsTo1() {
		PicPeakProperties props = withValues(30, -3);
		assertThat(props.cssTemplateId()).isEqualTo(1);
	}

	@Test
	void customValuesPreserved() {
		PicPeakProperties props = new PicPeakProperties(true, false, "https://custom.api", "user", "pass", "wedding",
				"2026-01-01", "cust@email.com", "admin@email.com", true, "Welcome!", 60, true, true, true, true, true,
				true, true, true, true, true, true, true, true, true, true, "cover", "none", 5, "dark", "strict",
				"external", "top", "large", "bottom", 10, 20, 30, "external/path", 99, 5);
		assertThat(props.enabled()).isTrue();
		assertThat(props.createEvents()).isFalse();
		assertThat(props.apiUrl()).isEqualTo("https://custom.api");
		assertThat(props.username()).isEqualTo("user");
		assertThat(props.password()).isEqualTo("pass");
		assertThat(props.eventType()).isEqualTo("wedding");
		assertThat(props.eventDate()).isEqualTo("2026-01-01");
		assertThat(props.customerEmail()).isEqualTo("cust@email.com");
		assertThat(props.adminEmail()).isEqualTo("admin@email.com");
		assertThat(props.requirePassword()).isTrue();
		assertThat(props.welcomeMessage()).isEqualTo("Welcome!");
		assertThat(props.expirationDays()).isEqualTo(60);
		assertThat(props.headerStyle()).isEqualTo("cover");
		assertThat(props.heroDividerStyle()).isEqualTo("none");
		assertThat(props.cssTemplateId()).isEqualTo(5);
		assertThat(props.colorTheme()).isEqualTo("dark");
		assertThat(props.protectionLevel()).isEqualTo("strict");
		assertThat(props.sourceMode()).isEqualTo("external");
	}

	@Test
	void enabledRequiresApiUrl() {
		assertThatIllegalArgumentException().isThrownBy(() -> enabledWith(null, "user", "pass"))
			.withMessageContaining("api-url");
	}

	@Test
	void enabledRequiresUsername() {
		assertThatIllegalArgumentException().isThrownBy(() -> enabledWith("https://pics.example.com", null, "pass"))
			.withMessageContaining("username");
	}

	@Test
	void enabledRequiresPassword() {
		assertThatIllegalArgumentException().isThrownBy(() -> enabledWith("https://pics.example.com", "user", null))
			.withMessageContaining("password");
	}

	@Test
	void enabledWithAllCredentialsSucceeds() {
		PicPeakProperties props = enabledWith("https://pics.example.com", "user", "pass");
		assertThat(props.enabled()).isTrue();
		assertThat(props.apiUrl()).isEqualTo("https://pics.example.com");
		assertThat(props.username()).isEqualTo("user");
		assertThat(props.password()).isEqualTo("pass");
	}

	@Test
	void disabledAllowsMissingCredentials() {
		PicPeakProperties props = withAllNulls();
		assertThat(props.enabled()).isFalse();
		assertThat(props.apiUrl()).isEmpty();
		assertThat(props.username()).isEmpty();
		assertThat(props.password()).isEmpty();
	}

}
