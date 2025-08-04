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
package com.alibaba.cloud.ai.example.manus.tool.pptGenerator;

import com.alibaba.cloud.ai.example.manus.tool.pptGenerator.PptInput.SlideContent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.FileState;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
		if (StringUtils.isBlank(pptInput.getOutputPath())) {
			throw new IllegalArgumentException("Output path cannot be blank");
		}
		if (StringUtils.isBlank(pptInput.getTitle())) {
			throw new IllegalArgumentException("Title cannot be blank");
		}

		// Create PPT document.
		XMLSlideShow presentation = new XMLSlideShow();

		try {
			// Create title slide.
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
					if (slideContent == null) {
						continue;
					}

					// Validate slide content.
					if (StringUtils.isBlank(slideContent.getTitle())
							&& StringUtils.isBlank(slideContent.getContent())) {
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

			// Save PPT file.
			File outputFile = new File(pptInput.getOutputPath());
			try (FileOutputStream out = new FileOutputStream(outputFile)) {
				presentation.write(out);
			}

			log.info("PPT created successfully: {}", pptInput.getOutputPath());
			return pptInput.getOutputPath();
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

}
