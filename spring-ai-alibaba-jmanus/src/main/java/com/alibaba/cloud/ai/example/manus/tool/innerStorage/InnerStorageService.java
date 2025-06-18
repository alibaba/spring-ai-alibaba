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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 内部文件存储服务，用于MapReduce流程中存储中间数据
 */
@Service
public class InnerStorageService {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageService.class);

	private final ManusProperties manusProperties;

	private final String workingDirectoryPath;

	public InnerStorageService(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	/**
	 * 获取内部存储的根目录路径
	 */
	public Path getInnerStorageRoot() {
		return Paths.get(workingDirectoryPath, "inner_storage");
	}

	/**
	 * 获取计划目录路径
	 */
	public Path getPlanDirectory(String planId) {
		return getInnerStorageRoot().resolve(planId);
	}

	/**
	 * 获取Agent目录路径
	 */
	public Path getAgentDirectory(String planId, String agentName) {
		if (agentName == null || agentName.trim().isEmpty()) {
			agentName = "default";
		}
		return getPlanDirectory(planId).resolve(agentName);
	}


	/**
	 * 确保目录存在
	 */
	public void ensureDirectoryExists(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			Files.createDirectories(directory);
			log.debug("Created directory: {}", directory);
		}
	}

	/**
	 * 获取目录下的所有文件信息
	 */
	public List<FileInfo> getDirectoryFiles(String planId) {
		List<FileInfo> files = new ArrayList<>();
		try {
			Path planDir = getPlanDirectory(planId);
			if (!Files.exists(planDir)) {
				return files;
			}

			Files.walkFileTree(planDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile()) {
						String relativePath = planDir.relativize(file).toString();
						long size = attrs.size();
						String lastModified = attrs.lastModifiedTime().toString();
						files.add(new FileInfo(relativePath, size, lastModified));
					}
					return FileVisitResult.CONTINUE;
				}
			});

		}
		catch (IOException e) {
			log.error("Failed to list files for plan {}", planId, e);
		}
		return files;
	}

	/**
	 * 搜索自动存储的文件（以"auto_"开头的文件）
	 */
	public List<FileInfo> searchAutoStoredFiles(String planId, String keyword) {
		List<FileInfo> autoFiles = new ArrayList<>();
		try {
			Path planDir = getPlanDirectory(planId);
			if (!Files.exists(planDir)) {
				return autoFiles;
			}

			Files.walkFileTree(planDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile()) {
						String fileName = file.getFileName().toString();
						// 只包含自动存储的文件（以"auto_"开头）
						if (fileName.startsWith("auto_")) {
							String relativePath = planDir.relativize(file).toString();
							// 如果提供了关键词，进行内容搜索
							if (keyword == null || keyword.trim().isEmpty() || containsKeyword(file, keyword)) {
								long size = attrs.size();
								String lastModified = attrs.lastModifiedTime().toString();
								autoFiles.add(new FileInfo(relativePath, size, lastModified));
							}
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});

		}
		catch (IOException e) {
			log.error("Failed to search auto stored files for plan {}", planId, e);
		}
		return autoFiles;
	}

	/**
	 * 检查文件是否包含关键词
	 */
	private boolean containsKeyword(Path file, String keyword) {
		try {
			String content = Files.readString(file);
			return content.toLowerCase().contains(keyword.toLowerCase());
		}
		catch (IOException e) {
			log.warn("Failed to read file for keyword search: {}", file, e);
			return false;
		}
	}

	/**
	 * 根据文件路径读取文件内容
	 */
	public String readFileContent(String planId, String relativePath) throws IOException {
		Path planDir = getPlanDirectory(planId);
		Path filePath = planDir.resolve(relativePath);

		if (!Files.exists(filePath)) {
			throw new IOException("文件不存在: " + relativePath);
		}

		return Files.readString(filePath);
	}

	/**
	 * 清理计划相关的文件
	 */
	public void cleanupPlan(String planId) {
		try {
			Path planDir = getPlanDirectory(planId);
			if (Files.exists(planDir)) {
				Files.walkFileTree(planDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
				log.info("Cleaned up files for plan: {}", planId);
			}
		}
		catch (IOException e) {
			log.error("Failed to cleanup plan {}", planId, e);
		}
	}

	/**
	 * 文件信息类
	 */
	public static class FileInfo {

		private final String relativePath;

		private final long size;

		private final String lastModified;

		public FileInfo(String relativePath, long size, String lastModified) {
			this.relativePath = relativePath;
			this.size = size;
			this.lastModified = lastModified;
		}

		public String getRelativePath() {
			return relativePath;
		}

		public long getSize() {
			return size;
		}

		public String getLastModified() {
			return lastModified;
		}

		@Override
		public String toString() {
			return String.format("%s (%d bytes, %s)", relativePath, size, lastModified);
		}

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
	 * @return 处理结果，包含文件名和摘要
	 */
	public SmartProcessResult processContent(String planId, String content) {
		if (planId == null || content == null) {
			return new SmartProcessResult(null, content);
		}

		// 默认阈值：2KB
		int threshold = 2048;
		
		log.info("Processing content for plan {}: content length = {}, threshold = {}", planId, content.length(), threshold);

		// 如果内容未超过阈值，直接返回
		if (content.length() <= threshold) {
			log.info("Content length {} is within threshold {}, returning original content", content.length(), threshold);
			return new SmartProcessResult(null, content);
		}

		log.info("Content length {} exceeds threshold {}, triggering auto storage", content.length(), threshold);

		try {
			// 生成存储文件名
			String storageFileName = generateStorageFileName(planId);

			// 确保计划目录存在 - 直接存储在 planId 目录下，不使用 agent 子目录
			Path planDir = getPlanDirectory(planId);
			ensureDirectoryExists(planDir);

			// 保存详细内容到 InnerStorage - 直接存储在计划目录下
			Path storagePath = planDir.resolve(storageFileName);
			saveDetailedContentToStorage(storagePath, content, planId);

			// 生成简化摘要
			String summary = generateSmartSummary(content, storageFileName);

			log.info("Content exceeds threshold ({} bytes), saved to storage file: {}", threshold, storageFileName);

			return new SmartProcessResult(storageFileName, summary);

		} catch (IOException e) {
			log.error("Failed to save content to storage for plan {}", planId, e);
			// 如果保存失败，返回截断的内容
			return new SmartProcessResult(null, content.substring(0, threshold) + "\n\n... (内容过长，已截断)");
		}
	}

	/**
	 * 生成存储文件名 - 格式：planId_时间戳_随机4位数.md
	 */
	private String generateStorageFileName(String planId) {
		String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
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

		Files.writeString(storagePath, detailedContent.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * 生成智能摘要
	 */
	private String generateSmartSummary(String content, String storageFileName) {
		StringBuilder summary = new StringBuilder();

		summary.append("📄 内容已自动存储\n\n");
		summary.append("📊 内容统计:\n");
		summary.append("  - 总字符数: ").append(content.length()).append("\n");
		summary.append("  - 总行数: ").append(content.split("\n").length).append("\n");

		// 添加内容预览（前几行）
		String[] lines = content.split("\n");
		int previewLines = Math.min(5, lines.length);
		summary.append("  - 内容预览 (前").append(previewLines).append("行):\n");
		for (int i = 0; i < previewLines; i++) {
			String line = lines[i];
			if (line.length() > 80) {
				line = line.substring(0, 80) + "...";
			}
			summary.append("    ").append(i + 1).append(": ").append(line).append("\n");
		}

		if (lines.length > previewLines) {
			summary.append("    ... (还有 ").append(lines.length - previewLines).append(" 行)\n");
		}

		summary.append("\n💾 完整内容已自动保存:\n");
		summary.append("  - 存储文件: ").append(storageFileName).append("\n\n");
		summary.append("💡 使用 InnerStorageTool 的以下操作获取详细内容:\n");
		summary.append("  - list_contents: 查看所有存储的内容\n");
		summary.append("  - get_content: 根据文件名获取具体内容\n");
		summary.append("  - search: 搜索关键词");

		return summary.toString();
	}

}
