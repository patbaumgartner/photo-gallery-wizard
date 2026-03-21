package com.pabaumgartner.photogallery.wizard.model;

import java.util.List;

public record CsvReadResult(List<GalleryCode> codes, String eventName) {
}
