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
package com.alibaba.cloud.ai.example.manus.tool.fileSandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File Sandbox Controller - Handles file upload and management for sandbox
 */
@RestController
@RequestMapping("/api/file-sandbox")
@CrossOrigin(origins = "*")
public class FileSandboxController {

	private static final Logger logger = LoggerFactory.getLogger(FileSandboxController.class);

	@Autowired
	private FileSandboxManager sandboxManager;

	/**
	 * Upload file to sandbox
	 */
	@PostMapping("/upload/{planId}")
	public ResponseEntity<?> uploadFile(@PathVariable("planId") String planId,
			@RequestParam("file") MultipartFile file) {
		try {
			logger.info("Uploading file to sandbox: planId={}, fileName={}, size={}", planId,
					file.getOriginalFilename(), file.getSize());

			// Validate file
			if (file.isEmpty()) {
				return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
			}

			// Store file in sandbox
			SandboxFile sandboxFile = sandboxManager.storeUploadedFile(planId, file.getOriginalFilename(),
					file.getBytes(), file.getContentType());

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "File uploaded successfully");
			response.put("file", createFileResponse(sandboxFile));

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error uploading file to sandbox: planId={}, fileName={}", planId, file.getOriginalFilename(),
					e);
			return ResponseEntity.internalServerError().body(createErrorResponse("Upload failed: " + e.getMessage()));
		}
	}

	/**
	 * Upload multiple files to sandbox
	 */
	@PostMapping("/upload-multiple/{planId}")
	public ResponseEntity<?> uploadMultipleFiles(@PathVariable("planId") String planId,
			@RequestParam("files") MultipartFile[] files) {
		try {
			logger.info("Uploading {} files to sandbox: planId={}", files.length, planId);

			if (files.length == 0) {
				return ResponseEntity.badRequest().body(createErrorResponse("No files provided"));
			}

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Files uploaded successfully");
			response.put("uploadedFiles", java.util.Arrays.stream(files).map(file -> {
				try {
					SandboxFile sandboxFile = sandboxManager.storeUploadedFile(planId, file.getOriginalFilename(),
							file.getBytes(), file.getContentType());
					return createFileResponse(sandboxFile);
				}
				catch (Exception e) {
					logger.error("Error uploading file: {}", file.getOriginalFilename(), e);
					Map<String, Object> errorFile = new HashMap<>();
					errorFile.put("originalName", file.getOriginalFilename());
					errorFile.put("error", e.getMessage());
					return errorFile;
				}
			}).toList());

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error uploading multiple files to sandbox: planId={}", planId, e);
			return ResponseEntity.internalServerError().body(createErrorResponse("Upload failed: " + e.getMessage()));
		}
	}

	/**
	 * List files in sandbox
	 */
	@GetMapping("/files/{planId}")
	public ResponseEntity<?> listFiles(@PathVariable("planId") String planId) {
		try {
			List<SandboxFile> files = sandboxManager.listFiles(planId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("files", files.stream().map(this::createFileResponse).toList());

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error listing files in sandbox: planId={}", planId, e);
			return ResponseEntity.internalServerError()
				.body(createErrorResponse("Failed to list files: " + e.getMessage()));
		}
	}

	/**
	 * Get file information
	 */
	@GetMapping("/file-info/{planId}/{fileName}")
	public ResponseEntity<?> getFileInfo(@PathVariable("planId") String planId,
			@PathVariable("fileName") String fileName) {
		try {
			SandboxFile file = sandboxManager.getFileInfo(planId, fileName);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("file", createFileResponse(file));

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error getting file info: planId={}, fileName={}", planId, fileName, e);
			return ResponseEntity.internalServerError()
				.body(createErrorResponse("Failed to get file info: " + e.getMessage()));
		}
	}

	/**
	 * Delete sandbox for plan
	 */
	@DeleteMapping("/cleanup/{planId}")
	public ResponseEntity<?> cleanupSandbox(@PathVariable("planId") String planId) {
		try {
			sandboxManager.cleanupSandbox(planId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Sandbox cleaned up successfully");

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			logger.error("Error cleaning up sandbox: planId={}", planId, e);
			return ResponseEntity.internalServerError()
				.body(createErrorResponse("Failed to cleanup sandbox: " + e.getMessage()));
		}
	}

	// Helper methods

	private Map<String, Object> createFileResponse(SandboxFile file) {
		Map<String, Object> fileInfo = new HashMap<>();
		fileInfo.put("name", file.getName());
		fileInfo.put("originalName", file.getOriginalName());
		fileInfo.put("type", file.getType());
		fileInfo.put("size", file.getSize());
		fileInfo.put("mimeType", file.getMimeType());
		fileInfo.put("uploadTime", file.getUploadTime().toString());
		fileInfo.put("status", file.getStatus());
		return fileInfo;
	}

	private Map<String, Object> createErrorResponse(String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", message);
		return response;
	}

}
