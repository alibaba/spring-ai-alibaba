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
package com.alibaba.cloud.ai.example.manus.tool.innerStorage;

import java.io.IOException;
import java.nio.file.*;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 内部文件存储服务，用于MapReduce流程中存储中间数据
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
	 * 智能处理结果类
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
	 * 智能处理内容，如果内容过长则自动存储并返回摘要
	 * @param planId 计划ID
	 * @param content 内容
	 * @param callingMethod 调用的方法名
	 * @return 处理结果，包含文件名和摘要
	 */
	public SmartProcessResult processContent(String planId, String content, String callingMethod) {
		if (planId == null || content == null) {
			return new SmartProcessResult(null, content);
		}

		// Check if infinite context is enabled
		boolean infiniteContextEnabled = isInfiniteContextEnabled();

		if (!infiniteContextEnabled) {
			// When infinite context is disabled, return content directly without any
			// processing
			log.info("Infinite context disabled for plan {}, returning content directly without smart processing",
					planId);
			return new SmartProcessResult(null, content);
		}

		// Use configured threshold from ManusProperties when infinite context is enabled
		int threshold = manusProperties.getInfiniteContextTaskContextSize();

		log.info("Processing content for plan {}: content length = {}, threshold = {}, infinite context enabled",
				planId, content.length(), threshold);

		// If content is within threshold, return directly
		if (content.length() <= threshold) {
			log.info("Content length {} is within threshold {}, returning original content", content.length(),
					threshold);
			return new SmartProcessResult(null, content);
		}

		log.info("Content length {} exceeds threshold {}, triggering auto storage", content.length(), threshold);

		try {
			// 生成存储文件名
			String storageFileName = generateStorageFileName(planId);

			// 确保计划目录存在 - 直接存储在 planId 目录下，不使用 agent 子目录
			Path planDir = directoryManager.getRootPlanDirectory(planId);
			directoryManager.ensureDirectoryExists(planDir);

			// 保存详细内容到 InnerStorage - 直接存储在计划目录下
			Path storagePath = planDir.resolve(storageFileName);
			saveDetailedContentToStorage(storagePath, content, planId);

			// 生成简化摘要
			String summary = generateSmartSummary(content, storageFileName, callingMethod);

			log.info("Content exceeds threshold ({} bytes), saved to storage file: {}", threshold, storageFileName);

			return new SmartProcessResult(storageFileName, summary);

		}
		catch (IOException e) {
			log.error("Failed to save content to storage for plan {}", planId, e);
			// 如果保存失败，返回截断的内容
			return new SmartProcessResult(null, content.substring(0, threshold) + "\n\n... (内容过长，已截断)");
		}
	}

	/**
	 * 生成存储文件名 - 格式：planId_时间戳_随机4位数.md
	 */
	private String generateStorageFileName(String planId) {
		String timestamp = java.time.LocalDateTime.now()
			.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		// 生成4位随机数
		int randomNum = (int) (Math.random() * 9000) + 1000; // 1000-9999
		return String.format("%s_%s_%04d.md", planId, timestamp, randomNum);
	}

	/**
	 * 保存详细内容到存储
	 */
	private void saveDetailedContentToStorage(Path storagePath, String content, String planId) throws IOException {
		StringBuilder detailedContent = new StringBuilder();
		detailedContent.append(content);

		Files.writeString(storagePath, detailedContent.toString(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * 生成智能摘要
	 */
	private String generateSmartSummary(String content, String storageFileName, String callingMethod) {
		StringBuilder summary = new StringBuilder();

		// 如果提供了调用方法，添加成功调用信息
		if (callingMethod != null && !callingMethod.trim().isEmpty()) {
			summary.append("成功调用了").append(callingMethod).append("函数，\n\n");
		}

		summary.append("但函数返回的内容过长，所以自动存储到了文件里");
		summary.append("\n\n");
		summary.append("存储文件的名: ").append(storageFileName).append("\n\n");

		// 添加内容统计
		String[] lines = content.split("\n");
		summary.append("内容统计:\n");
		summary.append("  - 总字符数: ").append(content.length()).append("\n");
		summary.append("  - 总行数: ").append(lines.length).append("\n\n");

		summary.append("在后续的调用中，必需要使用 inner_storage_content_tool 工具的 getContent 来获取相关信息,\n");
		summary.append("该方法可以从内容中总结出需要的关键信息。\n\n");
		return summary.toString();
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
