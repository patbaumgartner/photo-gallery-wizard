package com.pabaumgartner.photogallery.wizard.service;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.pabaumgartner.photogallery.wizard.config.AppProperties;
import com.pabaumgartner.photogallery.wizard.config.SchulfotosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageProcessingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageProcessingService.class);

	private final float watermarkOpacity;

	private final float watermarkScale;

	private final float jpegQuality;

	private final String klassenfotoFolder;

	private final String portraitPrefix;

	private final String watermarkedSuffix;

	public ImageProcessingService(AppProperties appProperties, SchulfotosProperties schulfotosProperties) {
		this.watermarkOpacity = appProperties.watermarkOpacity();
		this.watermarkScale = appProperties.watermarkScale();
		this.jpegQuality = appProperties.jpegQuality();
		this.klassenfotoFolder = schulfotosProperties.klassenfotoFolder();
		this.portraitPrefix = schulfotosProperties.portraitPrefix();
		this.watermarkedSuffix = schulfotosProperties.watermarkedSuffix();
	}

	public record ProcessingProgress(double percent, String stage) {
	}

	public ImageProcessingResult processEventFolders(Path eventDir, Path watermarkImagePath, int maxEdge)
			throws IOException {
		return processEventFolders(eventDir, watermarkImagePath, maxEdge, progress -> {
		});
	}

	public ImageProcessingResult processEventFolders(Path eventDir, Path watermarkImagePath, int maxEdge,
			Consumer<ProcessingProgress> progressListener) throws IOException {
		progressListener.accept(new ProcessingProgress(0.05d, "Wasserzeichen laden"));
		BufferedImage watermark = loadWatermark(watermarkImagePath);
		int totalProcessed = 0;
		List<Path> outputFolders = new ArrayList<>();

		List<Path> inputImages = collectInputImages(eventDir);
		int totalImages = Math.max(inputImages.size(), 1);
		int processedSoFar = 0;

		progressListener.accept(new ProcessingProgress(0.10d, "Fotoordner analysieren"));

		Path klassenfotoDir = eventDir.resolve(klassenfotoFolder);
		if (Files.isDirectory(klassenfotoDir)) {
			Path outputDir = eventDir.resolve(klassenfotoFolder + "s" + watermarkedSuffix);
			int count = resizeAndWatermarkFolder(klassenfotoDir, outputDir, watermark, maxEdge, (done, file) -> {
				double ratio = (double) done / totalImages;
				progressListener.accept(new ProcessingProgress(0.10d + 0.85d * ratio, "Bilder werden verarbeitet"));
			}, processedSoFar);
			totalProcessed += count;
			processedSoFar += count;
			if (count > 0) {
				outputFolders.add(outputDir);
			}
			LOGGER.info("Processed {} class photos from {}", count, klassenfotoDir);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(eventDir, Files::isDirectory)) {
			for (Path sub : stream) {
				String name = sub.getFileName().toString();
				if (name.startsWith(portraitPrefix) && !name.endsWith(watermarkedSuffix)) {
					Path outputDir = eventDir.resolve(name + watermarkedSuffix);
					int count = resizeAndWatermarkFolder(sub, outputDir, watermark, maxEdge, (done, file) -> {
						double ratio = (double) done / totalImages;
						progressListener
							.accept(new ProcessingProgress(0.10d + 0.85d * ratio, "Bilder werden verarbeitet"));
					}, processedSoFar);
					totalProcessed += count;
					processedSoFar += count;
					if (count > 0) {
						outputFolders.add(outputDir);
					}
					LOGGER.info("Processed {} portrait photos from {}", count, sub);
				}
			}
		}

		LOGGER.info("Total: processed {} images under {}", totalProcessed, eventDir);
		progressListener.accept(new ProcessingProgress(1.0d, "Wasserzeichen fertig"));
		return new ImageProcessingResult(totalProcessed, outputFolders);
	}

	private int resizeAndWatermarkFolder(Path sourceDir, Path outputDir, BufferedImage watermark, int maxEdge,
			ProgressImageListener listener, int alreadyProcessed) throws IOException {
		if (!Files.isDirectory(sourceDir)) {
			return 0;
		}
		Files.createDirectories(outputDir);
		int count = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, this::isImageFile)) {
			for (Path imageFile : stream) {
				BufferedImage original = ImageIO.read(imageFile.toFile());
				if (original == null) {
					LOGGER.warn("Could not read image: {}", imageFile);
					continue;
				}
				BufferedImage resized = resize(original, maxEdge);
				BufferedImage result = applyWatermark(resized, watermark);

				String outputName = changeExtension(imageFile.getFileName().toString(), "jpg");
				Path outputFile = outputDir.resolve(outputName);
				writeJpeg(result, outputFile);
				count++;
				listener.onImageDone(alreadyProcessed + count, imageFile.getFileName().toString());
			}
		}
		return count;
	}

	private List<Path> collectInputImages(Path eventDir) throws IOException {
		List<Path> images = new ArrayList<>();
		Path klassenfotoDir = eventDir.resolve(klassenfotoFolder);
		if (Files.isDirectory(klassenfotoDir)) {
			images.addAll(listImageFiles(klassenfotoDir));
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(eventDir, Files::isDirectory)) {
			for (Path sub : stream) {
				String name = sub.getFileName().toString();
				if (name.startsWith(portraitPrefix) && !name.endsWith(watermarkedSuffix)) {
					images.addAll(listImageFiles(sub));
				}
			}
		}
		return images;
	}

	private List<Path> listImageFiles(Path sourceDir) throws IOException {
		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, this::isImageFile)) {
			for (Path file : stream) {
				files.add(file);
			}
		}
		return files;
	}

	@FunctionalInterface
	private interface ProgressImageListener {

		void onImageDone(int done, String file);

	}

	private BufferedImage resize(BufferedImage source, int maxEdge) {
		int width = source.getWidth();
		int height = source.getHeight();

		if (width <= maxEdge && height <= maxEdge) {
			return source;
		}

		double scale = (double) maxEdge / Math.max(width, height);
		int newWidth = (int) Math.round(width * scale);
		int newHeight = (int) Math.round(height * scale);

		BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = resized.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.drawImage(source, 0, 0, newWidth, newHeight, null);
		g2d.dispose();
		return resized;
	}

	private BufferedImage applyWatermark(BufferedImage image, BufferedImage watermark) {
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		int wmTargetWidth = (int) (imageWidth * watermarkScale);
		double wmScale = (double) wmTargetWidth / watermark.getWidth();
		int wmTargetHeight = (int) (watermark.getHeight() * wmScale);

		int wmX = (imageWidth - wmTargetWidth) / 2;
		int wmY = (imageHeight - wmTargetHeight) / 2;

		BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, watermarkOpacity));
		g2d.drawImage(watermark, wmX, wmY, wmTargetWidth, wmTargetHeight, null);
		g2d.dispose();
		return result;
	}

	private BufferedImage loadWatermark(Path watermarkPath) throws IOException {
		if (Files.exists(watermarkPath)) {
			BufferedImage img = ImageIO.read(watermarkPath.toFile());
			if (img != null) {
				return img;
			}
		}
		var classPathStream = getClass().getClassLoader().getResourceAsStream(watermarkPath.toString());
		if (classPathStream != null) {
			try (classPathStream) {
				BufferedImage img = ImageIO.read(classPathStream);
				if (img != null) {
					return img;
				}
			}
		}
		throw new IOException("Watermark image not found: " + watermarkPath);
	}

	private void writeJpeg(BufferedImage image, Path outputFile) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG writer available");
		}
		ImageWriter writer = writers.next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(jpegQuality);

		try (OutputStream os = Files.newOutputStream(outputFile);
				ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), param);
		}
		finally {
			writer.dispose();
		}
	}

	private boolean isImageFile(Path path) {
		String name = path.getFileName().toString().toLowerCase();
		return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
	}

	private String changeExtension(String filename, String ext) {
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			return filename.substring(0, dotIndex) + "." + ext;
		}
		return filename + "." + ext;
	}

	public record ImageProcessingResult(int totalProcessed, List<Path> outputFolders) {
	}

}
