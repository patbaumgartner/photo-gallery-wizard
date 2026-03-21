package com.pabaumgartner.photogallery.wizard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhotoGalleryWizardApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotoGalleryWizardApplication.class, args);
	}

}
