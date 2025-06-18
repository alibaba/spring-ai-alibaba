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
package com.alibaba.cloud.ai.example.manus.tool.textOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 智能文件操作工具抽象基类 当返回值超过指定阈值时，自动使用 InnerStorageService 保存详细内容并返回摘要信息
 */
public abstract class AbstractSmartFileOperator {

	private static final Logger log = LoggerFactory.getLogger(AbstractSmartFileOperator.class);

	// 默认阈值：2KB
	private static final int DEFAULT_CONTENT_THRESHOLD = 3000;

	// 存储计划ID对应的内容阈值配置
	private final Map<String, Integer> planThresholds = new ConcurrentHashMap<>();

	// 存储计划ID对应的内容ID序列号
	private final Map<String, AtomicLong> planContentIdCounters = new ConcurrentHashMap<>();

	/**
	 * 获取工作目录路径
	 */
	protected abstract String getWorkingDirectoryPath();

	/**
	 * 获取当前计划ID
	 */
	protected abstract String getCurrentPlanId();

	/**
	 * 获取 InnerStorageService 实例
	 */
	protected abstract InnerStorageService getInnerStorageService();

	/**
	 * 设置内容阈值
	 * @param planId 计划ID
	 * @param threshold 阈值（字节数）
	 */
	public void setContentThreshold(String planId, int threshold) {
		if (threshold > 0) {
			planThresholds.put(planId, threshold);
			log.debug("Set content threshold for plan {}: {} bytes", planId, threshold);
		}
	}

	/**
	 * 获取内容阈值
	 * @param planId 计划ID
	 * @return 阈值（字节数）
	 */
	protected int getContentThreshold(String planId) {
		return planThresholds.getOrDefault(planId, DEFAULT_CONTENT_THRESHOLD);
	}

	/**
	 * 智能处理工具执行结果 如果返回值超过阈值，则使用 InnerStorageService 保存详细内容并返回摘要
	 * @param result 原始执行结果
	 * @param operationType 操作类型
	 * @param fileName 相关文件名
	 * @return 处理后的执行结果
	 */
	protected ToolExecuteResult processResult(ToolExecuteResult result, String operationType, String fileName) {
		String planId = getCurrentPlanId();
		if (planId == null || result == null || result.getOutput() == null) {
			return result;
		}

		String content = result.getOutput();
		int threshold = getContentThreshold(planId);

		log.info("Processing result for plan {}: content length = {}, threshold = {}", planId, content.length(),
				threshold);

		// 如果内容未超过阈值，直接返回
		if (content.length() <= threshold) {
			log.info("Content length {} is within threshold {}, returning original result", content.length(),
					threshold);
			return result;
		}

		log.info("Content length {} exceeds threshold {}, triggering auto storage", content.length(), threshold);

		try {
			InnerStorageService storageService = getInnerStorageService();

			// 生成存储文件名
			String storageFileName = generateStorageFileName(planId, operationType, fileName);

			// 确保计划目录存在 - 直接使用计划目录，移除 agent 子目录逻辑
			Path planDir = storageService.getPlanDirectory(planId);
			storageService.ensureDirectoryExists(planDir);

			// 保存详细内容到 InnerStorage - 直接在计划目录下存储
			Path storagePath = planDir.resolve(storageFileName);
			saveDetailedContentToStorage(storagePath, content, operationType, fileName);

			// 生成内容ID
			String contentId = generateContentId(planId);

			// 生成简化摘要
			String summary = generateSmartSummary(content, operationType, fileName, contentId, storageFileName);

			log.info("Content exceeds threshold ({} bytes), saved to storage file: {}, content ID: {}", threshold,
					storageFileName, contentId);

			return new ToolExecuteResult(summary);

		}
		catch (IOException e) {
			log.error("Failed to save content to storage for plan {}", planId, e);
			// 如果保存失败，返回截断的内容
			return new ToolExecuteResult(content.substring(0, threshold) + "\n\n... (内容过长，已截断)");
		}
	}

