package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterServiceTest {

	@TempDir
	Path tempDir;

	private CsvWriterService service;

	@BeforeEach
	void setUp() {
		service = new CsvWriterService();
	}

	@Test
	void writeCodesCreatesFile() throws IOException {
		Path output = tempDir.resolve("output.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "secret", "https://share.link", 42));

		service.writeCodes(codes, output, "Event", "https://gallery.com/?code=");

		assertThat(output).exists();
		String content = Files.readString(output, StandardCharsets.UTF_8);
		assertThat(content).contains("Number,Code,Password,Event Name,URL,PicPeak Event ID");
		assertThat(content).contains("ABCD-1234-WXYZ");
		assertThat(content).contains("secret");
		assertThat(content).contains("Event");
		assertThat(content).contains("https://share.link");
		assertThat(content).contains("42");
	}

	@Test
	void writeCodesCreatesParentDirectories() throws IOException {
		Path output = tempDir.resolve("sub/dir/output.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));

		service.writeCodes(codes, output, "Test", "https://gallery.com/?code=");

		assertThat(output).exists();
	}

	@Test
	void writeCodesWithMultipleCodes() throws IOException {
		Path output = tempDir.resolve("multi.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw1"),
				new GalleryCode("EFGH-5678-STUV", "pw2"), new GalleryCode("IJKL-9012-MNOP", "pw3"));

		service.writeCodes(codes, output, "Multi", "https://gallery.com/?code=");

		List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(4);
		assertThat(lines.get(1)).startsWith("1,ABCD-1234-WXYZ");
		assertThat(lines.get(2)).startsWith("2,EFGH-5678-STUV");
		assertThat(lines.get(3)).startsWith("3,IJKL-9012-MNOP");
	}

	@Test
	void writeCodesEmptyListWritesHeaderOnly() throws IOException {
		Path output = tempDir.resolve("empty.csv");
		service.writeCodes(List.of(), output, "Empty", "https://gallery.com/?code=");

		List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(1);
		assertThat(lines.getFirst()).contains("Number,Code,Password");
	}

	@Test
	void writeCodesUsesGalleryUrlWhenNoShareUrl() throws IOException {
		Path output = tempDir.resolve("nourl.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw"));

		service.writeCodes(codes, output, "Test", "https://gallery.com/?code=");

		String content = Files.readString(output, StandardCharsets.UTF_8);
		assertThat(content).contains("https://gallery.com/?code=ABCD-1234-WXYZ");
	}

	@Test
	void writeCodesOmitsPicPeakEventIdWhenZero() throws IOException {
		Path output = tempDir.resolve("noid.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw", "", 0));

		service.writeCodes(codes, output, "Test", "https://gallery.com/?code=");

		String content = Files.readString(output, StandardCharsets.UTF_8);
		assertThat(content).doesNotContain(",0");
	}

	@Test
	void writeCodesIncludesPicPeakEventIdWhenPositive() throws IOException {
		Path output = tempDir.resolve("withid.csv");
		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "pw", "url", 99));

		service.writeCodes(codes, output, "Test", "https://gallery.com/?code=");

		String content = Files.readString(output, StandardCharsets.UTF_8);
		assertThat(content).contains("99");
	}

}
