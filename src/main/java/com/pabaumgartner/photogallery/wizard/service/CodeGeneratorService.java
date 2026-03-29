package com.pabaumgartner.photogallery.wizard.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CodeGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeGeneratorService.class);

	private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private static final int GROUP_LENGTH = 4;

	private static final String PASSWORD_DIGITS = "234679";

	private static final String PASSWORD_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final String PASSWORD_LOWERCASE = "abcdefghijkmnopqrstuvwxyz";

	private static final String PASSWORD_SPECIAL = "!@#$%&+";

	private static final String PASSWORD_CHARSET = PASSWORD_UPPERCASE + PASSWORD_LOWERCASE + PASSWORD_DIGITS
			+ PASSWORD_SPECIAL;

	private static final int REQUIRED_CLASSES = 4;

	private static final int MAX_PASSWORD_GENERATION_ATTEMPTS = 100;

	private final int passwordLength;

	private final SecureRandom random = new SecureRandom();

	public CodeGeneratorService(SchulfotosProperties schulfotosProperties) {
		this.passwordLength = schulfotosProperties.passwordLength();
		if (PASSWORD_CHARSET.length() < passwordLength) {
			throw new IllegalStateException("PASSWORD_CHARSET length (" + PASSWORD_CHARSET.length()
					+ ") must be >= passwordLength (" + passwordLength + ")");
		}
	}

	public List<GalleryCode> generateCodes(String eventCode, int count) {
		if (eventCode == null || eventCode.isBlank()) {
			throw new IllegalArgumentException("Event code must not be empty");
		}
		eventCode = eventCode.trim().toUpperCase();
		if (!eventCode.matches("^[A-Z0-9]{4}$")) {
			throw new IllegalArgumentException(
					"Event code must be exactly 4 alphanumeric characters, got: '" + eventCode + "'");
		}
		if (count <= 0) {
			throw new IllegalArgumentException("Code count must be positive, got: " + count);
		}

		LinkedHashSet<String> uniqueCodes = new LinkedHashSet<>();
		int maxAttempts = count * 10;
		int attempts = 0;

		while (uniqueCodes.size() < count && attempts < maxAttempts) {
			String group2 = randomGroup();
			String group3 = randomGroup();
			String code = eventCode + "-" + group2 + "-" + group3;
			uniqueCodes.add(code);
			attempts++;
		}

		if (uniqueCodes.size() < count) {
			LOGGER.warn("Could only generate {} unique codes out of {} requested", uniqueCodes.size(), count);
		}

		LinkedHashSet<String> usedPasswords = new LinkedHashSet<>();
		List<GalleryCode> codes = new ArrayList<>();
		for (String code : uniqueCodes) {
			String password;
			int passwordAttempts = 0;
			do {
				password = generatePassword();
				passwordAttempts++;
			}
			while (!usedPasswords.add(password) && passwordAttempts < MAX_PASSWORD_GENERATION_ATTEMPTS);
			if (passwordAttempts >= MAX_PASSWORD_GENERATION_ATTEMPTS) {
				LOGGER.warn("Could not generate a unique password for code '{}' after {} attempts", code,
						MAX_PASSWORD_GENERATION_ATTEMPTS);
			}
			codes.add(new GalleryCode(code, password));
		}

		LOGGER.atInfo()
			.addArgument(() -> codes.size())
			.addArgument(eventCode)
			.log("Generated {} unique gallery codes with event prefix '{}'");
		return codes;
	}

	private String randomGroup() {
		StringBuilder sb = new StringBuilder(GROUP_LENGTH);
		for (int i = 0; i < GROUP_LENGTH; i++) {
			sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
		}
		return sb.toString();
	}

	String generatePassword() {
		List<Character> mandatory = new ArrayList<>(REQUIRED_CLASSES);
		mandatory.add(PASSWORD_DIGITS.charAt(random.nextInt(PASSWORD_DIGITS.length())));
		mandatory.add(PASSWORD_UPPERCASE.charAt(random.nextInt(PASSWORD_UPPERCASE.length())));
		mandatory.add(PASSWORD_LOWERCASE.charAt(random.nextInt(PASSWORD_LOWERCASE.length())));
		mandatory.add(PASSWORD_SPECIAL.charAt(random.nextInt(PASSWORD_SPECIAL.length())));

		List<Character> pool = new ArrayList<>(PASSWORD_CHARSET.length());
		for (int i = 0; i < PASSWORD_CHARSET.length(); i++) {
			char c = PASSWORD_CHARSET.charAt(i);
			if (!mandatory.contains(c)) {
				pool.add(c);
			}
		}
		Collections.shuffle(pool, random);

		List<Character> passwordChars = new ArrayList<>(passwordLength);
		passwordChars.addAll(mandatory);
		for (int i = 0; i < passwordLength - REQUIRED_CLASSES; i++) {
			passwordChars.add(pool.get(i));
		}

		Collections.shuffle(passwordChars, random);
		if (isSpecialCharacter(passwordChars.getFirst())) {
			for (int i = 1; i < passwordChars.size(); i++) {
				if (!isSpecialCharacter(passwordChars.get(i))) {
					Collections.swap(passwordChars, 0, i);
					break;
				}
			}
		}
		int last = passwordChars.size() - 1;
		if (isSpecialCharacter(passwordChars.getLast())) {
			for (int i = last - 1; i >= 0; i--) {
				if (!isSpecialCharacter(passwordChars.get(i))) {
					Collections.swap(passwordChars, last, i);
					break;
				}
			}
		}
		StringBuilder sb = new StringBuilder(passwordLength);
		for (char c : passwordChars) {
			sb.append(c);
		}
		return sb.toString();
	}

	private boolean isSpecialCharacter(char c) {
		return PASSWORD_SPECIAL.indexOf(c) >= 0;
	}

}
