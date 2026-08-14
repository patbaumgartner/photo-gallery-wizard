package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FolderStructureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FolderStructureService.class);

	private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

	private static final Pattern WINDOWS_RESERVED_NAMES = Pattern
		.compile("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$");

	private static final int MAX_SEGMENT_LENGTH = 100;

	private final String klassenfotoFolder;

	private final String portraitPrefix;

	public FolderStructureService(SchulfotosProperties schulfotosProperties) {
		this.klassenfotoFolder = schulfotosProperties.klassenfotoFolder();
		this.portraitPrefix = schulfotosProperties.portraitPrefix();
	}

	/**
	 * Reduces untrusted text (a class name read from a CSV) to a single directory name
	 * that is safe and portable across Linux, macOS and Windows.
	 */
	public static String safeSegment(String value, String fallback) {
		String candidate = value == null ? "" : Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
		candidate = UNSAFE_CHARACTERS.matcher(candidate).replaceAll("-").strip();
		if (candidate.length() > MAX_SEGMENT_LENGTH) {
			// Cutting between the two halves of a surrogate pair would leave an
			// unpaired char that no file system accepts.
			int end = Character.isHighSurrogate(candidate.charAt(MAX_SEGMENT_LENGTH - 1)) ? MAX_SEGMENT_LENGTH - 1
					: MAX_SEGMENT_LENGTH;
			candidate = candidate.substring(0, end);
		}
		// Trailing dots and spaces are silently dropped by Windows, and stripping them
		// also turns "." and ".." into the fallback instead of a directory traversal.
		candidate = candidate.replaceAll("[. ]+$", "");
		if (candidate.isEmpty() || WINDOWS_RESERVED_NAMES.matcher(candidate).matches()) {
			return fallback;
		}
		return candidate;
	}

	public List<Path> listCsvFiles(Path schulfotosDir) throws IOException {
		List<Path> csvFiles = new ArrayList<>();
		if (!Files.isDirectory(schulfotosDir)) {
			return csvFiles;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(schulfotosDir, "*.csv")) {
			for (Path entry : stream) {
				csvFiles.add(entry);
			}
		}
		csvFiles.sort(Path::compareTo);
		return csvFiles;
	}

	public List<Path> listEventFolders(Path schulfotosDir) throws IOException {
		List<Path> eventFolders = new ArrayList<>();
		if (!Files.isDirectory(schulfotosDir)) {
			return eventFolders;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(schulfotosDir, Files::isDirectory)) {
			for (Path entry : stream) {
				if (hasPhotoSubfolders(entry)) {
					eventFolders.add(entry);
				}
				else {
					List<Path> nestedEventFolders = nestedEventFolders(entry);
					if (!nestedEventFolders.isEmpty()) {
						eventFolders.addAll(nestedEventFolders);
						LOGGER.warn(
								"Nested event folders detected under {}. This likely came from a past path-separator issue. "
										+ "Detected folders are still shown, but consider moving them directly under {}.",
								entry, schulfotosDir);
					}
				}
			}
		}
		eventFolders.sort(Path::compareTo);
		return eventFolders;
	}

	private List<Path> nestedEventFolders(Path parent) throws IOException {
		List<Path> nested = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, Files::isDirectory)) {
			for (Path child : stream) {
				if (hasPhotoSubfolders(child)) {
					nested.add(child);
				}
			}
		}
		return nested;
	}

	public List<Path> createFolderStructure(Path schulfotosDir, String eventName, List<GalleryCode> codes)
			throws IOException {
		Path eventDir = schulfotosDir.resolve(eventName);
		Path expectedParent = schulfotosDir.toAbsolutePath().normalize();
		if (!expectedParent.equals(eventDir.toAbsolutePath().normalize().getParent())) {
			throw new IOException("Refusing to create '" + eventName + "': it would escape " + expectedParent);
		}

		List<Path> createdFolders = new ArrayList<>();

		Path klassenfotoDir = eventDir.resolve(klassenfotoFolder);
		Files.createDirectories(klassenfotoDir);
		createdFolders.add(klassenfotoDir);
		LOGGER.info("Created folder: {}", klassenfotoDir);

		for (int i = 1; i <= codes.size(); i++) {
			Path portraitDir = eventDir.resolve(portraitPrefix + i);
			Files.createDirectories(portraitDir);
			createdFolders.add(portraitDir);
			LOGGER.info("Created folder: {}", portraitDir);
		}

		LOGGER.info("Created {} folders under {}", createdFolders.size(), eventDir);
		return createdFolders;
	}

	private boolean hasPhotoSubfolders(Path dir) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory)) {
			for (Path sub : stream) {
				String name = sub.getFileName().toString();
				if (klassenfotoFolder.equals(name) || name.startsWith(portraitPrefix)) {
					return true;
				}
			}
		}
		return false;
	}

}
