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
package com.alibaba.cloud.ai.example.manus.tool.uploadedFileLoader;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.model.ToolContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for processing uploaded files in a plan. Supports various file types including
 * PDF, text files, CSV, etc. Provides intelligent file analysis and processing
 * recommendations.
 *
 * @author Jmanus Team
 * @version 2.0
 * @since 1.0
 */
public class UploadedFileLoaderTool extends AbstractBaseTool<UploadedFileLoaderTool.UploadedFileInput> {

	private static final Logger log = LoggerFactory.getLogger(UploadedFileLoaderTool.class);

	// Constants
	private static final String TOOL_NAME = "uploaded_file_loader";

	private static final int DEFAULT_MAX_FILES = 10;

	private static final int PREVIEW_LINES = 3;

	private static final int MAX_LINE_LENGTH = 100;

	private static final int MAX_PREVIEW_LENGTH = 200;

	private static final long LARGE_FILE_THRESHOLD = 50L * 1024 * 1024; // 50MB

	private static final long MEDIUM_FILE_THRESHOLD = 5L * 1024 * 1024; // 5MB

	// File type constants
	private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(".txt", ".md", ".csv", ".json", ".xml", ".html",
			".htm", ".log", ".java", ".py", ".js", ".ts", ".css", ".sql", ".yaml", ".yml", ".properties", ".conf",
			".ini", ".sh", ".bat");

	private static final Set<String> CODE_FILE_EXTENSIONS = Set.of(".java", ".py", ".js", ".ts", ".css", ".sql", ".sh",
			".bat");

	private static final Set<String> DATA_FILE_EXTENSIONS = Set.of(".csv", ".xlsx", ".xls", ".json", ".xml");

	private final UnifiedDirectoryManager directoryManager;

	private final SmartContentSavingService smartContentSavingService;

	private final ObjectMapper objectMapper;

	/**
	 * Constructor for manual instantiation
	 * @param directoryManager the directory manager for file operations
	 * @param smartContentSavingService the service for handling large content
	 */
	public UploadedFileLoaderTool(UnifiedDirectoryManager directoryManager,
			SmartContentSavingService smartContentSavingService) {
		this.directoryManager = Objects.requireNonNull(directoryManager, "DirectoryManager cannot be null");
		this.smartContentSavingService = Objects.requireNonNull(smartContentSavingService,
				"SmartContentSavingService cannot be null");
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Input class for uploaded file operations
	 */
	public static class UploadedFileInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_name")
		private String fileName;

		@com.fasterxml.jackson.annotation.JsonProperty("file_pattern")
		private String filePattern;

		@com.fasterxml.jackson.annotation.JsonProperty("max_files")
		private Integer maxFiles;

		// Constructors
		public UploadedFileInput() {
		}

		public UploadedFileInput(String action, String fileName) {
			this.action = action;
			this.fileName = fileName;
		}

		// Getters and setters
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

		public String getFilePattern() {
			return filePattern;
		}

		public void setFilePattern(String filePattern) {
			this.filePattern = filePattern;
		}

		public Integer getMaxFiles() {
			return maxFiles;
		}

		public void setMaxFiles(Integer maxFiles) {
			this.maxFiles = maxFiles;
		}

	}

	// Tool name is now defined as constant at the top of the class

