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
package com.alibaba.cloud.ai.manus.tool.innerStorage;

import java.io.IOException;
import java.nio.file.*;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Internal file storage service for storing intermediate data in MapReduce processes
 */
@Service
public class SmartContentSavingService implements ISmartContentSavingService {

	private static final Logger log = LoggerFactory.getLogger(SmartContentSavingService.class);

	private final ManusProperties manusProperties;

	private final UnifiedDirectoryManager directoryManager;

	public SmartContentSavingService(ManusProperties manusProperties, UnifiedDirectoryManager directoryManager) {
		this.manusProperties = manusProperties;
		this.directoryManager = directoryManager;
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	/**
	 * Smart processing result class
	 */
	public static class SmartProcessResult {

		private final String fileName;

		private final String summary;

		public SmartProcessResult(String fileName, String summary) {
			this.fileName = fileName;
			this.summary = summary;
		}

		public String getFileName() {
			return fileName;
		}

		public String getSummary() {
			return summary;
		}

		@Override
		public String toString() {
			return String.format("SmartProcessResult{fileName='%s', summary='%s'}", fileName, summary);
		}

	}

	/**
	 * Intelligently process content, automatically store and return summary if content is
	 * too long
	 * @param planId Plan ID
	 * @param content Content
	 * @param callingMethod Calling method name
	 * @return Processing result containing filename and summary
	 */
	public SmartProcessResult processContent(String planId, String content, String callingMethod) {
		if (planId == null || content == null) {
			log.warn("processContent called with null parameters: planId={}, content={}, callingMethod={}", planId,
					content, callingMethod);
			return new SmartProcessResult(null, content != null ? content : "No content available");
		}

		// Check if content is empty
		if (content.trim().isEmpty()) {
			log.warn("processContent called with empty content: planId={}, callingMethod={}", planId, callingMethod);
			return new SmartProcessResult(null, "No content available");
		}

		// Check if infinite context is enabled
		boolean infiniteContextEnabled = isInfiniteContextEnabled();

		if (!infiniteContextEnabled) {
			// When infinite context is disabled, return content directly without any
			// processing
			log.info("Infinite context disabled for plan {}, returning content directly without smart processing",
					planId);
			return new SmartProcessResult(null, content != null && !content.trim().isEmpty() ? content : "");
		}

		// Use configured threshold from ManusProperties when infinite context is enabled
		int threshold = manusProperties.getInfiniteContextTaskContextSize();

		log.info("Processing content for plan {}: content length = {}, threshold = {}, infinite context enabled",
				planId, content.length(), threshold);

		// If content is within threshold, return directly
		if (content.length() <= threshold) {
			log.info("Content length {} is within threshold {}, returning original content", content.length(),
					threshold);
			return new SmartProcessResult(null, content != null && !content.trim().isEmpty() ? content : "");
		}

		log.info("Content length {} exceeds threshold {}, triggering auto storage", content.length(), threshold);

		try {
			// Generate storage filename
			String storageFileName = generateStorageFileName(planId);

			// Ensure plan directory exists - store directly in planId directory, not
			// using agent subdirectory
			Path planDir = directoryManager.getRootPlanDirectory(planId);
			directoryManager.ensureDirectoryExists(planDir);

			// Save detailed content to InnerStorage - store directly in plan directory
			Path storagePath = planDir.resolve(storageFileName);
			saveDetailedContentToStorage(storagePath, content, planId);

			// Generate simplified summary
			String summary = generateSmartSummary(content, storageFileName, callingMethod);

			log.info("Content exceeds threshold ({} bytes), saved to storage file: {}", threshold, storageFileName);

			return new SmartProcessResult(storageFileName, summary);

		}
		catch (IOException e) {
			log.error("Failed to save content to storage for plan {}", planId, e);
			// If save fails, return truncated content
			return new SmartProcessResult(null,
					content.substring(0, threshold) + "\n\n... (Content too long, truncated)");
		}
	}

	/**
	 * Generate storage filename - format: planId_timestamp_random4digits.md
	 */
	private String generateStorageFileName(String planId) {
		String timestamp = java.time.LocalDateTime.now()
			.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		// Generate 4-digit random number
		int randomNum = (int) (Math.random() * 9000) + 1000; // 1000-9999
		return String.format("%s_%s_%04d.md", planId, timestamp, randomNum);
	}

	/**
	 * Save detailed content to storage
	 */
	private void saveDetailedContentToStorage(Path storagePath, String content, String planId) throws IOException {
		StringBuilder detailedContent = new StringBuilder();
		detailedContent.append(content);

		Files.writeString(storagePath, detailedContent.toString(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * Generate intelligent summary
	 */
	private String generateSmartSummary(String content, String storageFileName, String callingMethod) {
		// Build calling method information
		String methodInfo = (callingMethod != null && !callingMethod.trim().isEmpty())
				? "Successfully called " + callingMethod + " function,\n\n" : "";

		return String.format(
				"""
						%sBut the function returned content is too long, so it was automatically stored in a file

						## You can freely use the following operations to meet user expectations (no need to follow order, but according to user expectations)

						### Operation 1: Use extract_relevant_content to get specific content from the stored file or directory
						```json
						{
						  "action": "extract_relevant_content",
						  "fileName": "%s",
						  "queryKey": "Keywords or questions you want to query, be specific and don't miss any requirements from user requests",
						  "outputFormatSpecification": "Specify the desired output format for the extracted content"
						}
						```


						""",
				methodInfo, storageFileName);
	}

	/**
	 * Check if infinite context is enabled
	 * @return true if infinite context is enabled, false otherwise
	 */
	private boolean isInfiniteContextEnabled() {
		if (manusProperties != null) {
			Boolean enabled = manusProperties.getInfiniteContextEnabled();
			return enabled != null ? enabled : false;
		}
		return false;
	}

}
