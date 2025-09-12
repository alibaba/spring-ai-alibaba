/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.manus.tool.pptGenerator;

import com.alibaba.cloud.ai.manus.tool.pptGenerator.PptInput.SlideContent;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.manus.tool.textOperator.FileState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class PptGeneratorService implements IPptGeneratorService {

	private static final Logger log = LoggerFactory.getLogger(PptGeneratorService.class);

	@Autowired
	private ManusProperties manusProperties;

	@Autowired
	private UnifiedDirectoryManager unifiedDirectoryManager;

	// File state management.
	private final ConcurrentHashMap<String, FileState> fileStates = new ConcurrentHashMap<>();

	// Supported PPT file extensions.
	private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Set.of(".pptx", ".ppt"));

	/**
	 * Create a PPT file.
	 * @param pptInput PPT input parameters.
	 * @return Path of the generated PPT file.
	 * @throws IOException IO exception.
	 */
	public String createPpt(PptInput pptInput) throws IOException {
		// Validate input.
		if (pptInput == null) {
			throw new IllegalArgumentException("PPT input cannot be null");
		}
		if (StringUtils.isBlank(pptInput.getFileName())) {
			throw new IllegalArgumentException("File name cannot be blank");
		}
		// Check if we should use a template
		XMLSlideShow presentation;

		// If path is provided and file exists, use it as template
		if (StringUtils.isNotBlank(pptInput.getPath())) {
			Path templatePath = Path.of("extensions/pptGenerator/template").resolve(pptInput.getPath());
			if (Files.exists(templatePath) && isSupportedPptFileType(templatePath.toString())) {
				presentation = new XMLSlideShow(new FileInputStream(templatePath.toFile()));
			}
			else {
				presentation = new XMLSlideShow();
			}
		}
		else {
			presentation = new XMLSlideShow();
		}

		try {
			// If templateContent is provided, use it to replace text in the presentation
			if (StringUtils.isNotBlank(pptInput.getTemplateContent())) {
				replaceTextWithTemplate(presentation, pptInput.getTemplateContent());
			}
			else {
				if (StringUtils.isBlank(pptInput.getTitle())) {
					throw new IllegalArgumentException("Title cannot be blank");
				}
				// Create title slide if not using templateContent
				XSLFSlideMaster titleMaster = presentation.getSlideMasters().get(0);
				XSLFSlideLayout titleLayout = titleMaster.getLayout(SlideLayout.TITLE);
				if (titleLayout == null) {
					log.error("PPT template is missing the required layout (TITLE)");
					throw new IllegalStateException("PPT template is missing the required layout (TITLE)");
				}
				XSLFSlide titleSlide = presentation.createSlide(titleLayout);

				// Set title and subtitle.
				XSLFTextShape titleShape = titleSlide.getPlaceholder(0);
				if (titleShape != null) {
					titleShape.setText(pptInput.getTitle());
				}

				XSLFTextShape subtitleShape = titleSlide.getPlaceholder(1);
				if (subtitleShape != null && StringUtils.isNotBlank(pptInput.getSubtitle())) {
					subtitleShape.setText(pptInput.getSubtitle());
				}

				// Create content slide.
				XSLFSlideLayout contentLayout = titleMaster.getLayout(SlideLayout.TITLE_AND_CONTENT);
				if (contentLayout == null) {
					log.error("PPT template is missing the required layout (TITLE_AND_CONTENT)");
					throw new IllegalStateException("PPT template is missing the required layout (TITLE_AND_CONTENT)");
				}

				List<SlideContent> slideContents = pptInput.getSlideContents();
				if (slideContents != null && !slideContents.isEmpty()) {
					for (int i = 0; i < slideContents.size(); i++) {
						SlideContent slideContent = slideContents.get(i);
						// Validate slide content.
						if (slideContent == null || (StringUtils.isBlank(slideContent.getTitle())
								&& StringUtils.isBlank(slideContent.getContent()))) {
							log.warn("Skip empty slide content, index: {}", i);
							continue;
						}

						XSLFSlide contentSlide = presentation.createSlide(contentLayout);

						// Set slide title.
						XSLFTextShape contentTitle = contentSlide.getPlaceholder(0);
						if (contentTitle != null && StringUtils.isNotBlank(slideContent.getTitle())) {
							contentTitle.setText(slideContent.getTitle());
						}

						// Set slide content.
						XSLFTextShape contentBody = contentSlide.getPlaceholder(1);
						if (contentBody != null && StringUtils.isNotBlank(slideContent.getContent())) {
							contentBody.setText(slideContent.getContent());
						}

						// Insert image (if specified).
						if (StringUtils.isNotBlank(slideContent.getImagePath())) {
							File imageFile = new File(slideContent.getImagePath());
							if (imageFile.exists()) {
								try (FileInputStream fis = new FileInputStream(imageFile)) {
									byte[] pictureData = IOUtils.toByteArray(fis);
									XSLFPictureData picture = presentation.addPicture(pictureData,
											XSLFPictureData.PictureType.JPEG);
									XSLFPictureShape pictureShape = contentSlide.createPicture(picture);
									// Set image position and size.
									pictureShape.setAnchor(new Rectangle(50, 150, 400, 300));
								}
								catch (IOException e) {
									log.warn("Failed to load image: {}", imageFile.getAbsolutePath(), e);
								}
							}
							else {
								log.warn("Specified image file does not exist: {}", imageFile.getAbsolutePath());
							}
						}
					}
				}
			}

			// Determine output file path
			String outputPath = "extensions/pptGenerator";
			if (StringUtils.isNotBlank(pptInput.getFileName())) {
				// If fileName is provided, use it as the output file name
				Path outputPathObj = Path.of(outputPath);
				if (Files.isDirectory(outputPathObj)) {
					// If outputPath is a directory, append fileName to it
					outputPath = outputPathObj.resolve(pptInput.getFileName()).toString();
				}
				else {
					// If outputPath is a file path, replace the file name
					outputPath = outputPathObj.getParent().resolve(pptInput.getFileName()).toString();
				}
			}

			// Save PPT file.
			File outputFile = new File(outputPath);
			try (FileOutputStream out = new FileOutputStream(outputFile)) {
				presentation.write(out);
			}

			log.info("PPT created successfully: {}", outputPath);
			return outputPath;
		}
		finally {
			// Ensure resources are properly released.
			try {
				presentation.close();
			}
			catch (IOException e) {
				log.warn("Failed to close PPT document", e);
			}
		}
	}

	/**
	 * Replace text in presentation with template content.
	 * @param presentation The presentation to modify.
	 * @param templateContent JSON string containing template content.
	 * @throws IOException IO exception.
	 */
	private void replaceTextWithTemplate(XMLSlideShow presentation, String templateContent) throws IOException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode templateNode = objectMapper.readTree(templateContent);

			if (templateNode.has("slides")) {
				JsonNode slidesNode = templateNode.get("slides");
				List<XSLFSlide> slides = presentation.getSlides();

				for (int i = 0; i < slidesNode.size() && i < slides.size(); i++) {
					JsonNode slideNode = slidesNode.get(i);
					if (slideNode.has("content")) {
						JsonNode contentNode = slideNode.get("content");
						XSLFSlide slide = slides.get(i);

						for (XSLFShape shape : slide.getShapes()) {
							if (shape instanceof XSLFTextShape) {
								XSLFTextShape textShape = (XSLFTextShape) shape;
								String shapeName = textShape.getShapeName();

								// Find matching content in template
								for (JsonNode contentItem : contentNode) {
									if (contentItem.has("shapeName") && contentItem.has("text")) {
										String templateShapeName = contentItem.get("shapeName").asText();
										String newText = contentItem.get("text").asText();

										if (shapeName.equals(templateShapeName)) {
											// Text replacement while preserving the
											// original style
											List<XSLFTextParagraph> paragraphs = textShape.getTextParagraphs();
											if (!paragraphs.isEmpty()) {
												XSLFTextParagraph paragraph = paragraphs.get(0);
												List<XSLFTextRun> textRuns = paragraph.getTextRuns();
												if (!textRuns.isEmpty()) {
													XSLFTextRun textRun = textRuns.get(0);
													String oldText = textRun.getRawText();
													String updatedText = oldText.replace(oldText, newText);
													textRun.setText(updatedText);
												}
												else {
													// If no textRun, set text directly
													textShape.setText(newText);
												}
											}
											else {
												// If no paragraph, set text directly
												textShape.setText(newText);
											}
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Failed to replace text with template content", e);
			throw new IOException("Failed to replace text with template content", e);
		}
	}

	// PptGeneratorService interface implementation.

	@Override
	public FileState getFileState(String planId) {
		return fileStates.computeIfAbsent(planId, k -> new FileState());
	}

	@Override
	public void updateFileState(String planId, String filePath, String operationResult) {
		FileState fileState = getFileState(planId);
		fileState.setCurrentFilePath(filePath);
		fileState.setLastOperationResult(operationResult);
		log.info("Updated PPT file state for plan {}: path={}, result={}", planId, filePath, operationResult);
	}

	@Override
	public String getCurrentFilePath(String planId) {
		return getFileState(planId).getCurrentFilePath();
	}

	@Override
	public String getLastOperationResult(String planId) {
		return getFileState(planId).getLastOperationResult();
	}

	@Override
	public String validatePptFilePath(String planId, String filePath) throws IOException {
		// 1. Basic validation.
		if (StringUtils.isBlank(filePath)) {
			throw new IllegalArgumentException("File path cannot be blank");
		}

		// 2. File type validation.
		if (!isSupportedPptFileType(filePath)) {
			throw new IllegalArgumentException("Unsupported file type: " + getFileExtension(filePath));
		}

		// 3. Path normalization.
		Path requestedPath = Path.of(filePath).normalize();

		// 4. Security validation - prevent path traversal attacks.
		if (requestedPath.toString().contains("../") || requestedPath.isAbsolute()) {
			throw new SecurityException("Illegal path: absolute path or parent directory reference is not allowed");
		}

		// 5. Limit output directory range.
		Path baseDir = Path.of("extensions/pptGenerator").normalize();

		// 6. Get the final absolute path.
		Path absolutePath = unifiedDirectoryManager.getSpecifiedDirectory(baseDir.resolve(requestedPath).toString());

		// 7. Ensure the directory exists.
		Files.createDirectories(absolutePath.getParent());

		return absolutePath.toString();
	}

	@Override
	public boolean isSupportedPptFileType(String filePath) {
		String fileExtension = getFileExtension(filePath);
		return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
	}

	@Override
	public String getFileExtension(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			return "";
		}
		int lastDotIndex = filePath.lastIndexOf('.');
		return lastDotIndex > 0 ? filePath.substring(lastDotIndex) : "";
	}

	@Override
	public void cleanupForPlan(String planId) {
		fileStates.remove(planId);
		log.info("Cleaned up PPT generator file state for plan: {}", planId);
	}

	@Override
	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	@Override
	public String getTemplateList() throws IOException {
		// Template directory path
		String templateDirPath = "extensions/pptGenerator/template";
		Path templateDir = Path.of(templateDirPath).normalize();
		Path jsonFilePath = templateDir.resolve("template_list.json");

		if (Files.exists(jsonFilePath)) {
			log.info("Template list JSON file exists, reading directly: {}", jsonFilePath);
			return Files.readString(jsonFilePath);
		}

		// If the JSON file does not exist, traverse the template directory to generate it
		log.info("Template list JSON file does not exist, generating: {}", jsonFilePath);
		Map<String, Map<String, String>> templateMap = new HashMap<>();

		// Traverse the template directory
		try (Stream<Path> paths = Files.walk(templateDir, 2)) {
			paths.filter(Files::isRegularFile).filter(path -> isSupportedPptFileType(path.toString())).forEach(path -> {
				try {
					String fileName = path.getFileName().toString();
					String relativePath = templateDir.relativize(path).toString();

					Map<String, String> titleInfo = getPptTitleInfo(path);
					titleInfo.put("path", relativePath);

					templateMap.put(fileName, titleInfo);
				}
				catch (Exception e) {
					log.error("Failed to process template file: {}", path, e);
				}
			});
		}

		// Write the template information to the JSON file
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(jsonFilePath.toFile(), templateMap);

		log.info("Template list generated successfully: {}", jsonFilePath);
		return mapper.writeValueAsString(templateMap);
	}

	/**
	 * Get all text content from the first slide of a PPT file
	 * @param pptPath PPT file path
	 * @return Map containing description with all text from first slide
	 * @throws IOException IO exception
	 */
	private Map<String, String> getPptTitleInfo(Path pptPath) throws IOException {
		Map<String, String> result = new HashMap<>();
		try (FileInputStream fis = new FileInputStream(pptPath.toFile());
				XMLSlideShow presentation = new XMLSlideShow(fis)) {

			// Get the first slide
			if (presentation.getSlides().isEmpty()) {
				result.put("description", "");
				return result;
			}

			XSLFSlide firstSlide = presentation.getSlides().get(0);

			// Extract all text content from the first slide
			StringBuilder descriptionBuilder = new StringBuilder();

			for (XSLFShape shape : firstSlide.getShapes()) {
				if (shape instanceof XSLFTextShape) {
					XSLFTextShape textShape = (XSLFTextShape) shape;
					String text = textShape.getText();
					if (text != null && !text.isEmpty()) {
						if (descriptionBuilder.length() > 0) {
							// Add a space between text elements
							descriptionBuilder.append(" ");
						}
						descriptionBuilder.append(text);
					}
				}
			}

			result.put("description", descriptionBuilder.toString().trim());
			return result;
		}
	}

	@Override
	public String getTemplate(String path) throws IOException {
		// 1. Basic validation.
		if (StringUtils.isBlank(path)) {
			throw new IllegalArgumentException("Template path cannot be blank");
		}

		// 2. Path normalization.
		Path requestedPath = Path.of(path).normalize();

		// 3. Security validation - prevent path traversal attacks.
		if (requestedPath.toString().contains("../") || requestedPath.isAbsolute()) {
			throw new SecurityException("Illegal path: absolute path or parent directory reference is not allowed");
		}

		// 4. Limit template directory range.
		Path baseDir = Path.of("extensions/pptGenerator/template").normalize();
		Path absolutePath = baseDir.resolve(requestedPath);

		// 5. Check if the file exists.
		if (!Files.exists(absolutePath)) {
			throw new IOException("Template file does not exist: " + absolutePath);
		}

		// 6. Check if the file type is supported.
		if (!isSupportedPptFileType(absolutePath.toString())) {
			throw new IllegalArgumentException("Unsupported file type: " + getFileExtension(absolutePath.toString()));
		}

		// 7. Read the PPT file and extract text content.
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode result = factory.objectNode();
		ArrayNode slidesArray = factory.arrayNode();

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				XMLSlideShow presentation = new XMLSlideShow(fis)) {

			List<XSLFSlide> slides = presentation.getSlides();
			for (int i = 0; i < slides.size(); i++) {
				XSLFSlide slide = slides.get(i);
				ObjectNode slideNode = factory.objectNode();
				slideNode.put("page", i + 1);

				ArrayNode contentArray = factory.arrayNode();
				for (XSLFShape shape : slide.getShapes()) {
					if (shape instanceof XSLFTextShape) {
						XSLFTextShape textShape = (XSLFTextShape) shape;
						String text = textShape.getText();
						if (text != null && !text.isEmpty()) {
							// Create an object node for each text shape with its unique
							// identifier
							ObjectNode textNode = factory.objectNode();
							textNode.put("text", text);
							// Use the shape's name as unique identifier, fallback to
							// class name + index if name is null
							String shapeName = textShape.getShapeName();
							if (shapeName == null || shapeName.isEmpty()) {
								shapeName = textShape.getClass().getSimpleName() + "_"
										+ slide.getShapes().indexOf(textShape);
							}
							textNode.put("shapeName", shapeName);
							contentArray.add(textNode);
						}
					}
				}
				slideNode.set("content", contentArray);
				slidesArray.add(slideNode);
			}
		}

		result.set("slides", slidesArray);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(result);
	}

}
