package com.pabaumgartner.photogallery.wizard.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchulfotosPropertiesTest {

	@Test
	void allNullsDefaultToSensibleValues() {
		SchulfotosProperties props = new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null,
				null, null, null, null, 0);
		assertThat(props.baseUrl()).isEqualTo("https://example.com/schulfotos");
		assertThat(props.galleryUrl()).isEqualTo("https://example.com/schulfotos/?code=");
		assertThat(props.defaultCodeCount()).isEqualTo(17);
		assertThat(props.qrSize()).isEqualTo(200);
		assertThat(props.gridColumns()).isEqualTo(3);
		assertThat(props.gridRows()).isEqualTo(4);
		assertThat(props.galleryCodeLabel()).isEqualTo("GALERIE CODE");
		assertThat(props.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
		assertThat(props.outputDir()).isEqualTo("schulfotos");
		assertThat(props.codeCharset()).isEqualTo("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		assertThat(props.logoPath()).isEqualTo("configuration/logo.png");
		assertThat(props.klassenfotoFolder()).isEqualTo("klassenfoto");
		assertThat(props.portraitPrefix()).isEqualTo("portrait-");
		assertThat(props.watermarkedSuffix()).isEqualTo("-watermarked");
		assertThat(props.passwordLength()).isEqualTo(9);
	}

	@Test
	void allBlanksDefaultToSensibleValues() {
		SchulfotosProperties props = new SchulfotosProperties("  ", "  ", 0, 0, 0, 0, false, "  ", "  ", "  ", "  ",
				"  ", "  ", "  ", "  ", 0);
		assertThat(props.baseUrl()).isEqualTo("https://example.com/schulfotos");
		assertThat(props.galleryUrl()).isEqualTo("https://example.com/schulfotos/?code=");
		assertThat(props.galleryCodeLabel()).isEqualTo("GALERIE CODE");
		assertThat(props.galleryPasswordLabel()).isEqualTo("GALERIE PASSWORT");
		assertThat(props.outputDir()).isEqualTo("schulfotos");
		assertThat(props.codeCharset()).isEqualTo("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		assertThat(props.logoPath()).isEqualTo("configuration/logo.png");
		assertThat(props.klassenfotoFolder()).isEqualTo("klassenfoto");
		assertThat(props.portraitPrefix()).isEqualTo("portrait-");
		assertThat(props.watermarkedSuffix()).isEqualTo("-watermarked");
	}

	@Test
	void customValuesPreserved() {
		SchulfotosProperties props = new SchulfotosProperties("https://custom.ch", "https://custom.ch/?code=", 20, 300,
				4, 5, true, "CODE", "PW", "output", "ABC", "logo.png", "groupphoto", "individual-", "-wm", 12);
		assertThat(props.baseUrl()).isEqualTo("https://custom.ch");
		assertThat(props.galleryUrl()).isEqualTo("https://custom.ch/?code=");
		assertThat(props.defaultCodeCount()).isEqualTo(20);
		assertThat(props.qrSize()).isEqualTo(300);
		assertThat(props.gridColumns()).isEqualTo(4);
		assertThat(props.gridRows()).isEqualTo(5);
		assertThat(props.showCuttingLines()).isTrue();
		assertThat(props.galleryCodeLabel()).isEqualTo("CODE");
		assertThat(props.galleryPasswordLabel()).isEqualTo("PW");
		assertThat(props.outputDir()).isEqualTo("output");
		assertThat(props.codeCharset()).isEqualTo("ABC");
		assertThat(props.logoPath()).isEqualTo("logo.png");
		assertThat(props.klassenfotoFolder()).isEqualTo("groupphoto");
		assertThat(props.portraitPrefix()).isEqualTo("individual-");
		assertThat(props.watermarkedSuffix()).isEqualTo("-wm");
		assertThat(props.passwordLength()).isEqualTo(12);
	}

	@Test
	void negativeValuesDefaultToPositive() {
		SchulfotosProperties props = new SchulfotosProperties(null, null, -1, -1, -1, -1, false, null, null, null, null,
				null, null, null, null, -1);
		assertThat(props.defaultCodeCount()).isEqualTo(17);
		assertThat(props.qrSize()).isEqualTo(200);
		assertThat(props.gridColumns()).isEqualTo(3);
		assertThat(props.gridRows()).isEqualTo(4);
		assertThat(props.passwordLength()).isEqualTo(9);
	}

}