	public OpenAiApi.FunctionTool getToolDefinition() {
		String description = getDescription();
		String parameters = getParameters();
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Get FunctionToolCallback for Spring AI
	 */
	public static FunctionToolCallback<UploadedFileInput, ToolExecuteResult> getFunctionToolCallback(
			UnifiedDirectoryManager directoryManager, SmartContentSavingService smartContentSavingService) {
		return FunctionToolCallback
			.<UploadedFileInput, ToolExecuteResult>builder(TOOL_NAME,
					(UploadedFileInput input,
							ToolContext context) -> new UploadedFileLoaderTool(directoryManager,
									smartContentSavingService)
								.run(input))
			.description("""
					Intelligent file loading and analysis tool for processing various types of uploaded files.

					Available actions:
					- list_files: List all uploaded files
					- load_file: Load specific file content
					- load_multiple: Load multiple files matching pattern
					- process_all: Process all uploaded files and return merged content
					- smart_analyze: Smart analysis with automatic file type detection and tool recommendations

					Use this tool to access and intelligently analyze user uploaded files.
					""")
			.inputType(UploadedFileInput.class)
			.build();
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return """
				Intelligent file loading and analysis tool. Processes various types of uploaded files, provides smart analysis and tool recommendations.
				Supports automatic recognition and processing of PDF, text, spreadsheet, code and other file formats.
				""";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "action": {
				            "type": "string",
				            "description": "(required) Action to perform. Options: list_files, load_file, load_multiple, process_all, smart_analyze (intelligent analysis with automatic tool selection)",
				            "enum": ["list_files", "load_file", "load_multiple", "process_all", "smart_analyze"]
				        },
				        "file_name": {
				            "type": "string",
				            "description": "(optional) Name of specific file to load. Required for load_file action."
				        },
				        "file_pattern": {
				            "type": "string",
				            "description": "(optional) Pattern to match multiple files. Required for load_multiple action."
				        },
				        "max_files": {
				            "type": "integer",
				            "description": "(optional) Maximum number of files to process. Default is 10."
				        }
				    },
				    "required": ["action"]
				}
				""";
	}

	@Override
	public Class<UploadedFileInput> getInputType() {
		return UploadedFileInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getCurrentToolStateString() {
		if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
			log.warn("getCurrentToolStateString called with null or empty planId");
			return "Uploaded Files State: No plan ID available";
		}

		log.debug("🔍 getCurrentToolStateString called for planId: {}", currentPlanId);

		try {
			Path uploadsDir = directoryManager.getRootPlanDirectory(currentPlanId).resolve("uploads");

			if (!Files.exists(uploadsDir)) {
				log.debug("No uploads directory found for plan: {}", currentPlanId);
				return "Uploaded Files State: No uploads directory found for plan " + currentPlanId;
			}

			long fileCount;
			try (var stream = Files.list(uploadsDir)) {
				fileCount = stream.filter(Files::isRegularFile).count();
			}

			log.debug("📁 Found {} files in uploads directory for plan {}", fileCount, currentPlanId);

			if (fileCount > 0) {
				String result = String.format("""
						Uploaded files available: %d files found in plan %s

						🔧 To access these files, you must call the 'uploaded_file_loader' tool:
						- Use action "list_files" to view available files
						- Use action "smart_analyze" for automatic file analysis
						- Use action "process_all" to load all file contents

