package com.pabaumgartner.photogallery.wizard.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GalleryCodeTest {

	@Test
	void fullConstructorPreservesAllFields() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "secret", "https://share.link", 42);
		assertThat(code.code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(code.password()).isEqualTo("secret");
		assertThat(code.shareUrl()).isEqualTo("https://share.link");
		assertThat(code.picPeakEventId()).isEqualTo(42);
	}

	@Test
	void threeArgConstructorDefaultsPicPeakEventId() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "secret", "https://share.link");
		assertThat(code.picPeakEventId()).isEqualTo(0);
		assertThat(code.shareUrl()).isEqualTo("https://share.link");
	}

	@Test
	void twoArgConstructorDefaultsShareUrlAndPicPeakEventId() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "secret");
		assertThat(code.shareUrl()).isEmpty();
		assertThat(code.picPeakEventId()).isEqualTo(0);
	}

	@Test
	void singleArgConstructorDefaultsPasswordShareUrlAndPicPeakEventId() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		assertThat(code.password()).isEmpty();
		assertThat(code.shareUrl()).isEmpty();
		assertThat(code.picPeakEventId()).isEqualTo(0);
	}

	@Test
	void compactConstructorCoercesNullPasswordToEmpty() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", null, "url", 0);
		assertThat(code.password()).isEmpty();
	}

	@Test
	void compactConstructorCoercesNullShareUrlToEmpty() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "pw", null, 0);
		assertThat(code.shareUrl()).isEmpty();
	}

	@Test
	void compactConstructorCoercesBothNullsToEmpty() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", null, null, 0);
		assertThat(code.password()).isEmpty();
		assertThat(code.shareUrl()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABCD-1234-WXYZ", "0000-0000-0000", "ZZZZ-9999-AAAA", "A1B2-C3D4-E5F6" })
	void isValidReturnsTrueForValidCodes(String code) {
		assertThat(GalleryCode.isValid(code)).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "abc", "ABCD", "ABCD-1234", "ABCD-1234-", "ABCD-1234-XYZ", "ABCD-1234-XYZAB",
			"abcd-1234-wxyz", "ABCD_1234_WXYZ", "ABCD 1234 WXYZ", "ABCD-12!4-WXYZ", "----", "ABCD-1234-WXY!" })
	void isValidReturnsFalseForInvalidCodes(String code) {
		assertThat(GalleryCode.isValid(code)).isFalse();
	}

	@Test
	void toUrlReturnsShareUrlWhenPresent() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "pw", "https://direct.link", 0);
		assertThat(code.toUrl("https://gallery.com/?code=")).isEqualTo("https://direct.link");
	}

	@Test
	void toUrlBuildsGalleryUrlWhenShareUrlIsBlank() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "pw", "", 0);
		assertThat(code.toUrl("https://gallery.com/?code=")).isEqualTo("https://gallery.com/?code=ABCD-1234-WXYZ");
	}

	@Test
	void toUrlBuildsGalleryUrlWhenShareUrlIsDefault() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ");
		assertThat(code.toUrl("https://example.com/")).isEqualTo("https://example.com/ABCD-1234-WXYZ");
	}

	@Test
	void toUrlBuildsGalleryUrlWhenShareUrlContainsOnlyWhitespace() {
		GalleryCode code = new GalleryCode("ABCD-1234-WXYZ", "pw", "   ", 0);
		assertThat(code.toUrl("https://example.com/")).isEqualTo("https://example.com/ABCD-1234-WXYZ");
	}

	@Test
	void recordEquality() {
		GalleryCode a = new GalleryCode("ABCD-1234-WXYZ", "pw", "url", 5);
		GalleryCode b = new GalleryCode("ABCD-1234-WXYZ", "pw", "url", 5);
		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	void recordInequalityOnDifferentFields() {
		GalleryCode a = new GalleryCode("ABCD-1234-WXYZ", "pw1", "url", 5);
		GalleryCode b = new GalleryCode("ABCD-1234-WXYZ", "pw2", "url", 5);
		assertThat(a).isNotEqualTo(b);
	}

}
