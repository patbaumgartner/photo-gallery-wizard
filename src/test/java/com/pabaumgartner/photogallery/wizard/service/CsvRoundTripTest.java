package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import com.pabaumgartner.photogallery.wizard.model.CsvReadResult;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class CsvRoundTripTest {

	@TempDir
	Path tempDir;

	@Test
	void writeAndReadBackPreservesAllFields() throws IOException {
		CsvWriterService writer = new CsvWriterService();
		CsvReaderService reader = new CsvReaderService();

		List<GalleryCode> original = List.of(new GalleryCode("ABCD-1234-WXYZ", "Pa$$w0rd!", "https://share.link", 42),
				new GalleryCode("EFGH-5678-STUV", "Secr3t&", "https://share.link/2", 99));

		Path csv = tempDir.resolve("roundtrip.csv");
		writer.writeCodes(original, csv, "My Event", "https://gallery.com/?code=");

		CsvReadResult result = reader.readCodes(csv);
		assertThat(result.eventName()).isEqualTo("My Event");
		assertThat(result.codes()).hasSize(2);

		GalleryCode first = result.codes().get(0);
		assertThat(first.code()).isEqualTo("ABCD-1234-WXYZ");
		assertThat(first.password()).isEqualTo("Pa$$w0rd!");
		assertThat(first.shareUrl()).isEqualTo("https://share.link");
		assertThat(first.picPeakEventId()).isEqualTo(42);

		GalleryCode second = result.codes().get(1);
		assertThat(second.code()).isEqualTo("EFGH-5678-STUV");
		assertThat(second.password()).isEqualTo("Secr3t&");
		assertThat(second.shareUrl()).isEqualTo("https://share.link/2");
		assertThat(second.picPeakEventId()).isEqualTo(99);
	}

	@Test
	void roundTripWithGeneratedCodes() throws IOException {
		SchulfotosProperties schulfotosProperties = new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null,
				null, null, null, null, null, null, 0);
		CodeGeneratorService codeGen = new CodeGeneratorService(schulfotosProperties);
		CsvWriterService writer = new CsvWriterService();
		CsvReaderService reader = new CsvReaderService();

		List<GalleryCode> generated = codeGen.generateCodes("TEST", 17);

		Path csv = tempDir.resolve("generated-roundtrip.csv");
		writer.writeCodes(generated, csv, "Klasse 5a", "https://gallery.com/?code=");

		CsvReadResult result = reader.readCodes(csv);
		assertThat(result.eventName()).isEqualTo("Klasse 5a");
		assertThat(result.codes()).hasSize(17);

		for (int i = 0; i < generated.size(); i++) {
			assertThat(result.codes().get(i).code()).isEqualTo(generated.get(i).code());
			assertThat(result.codes().get(i).password()).isEqualTo(generated.get(i).password());
		}
	}

	@Test
	void roundTripWithEmptyPasswords() throws IOException {
		CsvWriterService writer = new CsvWriterService();
		CsvReaderService reader = new CsvReaderService();

		List<GalleryCode> codes = List.of(new GalleryCode("ABCD-1234-WXYZ", "", "", 0));

		Path csv = tempDir.resolve("empty-pw.csv");
		writer.writeCodes(codes, csv, "", "https://gallery.com/?code=");

		CsvReadResult result = reader.readCodes(csv);
		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().password()).isEmpty();
	}

}
