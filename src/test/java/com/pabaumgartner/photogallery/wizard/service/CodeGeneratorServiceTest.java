package com.pabaumgartner.photogallery.wizard.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorServiceTest {

	private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");

	private static final Pattern PASSWORD_DIGITS = Pattern.compile("[234679]");

	private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-Z]");

	private static final Pattern PASSWORD_LOWER = Pattern.compile("[a-z]");

	private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[!@#$%&+]");

	private CodeGeneratorService service;

	private static SchulfotosProperties defaultSchulfotosProperties() {
		return new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null, null,
				0);
	}

	@BeforeEach
	void setUp() {
		service = new CodeGeneratorService(defaultSchulfotosProperties());
	}

	@Test
	void generateCodesReturnsRequestedCount() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 5);
		assertThat(codes).hasSize(5);
	}

	@Test
	void generateCodesWithSingleCode() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 1);
		assertThat(codes).hasSize(1);
	}

	@Test
	void generateCodesWithLargeCount() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 100);
		assertThat(codes).hasSize(100);
	}

	@Test
	void allCodesStartWithEventPrefix() {
		List<GalleryCode> codes = service.generateCodes("XY99", 10);
		assertThat(codes).allSatisfy(code -> assertThat(code.code()).startsWith("XY99-"));
	}

	@Test
	void allCodesMatchExpectedFormat() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 20);
		assertThat(codes).allSatisfy(code -> assertThat(code.code()).matches(CODE_PATTERN));
	}

	@Test
	void allCodesAreUnique() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 50);
		Set<String> codeSet = new HashSet<>();
		for (GalleryCode code : codes) {
			assertThat(codeSet.add(code.code())).as("Duplicate code: " + code.code()).isTrue();
		}
	}

	@Test
	void allPasswordsAreUnique() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 50);
		Set<String> pwSet = new HashSet<>();
		for (GalleryCode code : codes) {
			assertThat(pwSet.add(code.password())).as("Duplicate password found").isTrue();
		}
	}

	@Test
	void passwordHasCorrectLength() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 20);
		assertThat(codes).allSatisfy(code -> assertThat(code.password()).hasSize(9));
	}

	@Test
	void passwordContainsAllRequiredCharacterClasses() {
		for (int i = 0; i < 50; i++) {
			String password = service.generatePassword();
			assertThat(PASSWORD_DIGITS.matcher(password).find())
				.as("Password '%s' should contain a digit from 234679", password)
				.isTrue();
			assertThat(PASSWORD_UPPER.matcher(password).find()).as("Password '%s' should contain uppercase", password)
				.isTrue();
			assertThat(PASSWORD_LOWER.matcher(password).find()).as("Password '%s' should contain lowercase", password)
				.isTrue();
			assertThat(PASSWORD_SPECIAL.matcher(password).find())
				.as("Password '%s' should contain a special character", password)
				.isTrue();
		}
	}

	@Test
	void passwordDoesNotStartWithSpecialCharacter() {
		for (int i = 0; i < 100; i++) {
			String password = service.generatePassword();
			char first = password.charAt(0);
			assertThat("!@#$%&+").as("Password '%s' should not start with special char", password)
				.doesNotContain(String.valueOf(first));
		}
	}

	@Test
	void passwordDoesNotEndWithSpecialCharacter() {
		for (int i = 0; i < 100; i++) {
			String password = service.generatePassword();
			char last = password.charAt(password.length() - 1);
			assertThat("!@#$%&+").as("Password '%s' should not end with special char", password)
				.doesNotContain(String.valueOf(last));
		}
	}

	@Test
	void eventCodeIsTrimmedAndUppercased() {
		List<GalleryCode> codes = service.generateCodes("  ab12  ", 1);
		assertThat(codes.getFirst().code()).startsWith("AB12-");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void nullOrBlankEventCodeThrows(String eventCode) {
		assertThatThrownBy(() -> service.generateCodes(eventCode, 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC", "ABCDE", "AB-C", "A!CD" })
	void invalidFormatEventCodeThrows(String eventCode) {
		assertThatThrownBy(() -> service.generateCodes(eventCode, 1)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("4 alphanumeric");
	}

	@Test
	void zeroCountThrows() {
		assertThatThrownBy(() -> service.generateCodes("ABCD", 0)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("positive");
	}

	@Test
	void negativeCountThrows() {
		assertThatThrownBy(() -> service.generateCodes("ABCD", -5)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void passwordOnlyContainsAllowedCharacters() {
		String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz234679!@#$%&+";
		for (int i = 0; i < 50; i++) {
			String password = service.generatePassword();
			for (char c : password.toCharArray()) {
				assertThat(allowed).contains(String.valueOf(c));
			}
		}
	}

	@Test
	void codesShareUrlIsEmptyByDefault() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 3);
		assertThat(codes).allSatisfy(code -> assertThat(code.shareUrl()).isEmpty());
	}

	@Test
	void codesPicPeakEventIdIsZeroByDefault() {
		List<GalleryCode> codes = service.generateCodes("ABCD", 3);
		assertThat(codes).allSatisfy(code -> assertThat(code.picPeakEventId()).isEqualTo(0));
	}

}
