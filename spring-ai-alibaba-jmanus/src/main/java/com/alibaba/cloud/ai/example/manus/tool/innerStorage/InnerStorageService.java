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
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
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

	private final Map<String, String> planAgentCache = new ConcurrentHashMap<>();

	public InnerStorageService(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	/**
	 * 获取内部存储的根目录路径
	 */
	public Path getInnerStorageRoot(String workingDirectoryPath) {
		return Paths.get(workingDirectoryPath, "inner_storage");
	}

	/**
	 * 获取计划目录路径
	 */
	public Path getPlanDirectory(String workingDirectoryPath, String planId) {
		return getInnerStorageRoot(workingDirectoryPath).resolve(planId);
	}

	/**
	 * 获取Agent目录路径
	 */
	public Path getAgentDirectory(String workingDirectoryPath, String planId, String agentName) {
		if (agentName == null || agentName.trim().isEmpty()) {
			agentName = "default";
		}
		return getPlanDirectory(workingDirectoryPath, planId).resolve(agentName);
	}

	/**
	 * 获取文件路径
	 */
	public Path getFilePath(String workingDirectoryPath, String planId, String fileName) {
		String agentName = getPlanAgent(planId);
		Path agentDir = getAgentDirectory(workingDirectoryPath, planId, agentName);
		return agentDir.resolve(fileName);
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
	 * 设置计划对应的Agent名称
	 */
	public void setPlanAgent(String planId, String agentName) {
		if (planId != null && agentName != null) {
			planAgentCache.put(planId, agentName);
			log.debug("Set agent for plan {}: {}", planId, agentName);
		}
	}

	/**
	 * 获取计划对应的Agent名称
	 */
	public String getPlanAgent(String planId) {
		return planAgentCache.getOrDefault(planId, "default");
	}

	/**
	 * 获取目录下的所有文件信息
	 */
	public List<FileInfo> getDirectoryFiles(String workingDirectoryPath, String planId) {
		List<FileInfo> files = new ArrayList<>();
		try {
			Path planDir = getPlanDirectory(workingDirectoryPath, planId);
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
	public List<FileInfo> searchAutoStoredFiles(String workingDirectoryPath, String planId, String keyword) {
		List<FileInfo> autoFiles = new ArrayList<>();
		try {
			Path planDir = getPlanDirectory(workingDirectoryPath, planId);
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
	public String readFileContent(String workingDirectoryPath, String planId, String relativePath) throws IOException {
		Path planDir = getPlanDirectory(workingDirectoryPath, planId);
		Path filePath = planDir.resolve(relativePath);

		if (!Files.exists(filePath)) {
			throw new IOException("文件不存在: " + relativePath);
		}

		return Files.readString(filePath);
	}

	/**
	 * 清理计划相关的文件
	 */
	public void cleanupPlan(String workingDirectoryPath, String planId) {
		try {
			Path planDir = getPlanDirectory(workingDirectoryPath, planId);
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
			// 从缓存中移除
			planAgentCache.remove(planId);
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

}
