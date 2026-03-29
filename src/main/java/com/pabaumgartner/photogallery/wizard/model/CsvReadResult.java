package com.pabaumgartner.photogallery.wizard.model;

import java.util.List;

public record CsvReadResult(String eventName, List<GalleryCode> codes) {

	public CsvReadResult {
		if (eventName == null) {
			eventName = "";
		}
		if (codes == null) {
			codes = List.of();
		}
	}

}
