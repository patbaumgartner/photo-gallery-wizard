package com.pabaumgartner.photogallery.wizard.service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.pabaumgartner.photogallery.wizard.model.CsvReadResult;
import com.pabaumgartner.photogallery.wizard.model.GalleryCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CsvReaderService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvReaderService.class);

	private static final String NUMBER_HEADER = "Number";

	private static final String CODE_HEADER = "Code";

	private static final String PASSWORD_HEADER = "Password";

	private static final String CLASS_NAME_HEADER = "Class Name";

	private static final String LEGACY_EVENT_NAME_HEADER = "Event Name";

	private static final String URL_HEADER = "URL";

	private static final String PIC_PEAK_EVENT_ID_HEADER = "PicPeak Event ID";

	private static final String BYTE_ORDER_MARK = "\uFEFF";

	public CsvReadResult readCodes(Path csvFile) throws IOException {
		if (!Files.exists(csvFile)) {
			throw new IOException("CSV file not found: " + csvFile.toAbsolutePath());
		}

		CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setTrim(true).get();

		LinkedHashSet<String> seenCodes = new LinkedHashSet<>();
		List<GalleryCode> codes = new ArrayList<>();
		String eventName = "";
		Columns columns = Columns.positional();
		boolean firstRecord = true;

		try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
				CSVParser parser = format.parse(reader)) {
			for (CSVRecord record : parser) {
				List<String> values = values(record);
				if (firstRecord) {
					firstRecord = false;
					if (Columns.isHeaderRow(values)) {
						columns = Columns.fromHeader(values);
						continue;
					}
				}

				String rawCode = columns.code(values);
				if (eventName.isEmpty()) {
					eventName = columns.eventName(values);
				}
				if (rawCode.isBlank()) {
					continue;
				}
				if (!GalleryCode.isValid(rawCode)) {
					LOGGER.warn("Skipping invalid gallery code at line {}: '{}'", record.getRecordNumber(), rawCode);
					continue;
				}
				if (!seenCodes.add(rawCode)) {
					LOGGER.warn("Skipping duplicate gallery code at line {}: '{}'", record.getRecordNumber(), rawCode);
					continue;
				}
				if (columns.isIncomplete(values)) {
					LOGGER.warn("Row at line {} has only {} columns; missing values are read as empty",
							record.getRecordNumber(), values.size());
				}

				codes.add(new GalleryCode(rawCode, columns.password(values), columns.url(values),
						columns.picPeakEventId(values, record.getRecordNumber())));
			}
		}

		LOGGER.atInfo().addArgument(() -> codes.size()).addArgument(csvFile).log("Read {} valid gallery codes from {}");
		return new CsvReadResult(eventName, codes);
	}

	private static List<String> values(CSVRecord record) {
		List<String> values = new ArrayList<>(record.size());
		for (int i = 0; i < record.size(); i++) {
			String value = record.get(i).trim();
			values.add(i == 0 && value.startsWith(BYTE_ORDER_MARK) ? value.substring(1) : value);
		}
		return values;
	}

	private record Columns(int code, int password, int eventName, int url, int picPeakEventId, int expectedSize) {

		private static Columns positional() {
			return new Columns(0, -1, -1, -1, -1, 1);
		}

		private static boolean isHeaderRow(List<String> values) {
			return values.contains(CODE_HEADER) || (!values.isEmpty() && NUMBER_HEADER.equals(values.getFirst()));
		}

		private static Columns fromHeader(List<String> header) {
			int nameIndex = header.indexOf(CLASS_NAME_HEADER);
			if (nameIndex < 0) {
				nameIndex = header.indexOf(LEGACY_EVENT_NAME_HEADER);
			}
			return new Columns(Math.max(header.indexOf(CODE_HEADER), 0), header.indexOf(PASSWORD_HEADER), nameIndex,
					header.indexOf(URL_HEADER), header.indexOf(PIC_PEAK_EVENT_ID_HEADER), header.size());
		}

		private boolean isIncomplete(List<String> values) {
			return values.size() < expectedSize;
		}

		private String code(List<String> values) {
			return valueAt(values, code);
		}

		private String password(List<String> values) {
			return valueAt(values, password);
		}

		private String eventName(List<String> values) {
			return valueAt(values, eventName);
		}

		private String url(List<String> values) {
			return valueAt(values, url);
		}

		private int picPeakEventId(List<String> values, long lineNumber) {
			String raw = valueAt(values, picPeakEventId);
			if (raw.isEmpty()) {
				return 0;
			}
			try {
				return Integer.parseInt(raw);
			}
			catch (NumberFormatException ex) {
				LOGGER.warn("Invalid PicPeak Event ID at line {}: '{}'", lineNumber, raw);
				return 0;
			}
		}

		private static String valueAt(List<String> values, int index) {
			return index >= 0 && index < values.size() ? values.get(index) : "";
		}

	}

}
