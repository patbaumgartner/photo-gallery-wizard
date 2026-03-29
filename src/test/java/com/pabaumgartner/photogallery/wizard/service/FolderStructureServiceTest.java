package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FolderStructureServiceTest {

	@TempDir
	Path tempDir;

	private FolderStructureService service;

	private static SchulfotosProperties defaultSchulfotosProperties() {
		return new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null, null,
				0);
	}

	@BeforeEach
	void setUp() {
		service = new FolderStructureService(defaultSchulfotosProperties());
	}

	@Test
	void listCsvFilesFindsAllCsvs() throws IOException {
		Files.writeString(tempDir.resolve("a.csv"), "data");
		Files.writeString(tempDir.resolve("b.csv"), "data");
		Files.writeString(tempDir.resolve("c.txt"), "data");

		List<Path> csvFiles = service.listCsvFiles(tempDir);
		assertThat(csvFiles).hasSize(2);
		assertThat(csvFiles).extracting(p -> p.getFileName().toString()).containsExactly("a.csv", "b.csv");
	}

	@Test
	void listCsvFilesReturnsEmptyForNonexistentDir() throws IOException {
		List<Path> csvFiles = service.listCsvFiles(tempDir.resolve("nonexistent"));
		assertThat(csvFiles).isEmpty();
	}

	@Test
	void listCsvFilesSortedAlphabetically() throws IOException {
		Files.writeString(tempDir.resolve("z.csv"), "data");
		Files.writeString(tempDir.resolve("a.csv"), "data");
		Files.writeString(tempDir.resolve("m.csv"), "data");

		List<Path> csvFiles = service.listCsvFiles(tempDir);
		assertThat(csvFiles).extracting(p -> p.getFileName().toString()).containsExactly("a.csv", "m.csv", "z.csv");
	}

	@Test
	void listCsvFilesEmptyDirectory() throws IOException {
		List<Path> csvFiles = service.listCsvFiles(tempDir);
		assertThat(csvFiles).isEmpty();
	}

	@Test
	void listEventFoldersFindsDirectoriesWithPhotoSubfolders() throws IOException {
		Path event1 = tempDir.resolve("event1");
		Files.createDirectories(event1.resolve("klassenfoto"));

		Path event2 = tempDir.resolve("event2");
		Files.createDirectories(event2.resolve("portrait-1"));

		Path notEvent = tempDir.resolve("notanevent");
		Files.createDirectories(notEvent.resolve("random-subfolder"));

		List<Path> folders = service.listEventFolders(tempDir);
		assertThat(folders).hasSize(2);
		assertThat(folders).extracting(p -> p.getFileName().toString()).containsExactly("event1", "event2");
	}

	@Test
	void listEventFoldersReturnsEmptyForNonexistentDir() throws IOException {
		List<Path> folders = service.listEventFolders(tempDir.resolve("nonexistent"));
		assertThat(folders).isEmpty();
	}

	@Test
	void listEventFoldersIgnoresFiles() throws IOException {
		Files.writeString(tempDir.resolve("somefile.txt"), "data");
		Path event = tempDir.resolve("event");
		Files.createDirectories(event.resolve("klassenfoto"));

		List<Path> folders = service.listEventFolders(tempDir);
		assertThat(folders).hasSize(1);
	}

	@Test
	void listEventFoldersSortedAlphabetically() throws IOException {
		Path z = tempDir.resolve("zclass");
		Files.createDirectories(z.resolve("klassenfoto"));
		Path a = tempDir.resolve("aclass");
		Files.createDirectories(a.resolve("portrait-1"));

		List<Path> folders = service.listEventFolders(tempDir);
		assertThat(folders).extracting(p -> p.getFileName().toString()).containsExactly("aclass", "zclass");
	}

	@Test
	void createFolderStructureCreatesKlassenfotoAndPortraitDirs() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"), new GalleryCode("EFGH-5678-STUV"),
				new GalleryCode("IJKL-9012-MNOP"));

		List<Path> created = service.createFolderStructure(tempDir, "Klasse 1a", codes);

		assertThat(created).hasSize(4);
		assertThat(Files.isDirectory(tempDir.resolve("Klasse 1a/klassenfoto"))).isTrue();
		assertThat(Files.isDirectory(tempDir.resolve("Klasse 1a/portrait-1"))).isTrue();
		assertThat(Files.isDirectory(tempDir.resolve("Klasse 1a/portrait-2"))).isTrue();
		assertThat(Files.isDirectory(tempDir.resolve("Klasse 1a/portrait-3"))).isTrue();
	}

	@Test
	void createFolderStructureWithSingleCode() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"));

		List<Path> created = service.createFolderStructure(tempDir, "Solo", codes);

		assertThat(created).hasSize(2);
		assertThat(Files.isDirectory(tempDir.resolve("Solo/klassenfoto"))).isTrue();
		assertThat(Files.isDirectory(tempDir.resolve("Solo/portrait-1"))).isTrue();
	}

	@Test
	void createFolderStructureIdempotent() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"));

		service.createFolderStructure(tempDir, "Idempotent", codes);
		List<Path> created = service.createFolderStructure(tempDir, "Idempotent", codes);

		assertThat(created).hasSize(2);
		assertThat(Files.isDirectory(tempDir.resolve("Idempotent/klassenfoto"))).isTrue();
	}

	@Test
	void createFolderStructureWithEmptyCodeList() throws IOException {
		List<Path> created = service.createFolderStructure(tempDir, "EmptyEvent", List.of());

		assertThat(created).hasSize(1);
		assertThat(Files.isDirectory(tempDir.resolve("EmptyEvent/klassenfoto"))).isTrue();
	}

	@Test
	void createFolderStructureWith17Codes() throws IOException {
		CodeGeneratorService codeGen = new CodeGeneratorService(defaultSchulfotosProperties());
		List<GalleryCode> codes = codeGen.generateCodes("ABCD", 17);

		List<Path> created = service.createFolderStructure(tempDir, "LargeClass", codes);

		assertThat(created).hasSize(18);
		assertThat(Files.isDirectory(tempDir.resolve("LargeClass/klassenfoto"))).isTrue();
		for (int i = 1; i <= 17; i++) {
			assertThat(Files.isDirectory(tempDir.resolve("LargeClass/portrait-" + i))).isTrue();
		}
	}

	@Test
	void listEventFoldersDetectsCreatedStructure() throws IOException {
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ"));

		service.createFolderStructure(tempDir, "DetectedEvent", codes);

		List<Path> folders = service.listEventFolders(tempDir);
		assertThat(folders).hasSize(1);
		assertThat(folders.getFirst().getFileName().toString()).isEqualTo("DetectedEvent");
	}

	@Test
	void listEventFoldersFindsNestedPhotoStructureFromSeparatorSplit() throws IOException {
		Path nestedEvent = tempDir.resolve("SplitParent").resolve("SplitEvent");
		Files.createDirectories(nestedEvent.resolve("klassenfoto"));

		List<Path> folders = service.listEventFolders(tempDir);

		assertThat(folders).extracting(p -> p.getFileName().toString()).contains("SplitEvent");
	}

}
