package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pabaumgartner.photogallery.wizard.model.CsvReadResult;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvReaderServiceTest {

	@TempDir
	Path tempDir;

	private CsvReaderService service;

	@BeforeEach
	void setUp() {
		service = new CsvReaderService();
	}

	@Test
	void readCodesWithFullHeaderedCsv() throws IOException {
		Path csv = tempDir.resolve("test.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,secret,Klasse 1a,https://share.link/1,42
				2,EFGH-5678-STUV,pass2,Klasse 1a,https://share.link/2,43
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.eventName()).isEqualTo("Klasse 1a");
		assertThat(result.codes()).hasSize(2);

		GalleryCode first = result.codes().getFirst();
		assertThat(first.code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(first.password()).isEqualTo("secret");
		assertThat(first.shareUrl()).isEqualTo("https://share.link/1");
		assertThat(first.picPeakEventId()).isEqualTo(42);

		GalleryCode second = result.codes().get(1);
		assertThat(second.code()).isEqualTo("EFGH-5678-STUV");
		assertThat(second.password()).isEqualTo("pass2");
		assertThat(second.picPeakEventId()).isEqualTo(43);
	}

	@Test
	void readCodesWithBomPrefix() throws IOException {
		Path csv = tempDir.resolve("bom.csv");
		String content = "\uFEFFNumber,Code,Password,Class Name,URL,PicPeak Event ID\n"
				+ "1,ABCD-1234-WXYZ,secret,Test,,\n";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().code()).isEqualTo("ABCD-1234-WXYZ");
	}

	@Test
	void readCodesSupportsLegacyEventNameHeader() throws IOException {
		Path csv = tempDir.resolve("legacy-header.csv");
		String content = """
				Number,Code,Password,Event Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,secret,Klasse 1a,https://share.link/1,42
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.eventName()).isEqualTo("Klasse 1a");
		assertThat(result.codes()).hasSize(1);
	}

	@Test
	void readCodesWithoutHeader() throws IOException {
		Path csv = tempDir.resolve("noheader.csv");
		String content = "ABCD-1234-WXYZ\nEFGH-5678-STUV\n";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(2);
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void readCodesSkipsInvalidCodes() throws IOException {
		Path csv = tempDir.resolve("invalid.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,pw,Test,,
				2,INVALID-CODE,pw,Test,,
				3,EFGH-5678-STUV,pw,Test,,
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes().get(0).code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(result.codes().get(1).code()).isEqualTo("EFGH-5678-STUV");
	}

	@Test
	void readCodesSkipsDuplicateCodes() throws IOException {
		Path csv = tempDir.resolve("duplicates.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,pw1,Test,,
				2,ABCD-1234-WXYZ,pw2,Test,,
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().password()).isEqualTo("pw1");
	}

	@Test
	void readCodesSkipsBlankLines() throws IOException {
		Path csv = tempDir.resolve("blanks.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,pw,Test,,

				2,EFGH-5678-STUV,pw,Test,,
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(2);
	}

	@Test
	void readCodesSkipsBlankCodeValues() throws IOException {
		Path csv = tempDir.resolve("blankcode.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,,pw,Test,,
				2,ABCD-1234-WXYZ,pw,Test,,
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
	}

	@Test
	void readCodesFileNotFoundThrowsIOException() {
		Path csv = tempDir.resolve("nonexistent.csv");
		assertThatThrownBy(() -> service.readCodes(csv)).isInstanceOf(IOException.class)
			.hasMessageContaining("not found");
	}

	@Test
	void readCodesEmptyFileReturnsEmptyList() throws IOException {
		Path csv = tempDir.resolve("empty.csv");
		Files.writeString(csv, "", StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).isEmpty();
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void readCodesHeaderOnlyReturnsEmpty() throws IOException {
		Path csv = tempDir.resolve("headeronly.csv");
		String content = "Number,Code,Password,Class Name,URL,PicPeak Event ID\n";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).isEmpty();
	}

	@Test
	void readCodesWithOnlyCodeAndPasswordColumns() throws IOException {
		Path csv = tempDir.resolve("minimal.csv");
		String content = """
				Number,Code,Password
				1,ABCD-1234-WXYZ,secret
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		GalleryCode code = result.codes().getFirst();
		assertThat(code.code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(code.password()).isEqualTo("secret");
		assertThat(code.shareUrl()).isEmpty();
		assertThat(code.picPeakEventId()).isEqualTo(0);
	}

	@Test
	void readCodesHandlesInvalidPicPeakEventId() throws IOException {
		Path csv = tempDir.resolve("badid.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,ABCD-1234-WXYZ,pw,Test,,not-a-number
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().picPeakEventId()).isEqualTo(0);
	}

	@Test
	void readCodesTrimsWhitespace() throws IOException {
		Path csv = tempDir.resolve("whitespace.csv");
		String content = """
				Number,Code,Password,Class Name,URL,PicPeak Event ID
				1,  ABCD-1234-WXYZ  ,  secret  ,  Test  ,,
				""";
		Files.writeString(csv, content, StandardCharsets.UTF_8);

		CsvReadResult result = service.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(result.codes().getFirst().password()).isEqualTo("secret");
		assertThat(result.eventName()).isEqualTo("Test");
	}

}
