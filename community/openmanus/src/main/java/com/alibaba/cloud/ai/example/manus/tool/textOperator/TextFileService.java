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
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Service
@Primary
public class TextFileService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(TextFileService.class);

	/**
	 * 支持的文本文件扩展名集合
	 */
	private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Set.of(".txt", ".md", ".markdown", // 普通文本和Markdown
			".java", ".py", ".js", ".ts", ".jsx", ".tsx", // 常见编程语言
			".html", ".htm", ".css", ".scss", ".sass", ".less", // Web相关
			".xml", ".json", ".yaml", ".yml", ".properties", // 配置文件
			".sql", ".sh", ".bat", ".cmd", // 脚本和数据库
			".log", ".conf", ".ini", // 日志和配置
			".gradle", ".pom", ".mvn" // 构建工具
	));

	private final ConcurrentHashMap<String, FileState> fileStates = new ConcurrentHashMap<>();

	@Override
	public void run(ApplicationArguments args) {
		log.info("TextFileService initialized");
	}

	private Object getFileLock(String planId) {
		return getFileState(planId).getFileLock();
	}

	public FileState getFileState(String planId) {
		return fileStates.computeIfAbsent(planId, k -> new FileState());
	}

	public void closeFileForPlan(String planId) {
		synchronized (getFileLock(planId)) {
			fileStates.remove(planId);
			log.info("Closed file state for plan: {}", planId);
		}
	}

	public boolean isSupportedFileType(String filePath) {
		String fileExtension = getFileExtension(filePath);
		return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
	}

	public String getFileExtension(String filePath) {
		int lastDotIndex = filePath.lastIndexOf('.');
		if (lastDotIndex > 0) {
			return filePath.substring(lastDotIndex).toLowerCase();
		}
		return "";
	}

	public void validateAndGetAbsolutePath(String workingDirectoryPath, String filePath) throws IOException {
		Path workingDir = Paths.get(workingDirectoryPath).toAbsolutePath().normalize();
		Path absolutePath = workingDir.resolve(filePath).normalize();

		// 检查文件是否在工作目录范围内
		if (!absolutePath.startsWith(workingDir)) {
			throw new IOException("Access denied: File path must be within working directory");
		}

		// 检查文件大小（如果文件存在）
		if (Files.exists(absolutePath) && Files.size(absolutePath) > 10 * 1024 * 1024) { // 10MB
																							// limit
			throw new IOException("File is too large (>10MB). For safety reasons, please use a smaller file.");
		}
	}

	public void updateFileState(String planId, String filePath, String operationResult) {
		FileState state = getFileState(planId);
		synchronized (getFileLock(planId)) {
			state.setCurrentFilePath(filePath);
			state.setLastOperationResult(operationResult);
		}
	}

	public String getCurrentFilePath(String planId) {
		return getFileState(planId).getCurrentFilePath();
	}

	public String getLastOperationResult(String planId) {
		return getFileState(planId).getLastOperationResult();
	}

	@PreDestroy
	public void cleanup() {
		log.info("Cleaning up TextFileService resources");
		fileStates.clear();
	}

}
