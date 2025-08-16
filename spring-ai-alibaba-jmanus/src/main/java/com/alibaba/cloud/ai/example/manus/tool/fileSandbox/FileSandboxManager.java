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

import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File Sandbox Manager - Core service for managing file sandboxes Provides secure file
 * storage, validation, and processing capabilities
 *
 * Unified Directory Structure: - Uploaded files are stored directly in the plan root
 * directory for FileBrowser compatibility - Processed files go to the 'processed'
 * subdirectory - Temporary files go to the 'temp' subdirectory - This ensures FileBrowser
 * can display all uploaded files in the same view
 */
@Service
public class FileSandboxManager {

	private static final Logger log = LoggerFactory.getLogger(FileSandboxManager.class);

	// Directory structure constants for unified file management
	private static final String PROCESSED_DIR = "processed"; // For AI-generated files

	private static final String TEMP_DIR = "temp"; // For temporary processing files

	private static final String SHARED_DIR = "shared"; // For user uploaded files without
														// planId

	// Supported file types for security
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".csv", ".xlsx", ".xls", ".json", ".xml",
			".md", ".pdf", ".docx", ".doc", ".pptx", ".ppt", ".zip");

	private static final Set<String> TEXT_EXTENSIONS = Set.of(".txt", ".csv", ".json", ".xml", ".md");

	private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

	private static final int MAX_FILES_PER_SANDBOX = 100;

	private final UnifiedDirectoryManager directoryManager;

	private final Map<String, SandboxInstance> activeSandboxes = new ConcurrentHashMap<>();

	public FileSandboxManager(UnifiedDirectoryManager directoryManager) {
		this.directoryManager = directoryManager;
	}

	/**
	 * Initialize or get existing sandbox for a plan
	 */
	public SandboxInstance initializeSandbox(String planId) throws IOException {
		SandboxInstance sandbox = activeSandboxes.get(planId);
		if (sandbox == null) {
			sandbox = createNewSandbox(planId);
			activeSandboxes.put(planId, sandbox);
		}
		return sandbox;
	}

	/**
	 * Get shared directory path for files uploaded without planId
	 */
	public Path getSharedDirectory() throws IOException {
		Path sharedDir = directoryManager.getWorkingDirectory().resolve(SHARED_DIR);
		directoryManager.ensureDirectoryExists(sharedDir);
		return sharedDir;
	}

	/**
	 * Store uploaded file in shared directory (without planId)
	 */
	public SandboxFile storeUploadedFileToShared(String fileName, byte[] content, String mimeType) throws IOException {
		// Validate file
		validateFile(fileName, content.length);

		// Create unique file name to avoid conflicts
		String uniqueFileName = generateUniqueFileName(fileName);
		Path sharedDir = getSharedDirectory();
		Path uploadPath = sharedDir.resolve(uniqueFileName);

		// Write file
		Files.write(uploadPath, content);

		// Create file metadata
		SandboxFile sandboxFile = new SandboxFile();
		sandboxFile.setName(uniqueFileName);
		sandboxFile.setOriginalName(fileName);
		sandboxFile.setType(getFileType(fileName));
		sandboxFile.setSize(content.length);
		sandboxFile.setMimeType(mimeType);
		sandboxFile.setUploadTime(LocalDateTime.now());
		sandboxFile.setStatus("uploaded");
		sandboxFile.setPath(uploadPath);

		log.info("File stored in shared directory: fileName={}, size={}", uniqueFileName, content.length);

		return sandboxFile;
	}

	/**
	 * Store uploaded file in sandbox
	 */
	public SandboxFile storeUploadedFile(String planId, String fileName, byte[] content, String mimeType)
			throws IOException {
		// Validate file
		validateFile(fileName, content.length);

		SandboxInstance sandbox = initializeSandbox(planId);

		// Create unique file name to avoid conflicts
		String uniqueFileName = generateUniqueFileName(fileName);
		Path uploadPath = sandbox.getUploadsDir().resolve(uniqueFileName);

		// Ensure directory exists
		directoryManager.ensureDirectoryExists(sandbox.getUploadsDir());

		// Write file
		Files.write(uploadPath, content);

		// Create file metadata
		SandboxFile sandboxFile = new SandboxFile();
		sandboxFile.setName(uniqueFileName);
		sandboxFile.setOriginalName(fileName);
		sandboxFile.setType(getFileType(fileName));
		sandboxFile.setSize(content.length);
		sandboxFile.setMimeType(mimeType);
		sandboxFile.setUploadTime(LocalDateTime.now());
		sandboxFile.setStatus("uploaded");
		sandboxFile.setPath(uploadPath);

		// Store in sandbox
		sandbox.addFile(sandboxFile);

		log.info("File stored in sandbox: planId={}, fileName={}, size={}", planId, uniqueFileName, content.length);

		return sandboxFile;
	}

	/**
	 * List all files in shared directory
	 */
	public List<SandboxFile> listSharedFiles() throws IOException {
		Path sharedDir = getSharedDirectory();
		List<SandboxFile> files = new ArrayList<>();

		if (Files.exists(sharedDir)) {
			try (var stream = Files.list(sharedDir)) {
				stream.filter(Files::isRegularFile).forEach(filePath -> {
					try {
						String fileName = filePath.getFileName().toString();
						SandboxFile sandboxFile = new SandboxFile();
						sandboxFile.setName(fileName);
						sandboxFile.setOriginalName(fileName); // For shared files, use
																// the same name
						sandboxFile.setType(getFileType(fileName));
						sandboxFile.setSize(Files.size(filePath));
						sandboxFile.setMimeType(Files.probeContentType(filePath));
						sandboxFile.setUploadTime(LocalDateTime.now()); // Approximate
						sandboxFile.setStatus("shared");
						sandboxFile.setPath(filePath);
						files.add(sandboxFile);
					}
					catch (IOException e) {
						log.warn("Error reading shared file: {}", filePath, e);
					}
				});
			}
		}

		return files;
	}

	/**
	 * List all files in sandbox
	 */
	public List<SandboxFile> listFiles(String planId) throws IOException {
		SandboxInstance sandbox = activeSandboxes.get(planId);
		if (sandbox == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(sandbox.getFiles().values());
	}

	/**
	 * Read file content from shared directory
	 */
	public String readSharedFile(String fileName) throws IOException {
		Path sharedDir = getSharedDirectory();
		Path filePath = sharedDir.resolve(fileName);

		if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
			throw new IOException("File not found in shared directory: " + fileName);
		}

		// Only allow reading text files directly
		if (!isTextFile(fileName)) {
			throw new IOException("Cannot read binary file as text: " + fileName);
		}

		return Files.readString(filePath);
	}

	/**
	 * Read file content from sandbox
	 */
	public String readFile(String planId, String fileName) throws IOException {
		SandboxInstance sandbox = activeSandboxes.get(planId);
		if (sandbox == null) {
			throw new IOException("Sandbox not found for plan: " + planId);
		}

		SandboxFile file = sandbox.getFile(fileName);
		if (file == null) {
			throw new IOException("File not found: " + fileName);
		}

		// Only allow reading text files directly
		if (!isTextFile(file.getName())) {
			throw new IOException("Cannot read binary file as text: " + fileName);
		}

		return Files.readString(file.getPath());
	}

	/**
	 * Get shared file information
	 */
	public SandboxFile getSharedFileInfo(String fileName) throws IOException {
		Path sharedDir = getSharedDirectory();
		Path filePath = sharedDir.resolve(fileName);

		if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
			throw new IOException("File not found in shared directory: " + fileName);
		}

		SandboxFile sandboxFile = new SandboxFile();
		sandboxFile.setName(fileName);
		sandboxFile.setOriginalName(fileName);
		sandboxFile.setType(getFileType(fileName));
		sandboxFile.setSize(Files.size(filePath));
		sandboxFile.setMimeType(Files.probeContentType(filePath));
		sandboxFile.setUploadTime(LocalDateTime.now()); // Approximate
		sandboxFile.setStatus("shared");
		sandboxFile.setPath(filePath);

		return sandboxFile;
	}

	/**
	 * Get file information
	 */
	public SandboxFile getFileInfo(String planId, String fileName) throws IOException {
		SandboxInstance sandbox = activeSandboxes.get(planId);
		if (sandbox == null) {
			throw new IOException("Sandbox not found for plan: " + planId);
		}

		SandboxFile file = sandbox.getFile(fileName);
		if (file == null) {
			throw new IOException("File not found: " + fileName);
		}

		return file;
	}

	/**
	 * Process file with specified operation
	 */
	public String processFile(String planId, String fileName, String operation, Map<String, Object> parameters)
			throws IOException {
		SandboxInstance sandbox = activeSandboxes.get(planId);
		if (sandbox == null) {
			throw new IOException("Sandbox not found for plan: " + planId);
		}

		SandboxFile file = sandbox.getFile(fileName);
		if (file == null) {
			throw new IOException("File not found: " + fileName);
		}

		switch (operation) {
			case "parse":
				return parseFile(file);
			case "analyze":
				return analyzeFile(file);
			case "extract":
				return extractContent(file);
			case "convert":
				return convertFile(file, parameters);
			default:
				throw new IOException("Unsupported operation: " + operation);
		}
	}

	/**
	 * Create new file in sandbox
	 */
	public void createFile(String planId, String fileName, String content) throws IOException {
		SandboxInstance sandbox = initializeSandbox(planId);

		// Validate
		if (sandbox.getFiles().size() >= MAX_FILES_PER_SANDBOX) {
			throw new IOException("Maximum number of files exceeded in sandbox");
		}

		// Save created files to the plan root directory to be visible in FileBrowser
		Path filePath = sandbox.getUploadsDir().resolve(fileName); // uploads dir now
																	// points to plan root
		directoryManager.ensureDirectoryExists(sandbox.getUploadsDir());

		Files.writeString(filePath, content);

		// Create file metadata
		SandboxFile sandboxFile = new SandboxFile();
		sandboxFile.setName(fileName);
		sandboxFile.setOriginalName(fileName);
		sandboxFile.setType(getFileType(fileName));
		sandboxFile.setSize(content.getBytes().length);
		sandboxFile.setMimeType("text/plain");
		sandboxFile.setUploadTime(LocalDateTime.now());
		sandboxFile.setStatus("created");
		sandboxFile.setPath(filePath);

		sandbox.addFile(sandboxFile);

		log.info("File created in unified directory: planId={}, fileName={}", planId, fileName);
	}

	/**
	 * Delete file from shared directory
	 */
	public boolean deleteSharedFile(String fileName) throws IOException {
		Path sharedDir = getSharedDirectory();
		Path filePath = sharedDir.resolve(fileName);

		if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
			Files.delete(filePath);
			log.info("Deleted shared file: {}", fileName);
			return true;
		}

		log.warn("Shared file not found or not a regular file: {}", fileName);
		return false;
	}

	/**
	 * Cleanup sandbox for plan
	 */
	public void cleanupSandbox(String planId) throws IOException {
		SandboxInstance sandbox = activeSandboxes.remove(planId);
		if (sandbox != null) {
			// Clean up files
			if (Files.exists(sandbox.getSandboxRoot())) {
				deleteSandboxDirectory(sandbox.getSandboxRoot());
			}
			log.info("Sandbox cleaned up for plan: {}", planId);
		}
	}

	// Private helper methods

	private SandboxInstance createNewSandbox(String planId) throws IOException {
		// Use the same root directory as FileBrowser for unified file management
		Path planDir = directoryManager.getRootPlanDirectory(planId);
		Path sandboxRoot = planDir; // Use plan directory directly instead of sandbox
									// subdirectory

		// Create sandbox directories under plan root for better organization
		Path uploadsDir = planDir; // Save uploaded files directly to plan root directory
									// for FileBrowser compatibility
		Path processedDir = sandboxRoot.resolve(PROCESSED_DIR);
		Path tempDir = sandboxRoot.resolve(TEMP_DIR);

		directoryManager.ensureDirectoryExists(uploadsDir);
		directoryManager.ensureDirectoryExists(processedDir);
		directoryManager.ensureDirectoryExists(tempDir);

		SandboxInstance sandbox = new SandboxInstance(planId, sandboxRoot, uploadsDir, processedDir, tempDir);

		log.info("Created new sandbox for plan: {} with unified directory structure", planId);
		return sandbox;
	}

	private void validateFile(String fileName, long size) throws IOException {
		// Check file size
		if (size > MAX_FILE_SIZE) {
			throw new IOException("File too large: " + size + " bytes (max: " + MAX_FILE_SIZE + ")");
		}

		// Check file extension
		String extension = getFileExtension(fileName).toLowerCase();
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new IOException("File type not allowed: " + extension);
		}
	}

	private String generateUniqueFileName(String originalName) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String extension = getFileExtension(originalName);
		String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
		return baseName + "_" + timestamp + extension;
	}

	private String getFileExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		return lastDot > 0 ? fileName.substring(lastDot) : "";
	}

	private String getFileType(String fileName) {
		String extension = getFileExtension(fileName).toLowerCase();
		switch (extension) {
			case ".txt":
			case ".md":
				return "text";
			case ".csv":
				return "csv";
			case ".xlsx":
			case ".xls":
				return "excel";
			case ".json":
				return "json";
			case ".xml":
				return "xml";
			case ".pdf":
				return "pdf";
			case ".docx":
			case ".doc":
				return "document";
			case ".pptx":
			case ".ppt":
				return "presentation";
			case ".zip":
				return "archive";
			default:
				return "unknown";
		}
	}

	private boolean isTextFile(String fileName) {
		String extension = getFileExtension(fileName).toLowerCase();
		return TEXT_EXTENSIONS.contains(extension);
	}

	private String parseFile(SandboxFile file) throws IOException {
		switch (file.getType()) {
			case "csv":
				return parseCsvFile(file);
			case "json":
				return parseJsonFile(file);
			case "xml":
				return parseXmlFile(file);
			default:
				return "File type " + file.getType() + " does not support parsing";
		}
	}

	private String analyzeFile(SandboxFile file) throws IOException {
		StringBuilder analysis = new StringBuilder();
		analysis.append("File Analysis for: ").append(file.getName()).append("\n");
		analysis.append("Type: ").append(file.getType()).append("\n");
		analysis.append("Size: ").append(file.getSize()).append(" bytes\n");
		analysis.append("Upload Time: ").append(file.getUploadTime()).append("\n");

		if (isTextFile(file.getName())) {
			String content = Files.readString(file.getPath());
			analysis.append("Lines: ").append(content.split("\n").length).append("\n");
			analysis.append("Characters: ").append(content.length()).append("\n");
		}

		return analysis.toString();
	}

	private String extractContent(SandboxFile file) throws IOException {
		if (isTextFile(file.getName())) {
			return Files.readString(file.getPath());
		}
		else {
			return "Cannot extract content from binary file: " + file.getName();
		}
	}

	private String convertFile(SandboxFile file, Map<String, Object> parameters) throws IOException {
		String targetFormat = parameters != null ? (String) parameters.get("format") : null;
		if (targetFormat == null) {
			throw new IOException("Target format not specified for conversion");
		}

		// Simple conversion example (can be extended)
		if ("json".equals(file.getType()) && "text".equals(targetFormat)) {
			String jsonContent = Files.readString(file.getPath());
			return "Converted JSON to text:\n" + jsonContent;
		}

		return "Conversion from " + file.getType() + " to " + targetFormat + " not supported";
	}

	private String parseCsvFile(SandboxFile file) throws IOException {
		String content = Files.readString(file.getPath());
		String[] lines = content.split("\n");

		StringBuilder result = new StringBuilder();
		result.append("CSV File Structure:\n");
		result.append("Total rows: ").append(lines.length).append("\n");

		if (lines.length > 0) {
			result.append("Headers: ").append(lines[0]).append("\n");
			result.append("Sample data (first 3 rows):\n");
			for (int i = 0; i < Math.min(3, lines.length); i++) {
				result.append("Row ").append(i + 1).append(": ").append(lines[i]).append("\n");
			}
		}

		return result.toString();
	}

	private String parseJsonFile(SandboxFile file) throws IOException {
		String content = Files.readString(file.getPath());
		return "JSON File Content Preview:\n" + content.substring(0, Math.min(500, content.length()));
	}

	private String parseXmlFile(SandboxFile file) throws IOException {
		String content = Files.readString(file.getPath());
		return "XML File Content Preview:\n" + content.substring(0, Math.min(500, content.length()));
	}

	private void deleteSandboxDirectory(Path directory) throws IOException {
		Files.walk(directory).sorted((path1, path2) -> path2.compareTo(path1)).forEach(path -> {
			try {
				Files.delete(path);
			}
			catch (IOException e) {
				log.warn("Failed to delete: {}", path, e);
			}
		});
	}

}
