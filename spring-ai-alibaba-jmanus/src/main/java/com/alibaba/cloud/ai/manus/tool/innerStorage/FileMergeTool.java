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

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * File merge tool for merging single files into specified target folders, merging one
 * file per call
 */
public class FileMergeTool extends AbstractBaseTool<FileMergeTool.FileMergeInput> {

	private static final Logger log = LoggerFactory.getLogger(FileMergeTool.class);

	/**
	 * File merge input class
	 */
	public static class FileMergeInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_name")
		private String fileName;

		@com.fasterxml.jackson.annotation.JsonProperty("target_folder")
		private String targetFolder;

		public FileMergeInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getTargetFolder() {
			return targetFolder;
		}

		public void setTargetFolder(String targetFolder) {
			this.targetFolder = targetFolder;
		}

	}

	private final UnifiedDirectoryManager directoryManager;

	public FileMergeTool(UnifiedDirectoryManager directoryManager) {
		this.directoryManager = directoryManager;
	}

	private static final String TOOL_NAME = "file_merge_tool";

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return """
				Copy a single file to a specified target folder within the current plan directory.
				This tool searches for files containing the specified name and copies the found file to the target folder.

				* IMPORTANT: target_folder must be a RELATIVE path within the current plan directory.
				* DO NOT use absolute paths (paths starting with / or containing full directory paths).
				* Use relative paths like: "html_data", "output", "results", etc.
				""";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "file_name": {
				            "type": "string",
				            "description": "Name or partial name of the file to copy"
				        },
				        "target_folder": {
				            "type": "string",
				            "description": "Target folder name (RELATIVE PATH ONLY). DO NOT use absolute paths or paths starting with '/'."
				        }
				    },
				    "required": ["file_name", "target_folder"]
				}
				""";
	}

	@Override
	public Class<FileMergeInput> getInputType() {
		return FileMergeInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	/**
	 * Execute file merge operation
	 */
	@Override
	public ToolExecuteResult run(FileMergeInput input) {
		log.info("FileMergeTool input: action={}, fileName={}, targetFolder={}", input.getAction(), input.getFileName(),
				input.getTargetFolder());
		try {
			return mergeFile(input.getFileName(), input.getTargetFolder());
		}
		catch (Exception e) {
			log.error("FileMergeTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Merge single file to specified folder
	 */
	private ToolExecuteResult mergeFile(String fileName, String targetFolder) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return new ToolExecuteResult("Error: file_name parameter is required");
		}
		if (targetFolder == null || targetFolder.trim().isEmpty()) {
			return new ToolExecuteResult("Error: target_folder parameter is required");
		}

		try {
			// Validate and normalize target folder path
			String normalizedTargetFolder = validateAndNormalizeTargetFolder(targetFolder);
			if (normalizedTargetFolder == null) {
				return new ToolExecuteResult(
						"Error: target_folder must be a relative path. Absolute paths are not allowed.");
			}

			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			Path targetDir = planDir.resolve(normalizedTargetFolder);

			// Security check: ensure target directory is within plan directory
			if (!isTargetPathAllowed(planDir, targetDir)) {
				return new ToolExecuteResult("Error: Target folder is outside the allowed plan directory scope.");
			}

			// Ensure target folder exists
			Files.createDirectories(targetDir);

			// Find matching files
			String actualFileName = null;
			Path sourceFile = null;
			List<Path> files = Files.list(planDir).filter(Files::isRegularFile).toList();

			for (Path filePath : files) {
				if (filePath.getFileName().toString().contains(fileName)) {
					sourceFile = filePath;
					actualFileName = filePath.getFileName().toString();
					break;
				}
			}

			if (sourceFile == null) {
				return new ToolExecuteResult("File with name '" + fileName
						+ "' not found. Please use part of the filename to search for files.");
			}

			// Copy file to target folder
			Path targetFile = targetDir.resolve(actualFileName);
			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

			log.info("File merge completed: {} -> {}", actualFileName, targetFolder);

			StringBuilder result = new StringBuilder();
			result.append("File merge successful\n");
			result.append("Source file: ").append(actualFileName).append("\n");
			result.append("Target folder: ").append(targetFolder).append("\n");
			result.append("Target file path: ").append(targetFolder).append("/").append(actualFileName).append("\n");

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("File merge failed", e);
			return new ToolExecuteResult("File merge failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("File merge operation failed", e);
			return new ToolExecuteResult("File merge operation failed: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("FileMerge current status:\n");
			sb.append("- Storage root directory: ")
				.append(directoryManager.getRootPlanDirectory(rootPlanId))
				.append("\n");
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			List<Path> files = Files.exists(planDir) ? Files.list(planDir).filter(Files::isRegularFile).toList()
					: List.of();
			if (files.isEmpty()) {
				sb.append("- Available files: None\n");
			}
			else {
				sb.append("- Available files (").append(files.size()).append(" files): ");
				for (int i = 0; i < Math.min(files.size(), 5); i++) {
					sb.append(files.get(i).getFileName().toString());
					if (i < Math.min(files.size(), 5) - 1) {
						sb.append(", ");
					}
				}
				if (files.size() > 5) {
					sb.append("...");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
		catch (Exception e) {
			log.error("Failed to get tool status", e);
			return "FileMerge status retrieval failed: " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		// File merge tool does not need to perform cleanup operations
		log.info("FileMergeTool cleanup for plan: {}", planId);
	}

	/**
	 * Validate and normalize target folder path
	 * @param targetFolder original target folder path
	 * @return normalized relative path, or null if invalid
	 */
	private String validateAndNormalizeTargetFolder(String targetFolder) {
		if (targetFolder == null || targetFolder.trim().isEmpty()) {
			return null;
		}

		String trimmed = targetFolder.trim();

		// Check if it's an absolute path
		if (trimmed.startsWith("/") || trimmed.matches("^[A-Za-z]:.*")) {
			log.warn("Absolute path detected and rejected: {}", trimmed);
			return null;
		}

		// Normalize path, remove redundant path separators and relative path components
		Path normalized = Paths.get(trimmed).normalize();

		// Check if it contains upward directory references
		if (normalized.toString().contains("..")) {
			log.warn("Path traversal detected and rejected: {}", trimmed);
			return null;
		}

		return normalized.toString();
	}

	/**
	 * Validate if target path is within allowed scope
	 */
	private boolean isTargetPathAllowed(Path planDir, Path targetDir) {
		try {
			// Ensure target directory is within plan directory
			Path normalizedPlanDir = planDir.toAbsolutePath().normalize();
			Path normalizedTargetDir = targetDir.toAbsolutePath().normalize();

			return normalizedTargetDir.startsWith(normalizedPlanDir);
		}
		catch (Exception e) {
			log.error("Error validating target path", e);
			return false;
		}
	}

}
