package com.pabaumgartner.photogallery.wizard.service;

import java.io.BufferedReader;
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

	private static final String LEGACY_EVENT_NAME_HEADER = "Event Name";

	private static final String CLASS_NAME_HEADER = "Class Name";

	public CsvReadResult readCodes(Path csvFile) throws IOException {
		if (!Files.exists(csvFile)) {
			throw new IOException("CSV file not found: " + csvFile.toAbsolutePath());
		}

		LinkedHashSet<String> seenCodes = new LinkedHashSet<>();
		List<GalleryCode> codes = new ArrayList<>();
		String eventName = "";
		boolean hasHeader = hasHeaderRow(csvFile);

		CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setTrim(true);
		if (hasHeader) {
			formatBuilder.setSkipHeaderRecord(true).setHeader();
		}
		CSVFormat format = formatBuilder.get();

		try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
				CSVParser parser = format.parse(reader)) {

			boolean hasCodeColumn = hasHeader && parser.getHeaderNames().contains("Code");
			String nameHeader = resolveNameHeader(hasHeader, parser);
			boolean hasPasswordColumn = hasHeader && parser.getHeaderNames().contains("Password");
			boolean hasUrlColumn = hasHeader && parser.getHeaderNames().contains("URL");
			boolean hasPicPeakEventIdColumn = hasHeader && parser.getHeaderNames().contains("PicPeak Event ID");

			for (CSVRecord record : parser) {
				if (record.size() == 0) {
					continue;
				}

				String rawCode = hasCodeColumn ? record.get("Code").trim() : record.get(0).trim();
				if (nameHeader != null && eventName.isEmpty()) {
					eventName = record.get(nameHeader).trim();
				}
				if (rawCode.startsWith("\uFEFF")) {
					rawCode = rawCode.substring(1);
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

				String password = hasPasswordColumn ? record.get("Password").trim() : "";
				String shareUrl = hasUrlColumn ? record.get("URL").trim() : "";
				int picPeakEventId = 0;
				if (hasPicPeakEventIdColumn) {
					String idStr = record.get("PicPeak Event ID").trim();
					if (!idStr.isEmpty()) {
						try {
							picPeakEventId = Integer.parseInt(idStr);
						}
						catch (NumberFormatException ex) {
							LOGGER.debug("Invalid PicPeak Event ID at line {}: '{}'", record.getRecordNumber(), idStr);
						}
					}
				}
				codes.add(new GalleryCode(rawCode, password, shareUrl, picPeakEventId));
			}
		}

		LOGGER.atInfo().addArgument(() -> codes.size()).addArgument(csvFile).log("Read {} valid gallery codes from {}");
		return new CsvReadResult(eventName, codes);
	}

	private String resolveNameHeader(boolean hasHeader, CSVParser parser) {
		if (!hasHeader) {
			return null;
		}
		if (parser.getHeaderNames().contains(CLASS_NAME_HEADER)) {
			return CLASS_NAME_HEADER;
		}
		if (parser.getHeaderNames().contains(LEGACY_EVENT_NAME_HEADER)) {
			return LEGACY_EVENT_NAME_HEADER;
		}
		return null;
	}

	private boolean hasHeaderRow(Path csvFile) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
			String firstLine = br.readLine();
			if (firstLine == null) {
				return false;
			}
			if (firstLine.startsWith("\uFEFF")) {
				firstLine = firstLine.substring(1);
			}
			return firstLine.startsWith("Number,");
		}
	}

}
