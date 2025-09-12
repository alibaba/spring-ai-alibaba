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
package com.alibaba.cloud.ai.manus.runtime.controller;

import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/file-browser")
@CrossOrigin(origins = "*")
public class FileBrowserController {

	private static final Logger logger = LoggerFactory.getLogger(FileBrowserController.class);

	@Autowired
	private UnifiedDirectoryManager directoryManager;

	/**
	 * File tree node representation
	 */
	public static class FileNode {

		private String name;

		private String path;

		private String type; // "file" or "directory"

		private long size;

		private String lastModified;

		private List<FileNode> children;

		public FileNode() {
		}

		public FileNode(String name, String path, String type, long size, String lastModified) {
			this.name = name;
			this.path = path;
			this.type = type;
			this.size = size;
			this.lastModified = lastModified;
			this.children = new ArrayList<>();
		}

		// Getters and setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public String getLastModified() {
			return lastModified;
		}

		public void setLastModified(String lastModified) {
			this.lastModified = lastModified;
		}

		public List<FileNode> getChildren() {
			return children;
		}

		public void setChildren(List<FileNode> children) {
			this.children = children;
		}

	}

	/**
	 * Get file tree for a specific plan ID
	 * @param planId The plan ID
	 * @return File tree structure
	 */
	@GetMapping("/tree/{planId}")
	public ResponseEntity<?> getFileTree(@PathVariable("planId") String planId) {
		try {
			Path planDir = directoryManager.getRootPlanDirectory(planId);

			if (!Files.exists(planDir)) {
				return ResponseEntity
					.ok(Map.of("success", false, "message", "Plan directory not found for planId: " + planId));
			}

			FileNode rootNode = buildFileTree(planDir, planId);

			return ResponseEntity.ok(Map.of("success", true, "data", rootNode));

		}
		catch (Exception e) {
			logger.error("Error getting file tree for planId: {}", planId, e);
			return ResponseEntity.internalServerError()
				.body(Map.of("success", false, "message", "Error retrieving file tree: " + e.getMessage()));
		}
	}

	/**
	 * Get file content
	 * @param planId The plan ID
	 * @param filePath The relative file path
	 * @return File content
	 */
	@GetMapping("/content/{planId}")
	public ResponseEntity<?> getFileContent(@PathVariable("planId") String planId,
			@RequestParam("path") String filePath) {
		try {
			Path planDir = directoryManager.getRootPlanDirectory(planId);
			Path targetFile = planDir.resolve(filePath).normalize();

			// Security check: ensure the file is within the plan directory
			if (!targetFile.startsWith(planDir)) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Access denied: File path is outside plan directory"));
			}

			if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
				return ResponseEntity.notFound().build();
			}

			String content = Files.readString(targetFile);
			String mimeType = Files.probeContentType(targetFile);

			return ResponseEntity.ok(Map.of("success", true, "data", Map.of("content", content, "mimeType",
					mimeType != null ? mimeType : "text/plain", "size", Files.size(targetFile))));

		}
		catch (Exception e) {
			logger.error("Error reading file content for planId: {}, path: {}", planId, filePath, e);
			return ResponseEntity.internalServerError()
				.body(Map.of("success", false, "message", "Error reading file: " + e.getMessage()));
		}
	}

	/**
	 * Download file
	 * @param planId The plan ID
	 * @param filePath The relative file path
	 * @return File download response
	 */
	@GetMapping("/download/{planId}")
	public ResponseEntity<Resource> downloadFile(@PathVariable("planId") String planId,
			@RequestParam("path") String filePath) {
		try {
			Path planDir = directoryManager.getRootPlanDirectory(planId);
			Path targetFile = planDir.resolve(filePath).normalize();

			// Security check: ensure the file is within the plan directory
			if (!targetFile.startsWith(planDir)) {
				return ResponseEntity.badRequest().build();
			}

			if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
				return ResponseEntity.notFound().build();
			}

			Resource resource = new FileSystemResource(targetFile);
			String mimeType = Files.probeContentType(targetFile);
			if (mimeType == null) {
				mimeType = "application/octet-stream";
			}

			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(mimeType))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + targetFile.getFileName().toString() + "\"")
				.body(resource);

		}
		catch (Exception e) {
			logger.error("Error downloading file for planId: {}, path: {}", planId, filePath, e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Build file tree recursively
	 */
	private FileNode buildFileTree(Path directory, String planId) throws IOException {
		String relativePath = "";
		Path planDir = directoryManager.getRootPlanDirectory(planId);

		if (!directory.equals(planDir)) {
			relativePath = planDir.relativize(directory).toString();
		}

		FileNode node = new FileNode(directory.getFileName() != null ? directory.getFileName().toString() : planId,
				relativePath, "directory", 0, Files.getLastModifiedTime(directory).toString());

		try (Stream<Path> children = Files.list(directory)) {
			children.sorted((a, b) -> {
				// Directories first, then files
				boolean aIsDir = Files.isDirectory(a);
				boolean bIsDir = Files.isDirectory(b);
				if (aIsDir && !bIsDir)
					return -1;
				if (!aIsDir && bIsDir)
					return 1;
				return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
			}).forEach(child -> {
				try {
					if (Files.isDirectory(child)) {
						node.getChildren().add(buildFileTree(child, planId));
					}
					else {
						String childRelativePath = planDir.relativize(child).toString();
						FileNode fileNode = new FileNode(child.getFileName().toString(), childRelativePath, "file",
								Files.size(child), Files.getLastModifiedTime(child).toString());
						node.getChildren().add(fileNode);
					}
				}
				catch (IOException e) {
					logger.warn("Error processing file: {}", child, e);
				}
			});
		}

		return node;
	}

}
