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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.pabaumgartner.photogallery.wizard.config.ImageProperties;
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

	private final String filenameStripPostfix;

	public ImageProcessingService(ImageProperties imageProperties, SchulfotosProperties schulfotosProperties) {
		this.watermarkOpacity = imageProperties.watermarkOpacity();
		this.watermarkScale = imageProperties.watermarkScale();
		this.jpegQuality = imageProperties.jpegQuality();
		this.klassenfotoFolder = schulfotosProperties.klassenfotoFolder();
		this.portraitPrefix = schulfotosProperties.portraitPrefix();
		this.watermarkedSuffix = schulfotosProperties.watermarkedSuffix();
		this.filenameStripPostfix = imageProperties.filenameStripPostfix();
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

		progressListener.accept(new ProcessingProgress(0.10d, "Fotoordner analysieren"));
		List<Path> sourceFolders = collectSourceFolders(eventDir);
		int totalImages = Math.max(countImages(sourceFolders), 1);

		int totalProcessed = 0;
		List<Path> outputFolders = new ArrayList<>();
		for (Path sourceDir : sourceFolders) {
			Path outputDir = watermarkedOutputDir(eventDir, sourceDir);
			int count = resizeAndWatermarkFolder(sourceDir, outputDir, watermark, maxEdge, totalProcessed, totalImages,
					progressListener);
			totalProcessed += count;
			if (count > 0) {
				outputFolders.add(outputDir);
			}
			LOGGER.info("Processed {} photos from {}", count, sourceDir);
		}

		LOGGER.info("Total: processed {} images under {}", totalProcessed, eventDir);
		progressListener.accept(new ProcessingProgress(1.0d, "Wasserzeichen fertig"));
		return new ImageProcessingResult(totalProcessed, outputFolders);
	}

	private Path watermarkedOutputDir(Path eventDir, Path sourceDir) {
		String name = sourceDir.getFileName().toString();
		if (name.equals(klassenfotoFolder)) {
			return eventDir.resolve(klassenfotoFolder + "s" + watermarkedSuffix);
		}
		return eventDir.resolve(name + watermarkedSuffix);
	}

	private List<Path> collectSourceFolders(Path eventDir) throws IOException {
		List<Path> folders = new ArrayList<>();
		Path klassenfotoDir = eventDir.resolve(klassenfotoFolder);
		if (Files.isDirectory(klassenfotoDir)) {
			folders.add(klassenfotoDir);
		}
		if (!Files.isDirectory(eventDir)) {
			return folders;
		}
		List<Path> portraitFolders = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(eventDir, Files::isDirectory)) {
			for (Path sub : stream) {
				String name = sub.getFileName().toString();
				if (name.startsWith(portraitPrefix) && !name.endsWith(watermarkedSuffix)) {
					portraitFolders.add(sub);
				}
			}
		}
		portraitFolders.sort(Path::compareTo);
		folders.addAll(portraitFolders);
		return folders;
	}

	private int countImages(List<Path> sourceFolders) throws IOException {
		int total = 0;
		for (Path folder : sourceFolders) {
			total += listImageFiles(folder).size();
		}
		return total;
	}

	private int resizeAndWatermarkFolder(Path sourceDir, Path outputDir, BufferedImage watermark, int maxEdge,
			int alreadyProcessed, int totalImages, Consumer<ProcessingProgress> progressListener) throws IOException {
		List<Path> sources = listImageFiles(sourceDir);
		if (sources.isEmpty()) {
			return 0;
		}
		Map<String, Path> plan = planOutputNames(sources);

		Path stagingDir = Files.createTempDirectory(outputDir.getParent(), ".staging-");
		try {
			int count = 0;
			for (Map.Entry<String, Path> planned : plan.entrySet()) {
				BufferedImage original = ImageIO.read(planned.getValue().toFile());
				if (original == null) {
					throw new IOException("Could not read image: " + planned.getValue());
				}
				BufferedImage result = applyWatermark(resize(original, maxEdge), watermark);
				writeJpeg(result, stagingDir.resolve(planned.getKey()));
				count++;
				double ratio = (double) (alreadyProcessed + count) / totalImages;
				progressListener.accept(new ProcessingProgress(0.10d + 0.85d * ratio, "Bilder werden verarbeitet"));
			}
			replaceDirectory(stagingDir, outputDir);
			return count;
		}
		finally {
			deleteRecursively(stagingDir);
		}
	}

	private Map<String, Path> planOutputNames(List<Path> sources) throws IOException {
		Map<String, Path> plan = new LinkedHashMap<>();
		List<String> collisions = new ArrayList<>();
		for (Path source : sources) {
			String outputName = changeExtension(stripPostfix(source.getFileName().toString()), "jpg");
			Path previous = plan.putIfAbsent(outputName, source);
			if (previous != null) {
				collisions.add(previous.getFileName() + " + " + source.getFileName() + " -> " + outputName);
			}
		}
		if (!collisions.isEmpty()) {
			throw new IOException("Source files would overwrite each other: " + String.join(", ", collisions));
		}
		return plan;
	}

	private void replaceDirectory(Path stagingDir, Path outputDir) throws IOException {
		deleteRecursively(outputDir);
		Files.move(stagingDir, outputDir);
	}

	private void deleteRecursively(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private List<Path> listImageFiles(Path sourceDir) throws IOException {
		List<Path> files = new ArrayList<>();
		if (!Files.isDirectory(sourceDir)) {
			return files;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, this::isImageFile)) {
			for (Path file : stream) {
				files.add(file);
			}
		}
		files.sort(Path::compareTo);
		return files;
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
		BufferedImage raw = null;
		if (Files.exists(watermarkPath)) {
			raw = ImageIO.read(watermarkPath.toFile());
		}
		if (raw == null) {
			var classPathStream = getClass().getClassLoader().getResourceAsStream(watermarkPath.toString());
			if (classPathStream != null) {
				try (classPathStream) {
					raw = ImageIO.read(classPathStream);
				}
			}
		}
		if (raw == null) {
			throw new IOException("Watermark image not found: " + watermarkPath);
		}
		return toWhite(raw);
	}

	private BufferedImage toWhite(BufferedImage source) {
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = source.getRGB(x, y);
				int alpha = (argb >> 24) & 0xFF;
				result.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
			}
		}
		return result;
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
		String name = path.getFileName().toString();
		if (name.startsWith(".")) {
			return false;
		}
		String lowerCaseName = name.toLowerCase();
		return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".png");
	}

	String stripPostfix(String filename) {
		if (filenameStripPostfix.isEmpty()) {
			return filename;
		}
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			String baseName = filename.substring(0, dotIndex);
			String extension = filename.substring(dotIndex);
			if (baseName.endsWith(filenameStripPostfix)) {
				baseName = baseName.substring(0, baseName.length() - filenameStripPostfix.length());
			}
			return baseName + extension;
		}
		return filename;
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

	public record ProcessingProgress(double percent, String stage) {
	}

}
