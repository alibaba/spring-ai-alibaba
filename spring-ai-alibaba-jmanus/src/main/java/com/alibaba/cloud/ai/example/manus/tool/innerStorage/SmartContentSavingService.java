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
		// 构建调用方法信息
		String methodInfo = (callingMethod != null && !callingMethod.trim().isEmpty())
				? "成功调用了" + callingMethod + "函数，\n\n" : "";

		return String.format("""
				%s但函数返回的内容过长，所以自动存储到了文件里

				## 你可以自由的使用后续的两个操作来达成用户的期望（不需要按照顺序，而是按照用户期望）

				### 操作1 ： 使用 inner_storage_content_tool 工具获取具体内容
				```json
				{
				  "action": "get_content",
				  "file_name": "%s",
				  "query_key": "你要查询的关键词或问题，查询要具体，不要丢掉任何一个用户请求中的需求"
				}
				```

				### 操作2 ： 使用 file_merge_tool 工具将文件聚合（或者复制）到指定文件夹
				```json
				{
				  "action": "merge_file",
				  "file_name": "%s",
				  "target_folder": "merged_data"
				}
				```

				请根据具体需求选择合适的工具和参数进行后续操作。

				""", methodInfo, storageFileName, storageFileName);
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
