package com.pabaumgartner.photogallery.wizard.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageProcessingServiceTest {

	@TempDir
	Path tempDir;

	private ImageProcessingService service;

	private static AppProperties defaultAppProperties() {
		return new AppProperties("", "", null, 0, 0f, 0f, 0f, 0, 0);
	}

	private static SchulfotosProperties defaultSchulfotosProperties() {
		return new SchulfotosProperties(null, null, 0, 0, 0, 0, false, null, null, null, null, null, null, null, null,
				0);
	}

	@BeforeEach
	void setUp() {
		service = new ImageProcessingService(defaultAppProperties(), defaultSchulfotosProperties());
	}

	private Path createTestImage(Path dir, String name, int width, int height) throws IOException {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		Files.createDirectories(dir);
		Path file = dir.resolve(name);
		ImageIO.write(img, "jpg", file.toFile());
		return file;
	}

	private Path createWatermark(int width, int height) throws IOException {
		BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = watermark.createGraphics();
		g.setColor(new Color(255, 255, 255, 100));
		g.fillRect(0, 0, width, height);
		g.dispose();
		Path wmPath = tempDir.resolve("watermark.png");
		ImageIO.write(watermark, "png", wmPath.toFile());
		return wmPath;
	}

	@Test
	void processEventFoldersResizesAndWatermarksKlassenfotos() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("klassenfoto"), "class1.jpg", 2000, 1500);
		Path wm = createWatermark(400, 200);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(1);
		assertThat(result.outputFolders()).hasSize(1);
		Path watermarkedDir = eventDir.resolve("klassenfotos-watermarked");
		assertThat(Files.isDirectory(watermarkedDir)).isTrue();
		assertThat(Files.list(watermarkedDir).count()).isEqualTo(1);

		BufferedImage output = ImageIO.read(watermarkedDir.resolve("class1.jpg").toFile());
		assertThat(Math.max(output.getWidth(), output.getHeight())).isLessThanOrEqualTo(1200);
	}

	@Test
	void processEventFoldersResizesAndWatermarksPortraits() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("portrait-1"), "p1.jpg", 3000, 2000);
		createTestImage(eventDir.resolve("portrait-2"), "p2.jpg", 1800, 2400);
		Path wm = createWatermark(400, 200);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(2);
		assertThat(result.outputFolders()).hasSize(2);

		assertThat(Files.isDirectory(eventDir.resolve("portrait-1-watermarked"))).isTrue();
		assertThat(Files.isDirectory(eventDir.resolve("portrait-2-watermarked"))).isTrue();
	}

	@Test
	void processEventFoldersDoesNotResizeSmallImages() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("klassenfoto"), "small.jpg", 800, 600);
		Path wm = createWatermark(200, 100);

		service.processEventFolders(eventDir, wm, 1200);

		Path output = eventDir.resolve("klassenfotos-watermarked/small.jpg");
		BufferedImage img = ImageIO.read(output.toFile());
		assertThat(img.getWidth()).isEqualTo(800);
		assertThat(img.getHeight()).isEqualTo(600);
	}

	@Test
	void processEventFoldersSkipsAlreadyWatermarkedFolders() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("portrait-1"), "p1.jpg", 2000, 1500);
		createTestImage(eventDir.resolve("portrait-1-watermarked"), "existing.jpg", 1200, 900);
		Path wm = createWatermark(400, 200);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(1);
	}

	@Test
	void processEventFoldersEmptyDirectoryReturnsZero() throws IOException {
		Path eventDir = tempDir.resolve("empty-event");
		Files.createDirectories(eventDir);
		Path wm = createWatermark(200, 100);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(0);
		assertThat(result.outputFolders()).isEmpty();
	}

	@Test
	void watermarkNotFoundThrowsIOException() {
		Path eventDir = tempDir.resolve("event");
		Path missingWm = tempDir.resolve("missing-watermark.png");

		assertThatThrownBy(() -> service.processEventFolders(eventDir, missingWm, 1200)).isInstanceOf(IOException.class)
			.hasMessageContaining("Watermark image not found");
	}

	@Test
	void processEventFoldersIgnoresPngFiles() throws IOException {
		Path eventDir = tempDir.resolve("event");
		Path dir = eventDir.resolve("klassenfoto");
		Files.createDirectories(dir);

		BufferedImage pngImg = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = pngImg.createGraphics();
		g.setColor(Color.RED);
		g.fillRect(0, 0, 2000, 1500);
		g.dispose();
		ImageIO.write(pngImg, "png", dir.resolve("test.png").toFile());

		Path wm = createWatermark(400, 200);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(1);
	}

	@Test
	void processEventFoldersOutputIsJpeg() throws IOException {
		Path eventDir = tempDir.resolve("event");
		Path dir = eventDir.resolve("klassenfoto");
		Files.createDirectories(dir);

		BufferedImage pngImg = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		ImageIO.write(pngImg, "png", dir.resolve("test.png").toFile());

		Path wm = createWatermark(200, 100);
		service.processEventFolders(eventDir, wm, 1200);

		Path outputDir = eventDir.resolve("klassenfotos-watermarked");
		assertThat(Files.list(outputDir).findFirst().get().getFileName().toString()).endsWith(".jpg");
	}

	@Test
	void processEventFoldersReportsProgress() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("klassenfoto"), "c1.jpg", 2000, 1500);
		Path wm = createWatermark(400, 200);

		java.util.List<ImageProcessingService.ProcessingProgress> progress = new java.util.ArrayList<>();
		service.processEventFolders(eventDir, wm, 1200, progress::add);

		assertThat(progress).isNotEmpty();
		assertThat(progress.getFirst().percent()).isGreaterThan(0.0);
		assertThat(progress.getLast().percent()).isEqualTo(1.0);
	}

	@Test
	void processEventFoldersHandlesBothKlassenfotoAndPortraits() throws IOException {
		Path eventDir = tempDir.resolve("event");
		createTestImage(eventDir.resolve("klassenfoto"), "c1.jpg", 2000, 1500);
		createTestImage(eventDir.resolve("portrait-1"), "p1.jpg", 2000, 1500);
		createTestImage(eventDir.resolve("portrait-2"), "p2.jpg", 2000, 1500);
		Path wm = createWatermark(400, 200);

		ImageProcessingService.ImageProcessingResult result = service.processEventFolders(eventDir, wm, 1200);

		assertThat(result.totalProcessed()).isEqualTo(3);
		assertThat(result.outputFolders()).hasSize(3);
	}

}
