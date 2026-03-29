package com.pabaumgartner.photogallery.wizard;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.pabaumgartner.photogallery.wizard.config.NativeImageRuntimeHints;
import com.pabaumgartner.photogallery.wizard.tui.PhotoGalleryWizardTui;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportRuntimeHints(NativeImageRuntimeHints.class)
public class PhotoGalleryWizardApplication {

	private static final String CONSOLE_APPENDER_NAME = "CONSOLE";

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(PhotoGalleryWizardApplication.class, args);
		Appender<ILoggingEvent> consoleAppender = detachConsoleAppender();
		try {
			context.getBean(PhotoGalleryWizardTui.class).run();
		}
		finally {
			restoreConsoleAppender(consoleAppender);
		}
	}

	private static Appender<ILoggingEvent> detachConsoleAppender() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		Appender<ILoggingEvent> consoleAppender = rootLogger.getAppender(CONSOLE_APPENDER_NAME);
		if (consoleAppender != null) {
			rootLogger.detachAppender(consoleAppender);
		}
		return consoleAppender;
	}

	private static void restoreConsoleAppender(Appender<ILoggingEvent> consoleAppender) {
		if (consoleAppender == null) {
			return;
		}
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (rootLogger.getAppender(CONSOLE_APPENDER_NAME) == null) {
			rootLogger.addAppender(consoleAppender);
		}
	}

}
