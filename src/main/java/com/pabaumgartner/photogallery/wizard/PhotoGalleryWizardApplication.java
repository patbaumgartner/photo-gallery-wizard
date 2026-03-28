package com.pabaumgartner.photogallery.wizard;

import com.pabaumgartner.photogallery.wizard.tui.PhotoGalleryWizardTui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhotoGalleryWizardApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(PhotoGalleryWizardApplication.class, args);
		context.getBean(PhotoGalleryWizardTui.class).run();
	}

}
