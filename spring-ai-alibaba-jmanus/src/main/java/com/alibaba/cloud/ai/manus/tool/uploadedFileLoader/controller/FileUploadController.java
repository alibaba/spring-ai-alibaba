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
package com.alibaba.cloud.ai.manus.tool.uploadedFileLoader.controller;

import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * File upload controller for handling multiple file uploads to specific plans Supports
 * uploading multiple documents to a plan's workspace for processing
 */
@RestController
@RequestMapping("/api/file-upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

	private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

	@Autowired
	private UnifiedDirectoryManager directoryManager;

	// Supported file extensions for upload
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".pdf", ".txt", ".md", ".doc", ".docx", ".csv",
			".xlsx", ".xls", ".json", ".xml", ".html", ".htm", ".log", ".java", ".py", ".js", ".ts", ".sql", ".sh",
			".bat", ".yaml", ".yml", ".properties", ".conf", ".ini",
			// Image formats
			".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".tif", ".webp", ".svg");

	/**
	 * Upload multiple files to a specific plan
	 * @param planId The target plan ID
	 * @param files Array of files to upload
	 * @return Upload result with file information
	 */
	@PostMapping("/upload/{planId}")
	public ResponseEntity<Map<String, Object>> uploadFiles(@PathVariable String planId,
			@RequestParam("files") MultipartFile[] files) {

		logger.info("Received file upload request for planId: {}, files count: {}", planId, files.length);

		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> uploadedFiles = new ArrayList<>();
		List<String> errors = new ArrayList<>();

		try {
			// Validate plan ID
			if (planId == null || planId.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Plan ID is required"));
			}

			// Create plan directory if not exists
			Path planDirectory = directoryManager.getRootPlanDirectory(planId);
			Path uploadsDirectory = planDirectory.resolve("uploads");
			Files.createDirectories(uploadsDirectory);

			// Process each file
			for (MultipartFile file : files) {
				try {
					Map<String, Object> fileResult = processFileUpload(file, uploadsDirectory);
					if (fileResult.containsKey("error")) {
						errors.add((String) fileResult.get("error"));
					}
					else {
						uploadedFiles.add(fileResult);
					}
				}
				catch (Exception e) {
					logger.error("Error processing file: {}", file.getOriginalFilename(), e);
					errors.add("Failed to process file: " + file.getOriginalFilename() + " - " + e.getMessage());
				}
			}

			// Prepare response
			response.put("planId", planId);
			response.put("uploadedFiles", uploadedFiles);
			response.put("successCount", uploadedFiles.size());
			response.put("errorCount", errors.size());

			if (!errors.isEmpty()) {
				response.put("errors", errors);
			}

			logger.info("File upload completed for planId: {}, success: {}, errors: {}", planId, uploadedFiles.size(),
					errors.size());

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error handling file upload for planId: {}", planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Failed to upload files: " + e.getMessage()));
		}
	}

	/**
	 * Upload files without specific plan (creates temporary plan)
	 * @param files Array of files to upload
	 * @return Upload result with temporary plan ID
	 */
	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadFilesTemporary(@RequestParam("files") MultipartFile[] files) {

		// Generate temporary plan ID
		String tempPlanId = "temp-" + System.currentTimeMillis();
		logger.info("Creating temporary plan for file upload: {}", tempPlanId);

		return uploadFiles(tempPlanId, files);
	}

	/**
	 * Get uploaded files list for a specific plan
	 * @param planId The plan ID
	 * @return List of uploaded files
	 */
	@GetMapping("/files/{planId}")
	public ResponseEntity<Map<String, Object>> getUploadedFiles(@PathVariable String planId) {
		try {
			Path planDirectory = directoryManager.getRootPlanDirectory(planId);
			Path uploadsDirectory = planDirectory.resolve("uploads");

			Map<String, Object> response = new HashMap<>();
			List<Map<String, Object>> files = new ArrayList<>();

			if (Files.exists(uploadsDirectory)) {
				Files.list(uploadsDirectory).filter(Files::isRegularFile).forEach(file -> {
					try {
						Map<String, Object> fileInfo = new HashMap<>();
						fileInfo.put("fileName", file.getFileName().toString());
						fileInfo.put("size", Files.size(file));
						fileInfo.put("lastModified", Files.getLastModifiedTime(file).toString());
						fileInfo.put("relativePath", "uploads/" + file.getFileName().toString());
						fileInfo.put("absolutePath", file.toString());
						files.add(fileInfo);
					}
					catch (IOException e) {
						logger.warn("Error reading file info: {}", file, e);
					}
				});
			}

			response.put("planId", planId);
			response.put("files", files);
			response.put("totalCount", files.size());

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error getting uploaded files for planId: {}", planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Failed to get uploaded files: " + e.getMessage()));
		}
	}

	/**
	 * Delete a specific uploaded file
	 * @param planId The plan ID
	 * @param fileName The file name to delete
	 * @return Deletion result
	 */
	@DeleteMapping("/files/{planId}/{fileName}")
	public ResponseEntity<Map<String, Object>> deleteUploadedFile(@PathVariable String planId,
			@PathVariable String fileName) {
		try {
			Path planDirectory = directoryManager.getRootPlanDirectory(planId);
			Path filePath = planDirectory.resolve("uploads").resolve(fileName);

			if (Files.exists(filePath)) {
				Files.delete(filePath);
				logger.info("Deleted uploaded file: {} for planId: {}", fileName, planId);
				return ResponseEntity
					.ok(Map.of("message", "File deleted successfully", "fileName", fileName, "planId", planId));
			}
			else {
				return ResponseEntity.notFound().build();
			}

		}
		catch (Exception e) {
			logger.error("Error deleting file {} for planId: {}", fileName, planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Failed to delete file: " + e.getMessage()));
		}
	}

	/**
	 * Process individual file upload
	 */
	private Map<String, Object> processFileUpload(MultipartFile file, Path uploadDirectory) throws IOException {
		Map<String, Object> result = new HashMap<>();

		// Validate file
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.trim().isEmpty()) {
			result.put("error", "Invalid file name");
			return result;
		}

		// Check file extension
		String extension = getFileExtension(originalFilename);
		if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
			result.put("error", "Unsupported file type: " + extension + " for file: " + originalFilename);
			return result;
		}

		// Generate unique filename to avoid conflicts
		String uniqueFilename = generateUniqueFilename(originalFilename, uploadDirectory);
		Path targetFile = uploadDirectory.resolve(uniqueFilename);

		// Save file
		Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

		// Prepare result
		result.put("originalName", originalFilename);
		result.put("savedName", uniqueFilename);
		result.put("size", file.getSize());
		result.put("extension", extension);
		result.put("uploadTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		result.put("relativePath", "uploads/" + uniqueFilename);
		result.put("absolutePath", targetFile.toString());

		return result;
	}

	/**
	 * Get file extension
	 */
	private String getFileExtension(String filename) {
		int lastDotIndex = filename.lastIndexOf('.');
		return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
	}

	/**
	 * Generate unique filename to avoid conflicts
	 */
	private String generateUniqueFilename(String originalFilename, Path uploadDirectory) {
		String baseName = originalFilename;
		String extension = "";

		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex > 0) {
			baseName = originalFilename.substring(0, lastDotIndex);
			extension = originalFilename.substring(lastDotIndex);
		}

		String uniqueFilename = originalFilename;
		int counter = 1;

		while (Files.exists(uploadDirectory.resolve(uniqueFilename))) {
			uniqueFilename = baseName + "_" + counter + extension;
			counter++;
		}

		return uniqueFilename;
	}

}