	/**
	 * 生成存储文件名
	 */
	private String generateStorageFileName(String planId, String operationType, String fileName) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String sanitizedFileName = fileName != null ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "unknown";
		return String.format("auto_%s_%s_%s_%s.txt", planId, operationType, sanitizedFileName, timestamp);
	}

	/**
	 * 生成内容ID
	 */
	private String generateContentId(String planId) {
		AtomicLong counter = planContentIdCounters.computeIfAbsent(planId, k -> new AtomicLong(0));
		return String.format("%s_content_%d", planId, counter.incrementAndGet());
	}

	/**
	 * 保存详细内容到存储
	 */
	private void saveDetailedContentToStorage(Path storagePath, String content, String operationType, String fileName)
			throws IOException {
		StringBuilder detailedContent = new StringBuilder();
		detailedContent.append("=".repeat(60)).append("\n");
		detailedContent.append("自动存储的详细内容\n");
		detailedContent.append("=".repeat(60)).append("\n");
		detailedContent.append("生成时间: ").append(LocalDateTime.now()).append("\n");
		detailedContent.append("操作类型: ").append(operationType).append("\n");
		detailedContent.append("原始文件: ").append(fileName != null ? fileName : "N/A").append("\n");
		detailedContent.append("内容长度: ").append(content.length()).append(" 字符\n");
		detailedContent.append("=".repeat(60)).append("\n\n");
		detailedContent.append(content);

		Files.writeString(storagePath, detailedContent.toString(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * 生成智能摘要
	 */
	private String generateSmartSummary(String content, String operationType, String fileName, String contentId,
			String storageFileName) {
		StringBuilder summary = new StringBuilder();

		// 添加操作摘要
		summary.append("📄 ").append(operationType.toUpperCase()).append(" 操作完成\n\n");

		if (fileName != null) {
			summary.append("📁 原始文件: ").append(fileName).append("\n");
		}

		summary.append("📊 内容统计:\n");
		summary.append("  - 总字符数: ").append(content.length()).append("\n");
		summary.append("  - 总行数: ").append(content.split("\n").length).append("\n");

		// 添加内容预览（前几行）
		String[] lines = content.split("\n");
		int previewLines = Math.min(10, lines.length);
		summary.append("  - 内容预览 (前").append(previewLines).append("行):\n");
		for (int i = 0; i < previewLines; i++) {
			String line = lines[i];
			if (line.length() > 100) {
				line = line.substring(0, 100) + "...";
			}
			summary.append("    ").append(i + 1).append(": ").append(line).append("\n");
		}

		if (lines.length > previewLines) {
			summary.append("    ... (还有 ").append(lines.length - previewLines).append(" 行)\n");
		}

		summary.append("\n💾 完整内容已自动保存:\n");
		summary.append("  - 存储文件: ").append(storageFileName).append("\n");
		summary.append("  - 内容ID: ").append(contentId).append("\n\n");
		summary.append("💡 使用 InnerStorageTool 的以下操作获取详细内容:\n");
		summary.append("  - list_contents: 查看所有存储的内容\n");
		summary.append("  - get_content: 根据文件名获取具体内容\n");
		summary.append("  - search: 搜索关键词");

		return summary.toString();
	}

	/**
	 * 清理计划相关的资源
	 * @param planId 计划ID
	 */
	public void cleanupPlan(String planId) {
		planThresholds.remove(planId);
		planContentIdCounters.remove(planId);

		// 清理 InnerStorage 中的相关文件
		try {
			InnerStorageService storageService = getInnerStorageService();
			storageService.cleanupPlan(planId);
			log.info("Cleaned up plan resources: {}", planId);
		}
		catch (Exception e) {
			log.error("Failed to cleanup plan resources: {}", planId, e);
		}
	}

}