						Example: Call uploaded_file_loader with {"action": "list_files"}
						""", fileCount, currentPlanId);
				log.debug("🎯 Returning tool state with {} files", fileCount);
				return result;
			}
			else {
				log.debug("No files found in uploads directory for plan: {}", currentPlanId);
				return "Uploaded Files State: No files in uploads directory for plan " + currentPlanId;
			}
		}
		catch (IOException e) {
			log.error("💥 IO error reading uploads directory for plan {}: {}", currentPlanId, e.getMessage(), e);
			return "Uploaded Files State: IO error reading uploads directory - " + e.getMessage();
		}
		catch (Exception e) {
			log.error("💥 Unexpected error in getCurrentToolStateString for plan {}: {}", currentPlanId, e.getMessage(),
					e);
			return "Uploaded Files State: Unexpected error - " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		log.info("Cleaned up UploadedFileLoaderTool for plan: {}", planId);
	}

	@Override
	public ToolExecuteResult run(UploadedFileInput input) {
		String action = input.getAction();
		log.info("UploadedFileLoaderTool action: {}, planId: {}", action, currentPlanId);

		try {
			return switch (action) {
				case "list_files" -> listUploadedFiles();
				case "load_file" -> loadSingleFile(input.getFileName());
				case "load_multiple" -> loadMultipleFiles(input.getFilePattern(), input.getMaxFiles());
				case "process_all" -> processAllUploadedFiles();
				case "smart_analyze" -> smartAnalyzeAllFiles();
				default -> new ToolExecuteResult("Unknown action: " + action
						+ ". Supported actions: list_files, load_file, load_multiple, process_all, smart_analyze");
			};
		}
		catch (Exception e) {
			log.error("Error executing uploaded file loader tool", e);
			return new ToolExecuteResult("Error: " + e.getMessage());
		}
	}

	/**
	 * List all uploaded files for the current plan with enhanced error handling
	 */
	private ToolExecuteResult listUploadedFiles() {
		try {
			Path planDirectory = directoryManager.getRootPlanDirectory(currentPlanId);
			Path uploadsDirectory = planDirectory.resolve("uploads");

			if (!Files.exists(uploadsDirectory)) {
				log.info("No uploads directory found for plan: {}", currentPlanId);
				return new ToolExecuteResult("No uploaded files found for current plan");
			}

			Map<String, Object> result = new HashMap<>();
			result.put("planId", currentPlanId);
			result.put("uploadsDirectory", uploadsDirectory.toString());

			List<Map<String, Object>> fileList;
			try (var stream = Files.list(uploadsDirectory)) {
				fileList = stream.filter(Files::isRegularFile)
					.map(this::createFileInfoMap)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}

			result.put("files", fileList);
			result.put("totalCount", fileList.size());

			return new ToolExecuteResult("📁 Uploaded Files List:\n"
					+ objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

		}
		catch (IOException e) {
			log.error("IO error listing uploaded files for plan {}: {}", currentPlanId, e.getMessage(), e);
			return new ToolExecuteResult("Error: Unable to read uploads directory - " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error listing uploaded files for plan {}: {}", currentPlanId, e.getMessage(), e);
			return new ToolExecuteResult("Error: Failed to list files - " + e.getMessage());
		}
	}

	/**
	 * Create file info map with proper error handling
	 */
	private Map<String, Object> createFileInfoMap(Path file) {
		Map<String, Object> fileInfo = new HashMap<>();
		try {
			fileInfo.put("name", file.getFileName().toString());
			fileInfo.put("size", Files.size(file));
			fileInfo.put("sizeFormatted", formatFileSize(Files.size(file)));
			fileInfo.put("type", getFileType(file));
			fileInfo.put("extension", getFileExtension(file.getFileName().toString()));
			fileInfo.put("lastModified", Files.getLastModifiedTime(file).toString());
			fileInfo.put("recommendedTool", getRecommendedTool(getFileExtension(file.getFileName().toString())));
			return fileInfo;
		}
		catch (IOException e) {
			log.warn("Error reading file info for {}: {}", file, e.getMessage());
			return null; // Will be filtered out
		}
	}

	/**
	 * Load a specific uploaded file with enhanced error handling and validation
	 */
	private ToolExecuteResult loadSingleFile(String fileName) {
		if (StringUtils.isEmpty(fileName)) {
			log.warn("loadSingleFile called with empty fileName");
			return new ToolExecuteResult("Error: file_name parameter is required");
		}

		try {
			Path planDirectory = directoryManager.getRootPlanDirectory(currentPlanId);
			Path filePath = planDirectory.resolve("uploads").resolve(fileName);

			if (!Files.exists(filePath)) {
				log.warn("File not found: {} in plan: {}", fileName, currentPlanId);
				return new ToolExecuteResult("Error: File not found: " + fileName);
			}

			if (!Files.isRegularFile(filePath)) {
				log.warn("Path is not a regular file: {}", filePath);
				return new ToolExecuteResult("Error: Specified path is not a regular file: " + fileName);
			}

			long fileSize = Files.size(filePath);
			log.info("Loading file: {} (size: {})", fileName, formatFileSize(fileSize));

			String fileContent = loadFileContent(filePath);

			if (fileContent == null || fileContent.trim().isEmpty()) {
				log.warn("File content is empty or null: {}", fileName);
				return new ToolExecuteResult("Warning: File '" + fileName + "' is empty or unreadable");
			}

			// Use smart content saving service to handle large files
			var smartResult = smartContentSavingService.processContent(currentPlanId, fileContent,
					"uploaded_file_loader");

			if (smartResult.getFileName() != null) {
				return new ToolExecuteResult(String.format(
						"📄 File '%s' loaded and processed successfully. Content saved to storage: %s\n📊 Summary: %s",
						fileName, smartResult.getFileName(), smartResult.getSummary()));
			}
			else {
				return new ToolExecuteResult(
						String.format("✅ File '%s' loaded successfully:\n%s", fileName, smartResult.getSummary()));
			}

		}
		catch (IOException e) {
			log.error("IO error loading file {}: {}", fileName, e.getMessage(), e);
			return new ToolExecuteResult("Error: Unable to load file '" + fileName + "' - " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error loading file {}: {}", fileName, e.getMessage(), e);
			return new ToolExecuteResult("Error: Failed to load file - " + e.getMessage());
		}
	}

	/**
	 * Load multiple files based on pattern with enhanced validation and error handling
	 */
	private ToolExecuteResult loadMultipleFiles(String filePattern, Integer maxFiles) {
		try {
			Path planDirectory = directoryManager.getRootPlanDirectory(currentPlanId);
			Path uploadsDirectory = planDirectory.resolve("uploads");

			if (!Files.exists(uploadsDirectory)) {
				log.info("No uploads directory found for plan: {}", currentPlanId);
				return new ToolExecuteResult("No uploaded files found for current plan");
			}

			int limit = maxFiles != null && maxFiles > 0 ? Math.min(maxFiles, 100) : DEFAULT_MAX_FILES;
			StringBuilder combinedContent = new StringBuilder();
			int processedCount = 0;
			int errorCount = 0;

			List<Path> matchingFiles;
			try (var stream = Files.list(uploadsDirectory)) {
				matchingFiles = stream.filter(Files::isRegularFile)
					.filter(file -> filePattern == null || file.getFileName().toString().contains(filePattern))
					.limit(limit)
					.collect(Collectors.toList());
			}

			log.info("Found {} matching files for pattern: {}", matchingFiles.size(), filePattern);

			for (Path file : matchingFiles) {
				try {
					String fileName = file.getFileName().toString();
					String content = loadFileContent(file);

					if (content != null && !content.trim().isEmpty()) {
						combinedContent.append("📄 === File: ").append(fileName).append(" ===\n");
						combinedContent.append(content);
						combinedContent.append("\n\n");
						processedCount++;
					}
					else {
						log.warn("File {} is empty or unreadable", fileName);
						combinedContent.append("⚠️ === File: ")
							.append(fileName)
							.append(" (empty or unreadable) ===\n\n");
						errorCount++;
					}
				}
				catch (Exception e) {
					log.warn("Error loading file {}: {}", file.getFileName(), e.getMessage());
					combinedContent.append("❌ === Failed to load file: ").append(file.getFileName()).append(" ===\n");
					combinedContent.append("Error: ").append(e.getMessage()).append("\n\n");
					errorCount++;
				}
			}

			if (processedCount == 0) {
				if (errorCount > 0) {
					return new ToolExecuteResult("Error: All matching files failed to load. Pattern: " + filePattern);
				}
				else {
					return new ToolExecuteResult("No matching files found. Pattern: " + filePattern);
				}
			}

			// Use smart content saving for combined content
			var smartResult = smartContentSavingService.processContent(currentPlanId, combinedContent.toString(),
					"uploaded_file_loader_multiple");

			String resultMessage;
			if (smartResult.getFileName() != null) {
				resultMessage = String.format(
						"📊 Successfully loaded %d files with %d errors. Combined content saved to storage: %s\n📝 Summary: %s",
						processedCount, errorCount, smartResult.getFileName(), smartResult.getSummary());
			}
			else {
				resultMessage = String.format("✅ Successfully loaded %d files with %d errors:\n%s", processedCount,
						errorCount, smartResult.getSummary());
			}

			return new ToolExecuteResult(resultMessage);

		}
		catch (IOException e) {
			log.error("IO error loading multiple files: {}", e.getMessage(), e);
			return new ToolExecuteResult("Error: IO error - " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error loading multiple files: {}", e.getMessage(), e);
			return new ToolExecuteResult("Error: Failed to load multiple files - " + e.getMessage());
		}
	}

	/**
	 * Process all uploaded files
	 */
	private ToolExecuteResult processAllUploadedFiles() {
		log.info("Processing all uploaded files for plan: {}", currentPlanId);
		return loadMultipleFiles(null, null); // Load all files without pattern or limit
	}

	/**
	 * Load content from a file based on its type with enhanced error handling
	 */
	private String loadFileContent(Path filePath) throws IOException {
		if (filePath == null || !Files.exists(filePath)) {
			throw new IOException("File path is null or file does not exist: " + filePath);
		}

		String fileName = filePath.getFileName().toString();
		String extension = getFileExtension(fileName).toLowerCase();
		long fileSize = Files.size(filePath);

		log.debug("Loading file content: {} ({}), size: {}", fileName, extension, formatFileSize(fileSize));

		try {
			return switch (extension) {
				case ".pdf" -> loadPdfContent(filePath);
				case ".txt", ".md", ".csv", ".json", ".xml", ".html", ".htm", ".log", ".java", ".py", ".js", ".ts",
						".sql", ".sh", ".bat", ".yaml", ".yml", ".properties", ".conf", ".ini" -> {
					if (fileSize > LARGE_FILE_THRESHOLD) {
						log.warn("Large text file detected: {} ({}), reading may be slow", fileName,
								formatFileSize(fileSize));
					}
					yield Files.readString(filePath, StandardCharsets.UTF_8);
				}
				default -> {
					log.warn("Unsupported file type: {} for file: {}", extension, fileName);
					yield "Unsupported file type: " + extension + " (file: " + fileName + ")";
				}
			};
		}
		catch (IOException e) {
			log.error("Error reading file content for {}: {}", fileName, e.getMessage());
			throw e;
		}
	}

	/**
	 * Load PDF content using PDFBox with enhanced error handling
	 */
	private String loadPdfContent(Path filePath) throws IOException {
		String fileName = filePath.getFileName().toString();
		log.debug("Loading PDF content from: {}", fileName);

		try (PDDocument document = PDDocument.load(filePath.toFile())) {
			if (document.isEncrypted()) {
				log.warn("PDF file is encrypted: {}", fileName);
				return "Error: PDF file is encrypted and cannot extract text content.";
			}

			int pageCount = document.getNumberOfPages();
			log.debug("PDF has {} pages", pageCount);

			if (pageCount == 0) {
				return "Warning: PDF file is empty with no page content.";
			}

			PDFTextStripper pdfStripper = new PDFTextStripper();
			// For very large PDFs, consider limiting page range
			if (pageCount > 100) {
				log.warn("Large PDF detected ({} pages), this may take time to process", pageCount);
			}

			String extractedText = pdfStripper.getText(document);

			if (extractedText == null || extractedText.trim().isEmpty()) {
				return "Warning: No text content extracted from PDF, may be image-based or handwritten PDF.";
			}

			log.debug("Successfully extracted {} characters from PDF: {}", extractedText.length(), fileName);
			return extractedText;

		}
		catch (IOException e) {
			log.error("Error loading PDF content from {}: {}", fileName, e.getMessage());
			throw new IOException("Unable to load PDF file content: " + e.getMessage(), e);
		}
	}

	/**
	 * Get file type for display
	 */
	private String getFileType(Path file) {
		String extension = getFileExtension(file.getFileName().toString());
		return switch (extension.toLowerCase()) {
			case ".pdf" -> "PDF Document";
			case ".txt" -> "Text File";
			case ".md" -> "Markdown";
			case ".csv" -> "CSV Data";
			case ".json" -> "JSON Data";
			case ".xml" -> "XML Document";
			case ".html", ".htm" -> "HTML Document";
			case ".log" -> "Log File";
			case ".java" -> "Java Source";
			case ".py" -> "Python Source";
			case ".js" -> "JavaScript";
			case ".ts" -> "TypeScript";
			case ".sql" -> "SQL Script";
			default -> "Unknown";
		};
	}

	/**
	 * Smart analyze all uploaded files with automatic tool selection and enhanced
	 * reporting
	 */
	private ToolExecuteResult smartAnalyzeAllFiles() {
		try {
			Path uploadsDir = directoryManager.getRootPlanDirectory(currentPlanId).resolve("uploads");

			if (!Files.exists(uploadsDir)) {
				log.info("No uploads directory found for plan: {}", currentPlanId);
				return new ToolExecuteResult("Uploads directory not found for plan: " + currentPlanId);
			}

			List<Path> files;
			try (var stream = Files.list(uploadsDir)) {
				files = stream.filter(Files::isRegularFile)
					.sorted((f1, f2) -> f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString()))
					.collect(Collectors.toList());
			}

			if (files.isEmpty()) {
				log.info("No files found in uploads directory for plan: {}", currentPlanId);
				return new ToolExecuteResult("No files found in uploads directory");
			}

			log.info("Starting smart analysis of {} files", files.size());

			StringBuilder analysis = new StringBuilder();
			analysis.append("🤖 Smart File Analysis Report\n");
			analysis.append("=".repeat(50)).append("\n\n");
			analysis.append(String.format("📂 Analysis scope: %d files\n", files.size()));
			analysis.append(String.format("📁 Storage location: %s\n", uploadsDir));
			analysis.append(String.format("🕰️ Analysis time: %s\n\n", java.time.LocalDateTime.now().toString()));

			// Group files by type for analysis
			Map<String, List<Path>> filesByType = files.stream()
				.collect(Collectors.groupingBy(file -> getFileExtension(file.getFileName().toString()).toLowerCase()));

			analysis.append("📊 File Type Statistics:\n");
			analysis.append("-".repeat(30)).append("\n");

			// Sort by file count
			filesByType.entrySet()
				.stream()
				.sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
				.forEach(entry -> {
					String extension = entry.getKey().isEmpty() ? "No extension" : entry.getKey();
					analysis.append(String.format("  %s: %d files (%s)\n", extension, entry.getValue().size(),
							getFileTypeDescription(entry.getKey())));
				});
			analysis.append("\n");

			// Smart analysis of each file
			analysis.append("🔍 Detailed File Analysis:\n");
			analysis.append("-".repeat(40)).append("\n");

			int successCount = 0;
			int errorCount = 0;

			for (int i = 0; i < files.size(); i++) {
				Path file = files.get(i);
				try {
					String fileName = file.getFileName().toString();
					String extension = getFileExtension(fileName).toLowerCase();
					long fileSize = Files.size(file);

					analysis.append(String.format("\n📄 [%d/%d] %s\n", i + 1, files.size(), fileName));
					analysis.append(String.format("   📏 Size: %s\n", formatFileSize(fileSize)));
					analysis.append(String.format("   🏷️ Type: %s\n", getFileTypeDescription(extension)));
					analysis.append(String.format("   🛠️ Recommended Tool: %s\n", getRecommendedTool(extension)));

					// Try to read file content preview
					String contentPreview = getContentPreview(file, extension);
					if (contentPreview != null && !contentPreview.trim().isEmpty()) {
						analysis.append(String.format("   📖 Content Preview: %s\n", contentPreview.trim()));
					}

					// Smart processing recommendations
					String processingAdvice = getProcessingAdvice(extension, fileSize);
					analysis.append(String.format("   💡 Processing Advice: %s\n", processingAdvice));

					successCount++;

				}
				catch (Exception e) {
					log.warn("Error analyzing file {}: {}", file.getFileName(), e.getMessage());
					analysis.append(String.format("\n❌ [%d/%d] %s - Analysis failed: %s\n", i + 1, files.size(),
							file.getFileName(), e.getMessage()));
					errorCount++;
				}
			}

			// Generate overall recommendations
			analysis.append("\n").append("=".repeat(50)).append("\n");
			analysis.append("🎯 Overall Analysis Results:\n");
			analysis.append(String.format("✅ Successfully analyzed: %d files\n", successCount));
			analysis.append(String.format("❌ Analysis failed: %d files\n\n", errorCount));
			analysis.append(generateOverallRecommendations(filesByType, files));

			log.info("Smart analysis completed: {} success, {} errors", successCount, errorCount);
			return new ToolExecuteResult(analysis.toString());

		}
		catch (IOException e) {
			log.error("IO error during smart analysis: {}", e.getMessage(), e);
			return new ToolExecuteResult("Error: Unable to access file directory - " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during smart analysis: {}", e.getMessage(), e);
			return new ToolExecuteResult("Error: Smart analysis failed - " + e.getMessage());
		}
	}

	/**
	 * Get file extension
	 */
	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
	}

	/**
	 * Format file size in human readable format
	 */
	private String formatFileSize(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String pre = "KMGTPE".charAt(exp - 1) + "";
		return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
	}

	/**
	 * Get file type description
	 */
	private String getFileTypeDescription(String extension) {
		return switch (extension) {
			case ".pdf" -> "PDF Document";
			case ".txt" -> "Text File";
			case ".md" -> "Markdown Document";
			case ".csv" -> "CSV Data";
			case ".xlsx", ".xls" -> "Excel Spreadsheet";
			case ".docx", ".doc" -> "Word Document";
			case ".json" -> "JSON Data";
			case ".xml" -> "XML Document";
			case ".html", ".htm" -> "HTML Webpage";
			case ".log" -> "Log File";
			case ".java", ".py", ".js", ".ts" -> "Source Code";
			case ".png", ".jpg", ".jpeg", ".gif" -> "Image File";
			default -> "Unknown Type";
		};
	}

	/**
	 * Get recommended tool for file type with correct tool names
	 */
	private String getRecommendedTool(String extension) {
		return switch (extension) {
			case ".pdf" -> "doc_loader (PDF Document Loader)";
			case ".txt", ".md", ".log" -> "text_file_operator (Text File Processor)";
			case ".csv", ".xlsx", ".xls" -> "database_use (Database Analysis Tool)";
			case ".json", ".xml" -> "text_file_operator (Structured Text Parser)";
			case ".html", ".htm" -> "browser_use (Web Content Parser)";
			case ".java", ".py", ".js", ".ts" -> "text_file_operator (Code Analyzer)";
			default -> "text_file_operator (General Text Processor)";
		};
	}

	/**
	 * Get content preview for file with enhanced error handling and size limits
	 */
	private String getContentPreview(Path file, String extension) {
		String fileName = file.getFileName().toString();

		try {
			long fileSize = Files.size(file);

			// Skip preview for very large files
			if (fileSize > LARGE_FILE_THRESHOLD) {
				return String.format("File too large (%s), skipping preview", formatFileSize(fileSize));
			}

			if (".pdf".equals(extension)) {
				return getPdfPreview(file, fileName);
			}
			else if (isTextFile(extension)) {
				return getTextFilePreview(file, fileName);
			}
			else {
				return "Binary or unsupported file type, cannot preview";
			}
		}
		catch (Exception e) {
			log.warn("Error getting preview for {}: {}", fileName, e.getMessage());
			return "Preview failed: " + e.getMessage();
		}
	}

	/**
	 * Get PDF file preview
	 */
	private String getPdfPreview(Path file, String fileName) {
		try (PDDocument document = PDDocument.load(file.toFile())) {
			if (document.isEncrypted()) {
				return "PDF file is encrypted, cannot preview";
			}

			int pageCount = document.getNumberOfPages();
			if (pageCount == 0) {
				return "PDF file is empty";
			}

			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setEndPage(1); // Read first page only
			String text = stripper.getText(document).trim();

			if (text.isEmpty()) {
				return String.format("PDF first page has no text content (total %d pages)", pageCount);
			}

			String preview = text.length() > MAX_PREVIEW_LENGTH ? text.substring(0, MAX_PREVIEW_LENGTH) + "..." : text;

			return String.format("%s (Total %d pages)", preview.replaceAll("\\s+", " "), pageCount);

		}
		catch (Exception e) {
			log.warn("Error getting PDF preview for {}: {}", fileName, e.getMessage());
			return "PDF preview failed: " + e.getMessage();
		}
	}

	/**
	 * Get text file preview
	 */
	private String getTextFilePreview(Path file, String fileName) {
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			if (lines.isEmpty()) {
				return "Empty file";
			}

			StringBuilder preview = new StringBuilder();
			int previewLines = Math.min(PREVIEW_LINES, lines.size());

			for (int i = 0; i < previewLines; i++) {
				String line = lines.get(i).trim();
				if (line.isEmpty()) {
					continue; // Skip empty lines
				}

				if (line.length() > MAX_LINE_LENGTH) {
					preview.append(line.substring(0, MAX_LINE_LENGTH)).append("...");
				}
				else {
					preview.append(line);
				}

				if (i < previewLines - 1 && preview.length() > 0) {
					preview.append(" | ");
				}
			}

			String result = preview.toString();
			return result.isEmpty() ? "Only empty lines" : String.format("%s (Total %d lines)", result, lines.size());

		}
		catch (Exception e) {
			log.warn("Error getting text preview for {}: {}", fileName, e.getMessage());
			return "Text preview failed: " + e.getMessage();
		}
	}

	/**
	 * Check if file is text-based using predefined constants
	 */
	private boolean isTextFile(String extension) {
		return TEXT_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Get processing advice for file with correct tool names and enhanced recommendations
	 */
	private String getProcessingAdvice(String extension, long fileSize) {
		StringBuilder advice = new StringBuilder();

		// Size recommendations
		if (fileSize > LARGE_FILE_THRESHOLD) {
			advice.append("Large file (>50MB), recommend using MapReduce parallel processing; ");
		}
		else if (fileSize > MEDIUM_FILE_THRESHOLD) {
			advice.append("Medium file (>5MB), recommend using SmartContentSaving intelligent caching; ");
		}
		else {
			advice.append("Small file (<5MB), can be processed directly; ");
		}

		// Type recommendations - using correct tool names
		switch (extension) {
			case ".pdf" -> advice.append("Use doc_loader to extract PDF text content");
			case ".csv", ".xlsx", ".xls" ->
				advice.append("Use database_use to import to database for structured analysis");
			case ".json" -> advice.append("Use text_file_operator to parse JSON structure and extract key fields");
			case ".xml" -> advice.append("Use text_file_operator to parse XML structure and extract node information");
			case ".log" ->
				advice.append("Use text_file_operator for time-series analysis to extract errors and key events");
			case ".java", ".py", ".js", ".ts" -> advice.append(
					"Use text_file_operator for code structure analysis to extract functions and class information");
			case ".html", ".htm" -> advice.append("Use browser_use for web content parsing and DOM analysis");
			case ".md" ->
				advice.append("Use text_file_operator to parse Markdown format and extract document structure");
			default -> advice.append("Use text_file_operator for text content analysis and extract key information");
		}

		return advice.toString();
	}

	/**
	 * Generate overall recommendations with correct tool names
	 */
	private String generateOverallRecommendations(Map<String, List<Path>> filesByType, List<Path> allFiles) {
		StringBuilder recommendations = new StringBuilder();

		long totalSize = allFiles.stream().mapToLong(file -> {
			try {
				return Files.size(file);
			}
			catch (Exception e) {
				log.warn("Failed to get file size: {}", file, e);
				return 0;
			}
		}).sum();

		recommendations.append(String.format("📊 Total size: %s\n", formatFileSize(totalSize)));

		if (totalSize > 100 * 1024 * 1024) {
			recommendations.append("🔄 Recommend using MapReduce workflow for parallel processing\n");
		}
		else {
			recommendations.append("⚡ Can use regular tool chain for processing\n");
		}

		// Provide correct tool recommendations by file type
		if (filesByType.containsKey(".pdf")) {
			recommendations.append("📄 PDF files detected, prioritize using doc_loader tool\n");
		}
		if (filesByType.containsKey(".csv") || filesByType.containsKey(".xlsx") || filesByType.containsKey(".xls")) {
			recommendations.append("📊 Spreadsheet files detected, recommend using database_use for data analysis\n");
		}
		if (filesByType.containsKey(".log")) {
			recommendations
				.append("📋 Log files detected, recommend using text_file_operator for time-series anomaly analysis\n");
		}
		if (filesByType.containsKey(".java") || filesByType.containsKey(".py") || filesByType.containsKey(".js")
				|| filesByType.containsKey(".ts")) {
			recommendations
				.append("💻 Code files detected, recommend using text_file_operator for code structure analysis\n");
		}
		if (filesByType.containsKey(".html") || filesByType.containsKey(".htm")) {
			recommendations.append("🌐 HTML files detected, recommend using browser_use for web parsing\n");
		}

		recommendations.append("\n💡 Smart Processing Workflow Recommendations:\n");
		recommendations.append("1. 📋 Use recommended tools to extract content from each file\n");
		recommendations.append("2. 🔄 Merge analysis results from same file types\n");
		recommendations.append("3. 🔗 Generate cross-file correlation analysis reports\n");
		recommendations.append("4. 📊 Output structured comprehensive analysis results\n");
		recommendations.append("5. 💾 Use SmartContentSaving to handle large file content\n");

		return recommendations.toString();
	}

}
