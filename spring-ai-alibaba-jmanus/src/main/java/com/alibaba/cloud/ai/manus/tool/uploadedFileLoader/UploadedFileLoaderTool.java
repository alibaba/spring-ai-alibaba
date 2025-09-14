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
package com.alibaba.cloud.ai.manus.tool.uploadedFileLoader;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Smart file analysis and tool recommendation system. Analyzes uploaded files and
 * recommends the most appropriate tools for processing. Does NOT process file content
 * directly - only provides intelligent analysis and tool selection guidance.
 *
 * @author Jmanus Team
 * @version 2.0
 * @since 1.0
 */
public class UploadedFileLoaderTool extends AbstractBaseTool<UploadedFileLoaderTool.UploadedFileInput> {

	private static final Logger log = LoggerFactory.getLogger(UploadedFileLoaderTool.class);

	// Constants
	private static final String TOOL_NAME = "uploaded_file_loader";

	private static final int PREVIEW_LINES = 3;

	private static final int MAX_LINE_LENGTH = 100;

	private static final int MAX_PREVIEW_LENGTH = 200;

	private static final long LARGE_FILE_THRESHOLD = 50L * 1024 * 1024; // 50MB

	private static final long MEDIUM_FILE_THRESHOLD = 5L * 1024 * 1024; // 5MB

	// File type constants
	private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(".txt", ".md", ".csv", ".json", ".xml", ".html",
			".htm", ".log", ".java", ".py", ".js", ".ts", ".css", ".sql", ".yaml", ".yml", ".properties", ".conf",
			".ini", ".sh", ".bat");

	private static final Set<String> IMAGE_FILE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp",
			".svg");

	private final UnifiedDirectoryManager directoryManager;

	private final ObjectMapper objectMapper;

	// Cache for fallback uploads directory to improve performance
	private volatile Path cachedFallbackDir = null;

	private volatile long cacheTimestamp = 0;

	private static final long CACHE_VALIDITY_MS = 30000; // 30 seconds cache

	/**
	 * Constructor for manual instantiation
	 * @param directoryManager the directory manager for file operations
	 */
	public UploadedFileLoaderTool(UnifiedDirectoryManager directoryManager) {
		this.directoryManager = Objects.requireNonNull(directoryManager, "DirectoryManager cannot be null");
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Input class for uploaded file operations - analysis and tool recommendation
	 */
	public static class UploadedFileInput {

		// Default constructor - no parameters needed
		public UploadedFileInput() {
		}

	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return """
				Smart file analysis with optimized two-track processing strategy:

				🔧 TRACK 1 - Document Files (Tool Chain Processing):
				   PDF, Excel, Text, Code, HTML → Specialized tools → AI model analysis

				🖼️ TRACK 2 - Image Files (Direct AI Processing):
				   PNG, JPG, GIF, SVG → Direct AI model analysis (no intermediate tools)

				Automatically analyzes uploaded files and provides intelligent processing strategy recommendations.
				For large files or complex processing, recommends using extract_relevant_content.
				""";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {},
				    "required": []
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

		// Check if rootPlanId is valid
		if (rootPlanId == null || rootPlanId.trim().isEmpty()) {
			log.warn("getCurrentToolStateString called with null or empty rootPlanId");
			return "Uploaded Files State: Invalid root plan ID";
		}

		log.debug("🔍 getCurrentToolStateString called for planId: {}", currentPlanId);

		try {
			Path uploadsDir = directoryManager.getRootPlanDirectory(rootPlanId).resolve("uploads");

			if (!Files.exists(uploadsDir)) {
				log.debug("No uploads directory found for plan: {}", currentPlanId);
				// Try to find the most recent temporary plan with uploaded files
				uploadsDir = findMostRecentTempPlanUploads();
				if (uploadsDir == null) {
					return "Uploaded Files State: No uploads directory found for plan " + currentPlanId;
				}
				log.debug("Found recent temp plan uploads directory: {}", uploadsDir);
			}

			long fileCount;
			try (var stream = Files.list(uploadsDir)) {
				fileCount = stream.filter(Files::isRegularFile).count();
			}

			log.debug("📁 Found {} files in uploads directory for plan {}", fileCount, currentPlanId);

			if (fileCount > 0) {
				String result = String.format(
						"""
								Uploaded files available: %d files found in plan %s

								🔧 To analyze these files, you must call the 'uploaded_file_loader' tool:
								- No parameters needed - automatically provides comprehensive analysis with content preview and processing advice

								Note: This tool provides analysis and recommendations. Use the recommended tools for actual file processing.

								Example: Call uploaded_file_loader with no parameters
								""",
						fileCount, currentPlanId);
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
		// Clear cache to prevent stale references
		cachedFallbackDir = null;
		cacheTimestamp = 0;
	}

	@Override
	public ToolExecuteResult run(UploadedFileInput input) {
		log.info("UploadedFileLoaderTool executing smart analysis for planId: {}", currentPlanId);

		try {
			return smartAnalyzeAllFiles();
		}
		catch (Exception e) {
			log.error("Error executing uploaded file loader tool", e);
			return new ToolExecuteResult("Error: " + e.getMessage());
		}
	}

	/**
	 * Smart analyze all uploaded files and provide processing recommendations
	 */
	private ToolExecuteResult smartAnalyzeAllFiles() {
		// Check if rootPlanId is valid
		if (rootPlanId == null || rootPlanId.trim().isEmpty()) {
			log.warn("smartAnalyzeAllFiles called with null or empty rootPlanId");
			return new ToolExecuteResult("Error: Invalid root plan ID");
		}

		try {
			Path uploadsDir = directoryManager.getRootPlanDirectory(rootPlanId).resolve("uploads");

			if (!Files.exists(uploadsDir)) {
				log.info("No uploads directory found for plan: {}", currentPlanId);
				// Try to find the most recent temporary plan with uploaded files
				uploadsDir = findMostRecentTempPlanUploads();
				if (uploadsDir == null) {
					return new ToolExecuteResult("Uploads directory not found for plan: " + currentPlanId);
				}
				log.info("Found recent temp plan uploads directory: {}", uploadsDir);
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
	 * Check if file is text-based using predefined constants with improved logic
	 */
	private boolean isTextFile(String extension) {
		// Extended text file type list
		Set<String> TEXT_FILE_EXTENSIONS = Set.of(".txt", ".md", ".csv", ".json", ".xml", ".html", ".htm", ".log",
				".java", ".py", ".js", ".ts", ".css", ".sql", ".yaml", ".yml", ".properties", ".conf", ".ini", ".sh",
				".bat", ".ps1", ".bash", ".r", ".php", ".rb", ".go", ".rs", ".cpp", ".c", ".h", ".hpp", ".cs", ".vb",
				".swift", ".kt", ".scala", ".clj", ".hs", ".ml", ".tex", ".rst", ".adoc", ".wiki", ".org", ".rtf",
				".odt");

		return TEXT_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Check if file is an image file
	 */
	private boolean isImageFile(String extension) {
		Set<String> IMAGE_FILE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tiff", ".tif", ".webp",
				".svg", ".ico", ".raw", ".heic", ".heif");

		return IMAGE_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Check if file is a binary document file
	 */
	private boolean isBinaryDocumentFile(String extension) {
		Set<String> BINARY_DOCUMENT_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".pptx", ".ppt",
				".odt", ".ods", ".odp", ".epub", ".mobi", ".azw3");

		return BINARY_DOCUMENT_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Check if file is a compressed file
	 */
	private boolean isCompressedFile(String extension) {
		Set<String> COMPRESSED_FILE_EXTENSIONS = Set.of(".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz", ".lzma",
				".lz4", ".zst", ".br");

		return COMPRESSED_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	/**
	 * Check if any image files exist in the files by type map
	 */
	private boolean hasImageFiles(Map<String, List<Path>> filesByType) {
		return IMAGE_FILE_EXTENSIONS.stream().anyMatch(filesByType::containsKey);
	}

	/**
	 * Get file type description
	 */
	private String getFileTypeDescription(String extension) {
		// Check if it's an image file first
		if (isImageFile(extension)) {
			return "Image File";
		}

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
			default -> "Unknown Type";
		};
	}

	/**
	 * Get recommended tool for file type
	 */
	private String getRecommendedTool(String extension) {
		// Check if it's an image file first
		if (isImageFile(extension)) {
			return "🚫 NO TOOLS NEEDED - DIRECT AI Analysis";
		}

		return switch (extension) {
			case ".pdf" -> "doc_loader → AI Analysis";
			case ".txt", ".md", ".log" -> "text_file_operator → AI Analysis";
			case ".csv", ".xlsx", ".xls" -> "table_processor → AI Analysis";
			case ".json", ".xml" -> "text_file_operator → AI Analysis";
			case ".html", ".htm" -> "browser_use → AI Analysis";
			case ".java", ".py", ".js", ".ts" -> "text_file_operator → AI Analysis";
			default -> "text_file_operator → AI Analysis";
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
	 * Get processing advice based on file size and type with improved logic
	 */
	private String getProcessingAdvice(String extension, long fileSize) {
		StringBuilder advice = new StringBuilder();

		// Size-based recommendations with file type consideration
		if (fileSize > LARGE_FILE_THRESHOLD) {
			// Check if file type is supported by extract_relevant_content
			if (isTextFile(extension)) {
				advice.append(
						"🚨 LARGE TEXT FILE (>50MB) - MUST USE extract_relevant_content for MapReduce processing; ");
				advice.append("✅ This file type is supported by MapReduce processing; ");
			}
			else {
				advice
					.append("🚨 LARGE BINARY FILE (>50MB) - extract_relevant_content CANNOT process this file type; ");
				advice.append("❌ Use specialized tools instead; ");
			}
		}
		else if (fileSize > MEDIUM_FILE_THRESHOLD) {
			if (isTextFile(extension)) {
				advice.append("Medium text file (>5MB), can use extract_relevant_content or specialized tools; ");
			}
			else {
				advice.append("Medium binary file (>5MB), use specialized tools; ");
			}
		}
		else {
			advice.append("Small file (<5MB), can be processed directly; ");
		}

		// Processing strategy recommendations based on file type
		if (isImageFile(extension)) {
			advice.append("🚫 DO NOT use any tools - Send directly to AI model for visual analysis");
		}
		else if (isTextFile(extension)) {
			// Text files can use extract_relevant_content or specialized tools
			switch (extension) {
				case ".pdf" -> advice.append("Use doc_loader → AI analysis");
				case ".csv", ".xlsx", ".xls" -> advice.append("Use table_processor → AI analysis");
				case ".json", ".xml" -> advice.append("Use text_file_operator → AI analysis");
				case ".log" -> advice.append("Use text_file_operator → AI analysis");
				case ".java", ".py", ".js", ".ts" -> advice.append("Use text_file_operator → AI analysis");
				case ".html", ".htm" -> advice.append("Use browser_use → AI analysis");
				case ".md" -> advice.append("Use text_file_operator → AI analysis");
				default -> advice.append("Use text_file_operator → AI analysis");
			}
		}
		else {
			// Binary files must use specialized tools
			switch (extension) {
				case ".pdf" -> advice.append("Use doc_loader → AI analysis");
				case ".csv", ".xlsx", ".xls" -> advice.append("Use table_processor → AI analysis");
				case ".docx", ".doc" -> advice.append("Use doc_loader → AI analysis");
				case ".pptx", ".ppt" -> advice.append("Use doc_loader → AI analysis");
				default -> advice.append("Use specialized tools based on file type");
			}
		}

		return advice.toString();
	}

	/**
	 * Generate overall processing recommendations with improved file type analysis
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

		// Analyze file types and sizes for better recommendations
		List<Path> largeTextFiles = new ArrayList<>();
		List<Path> largeBinaryFiles = new ArrayList<>();
		List<Path> mediumFiles = new ArrayList<>();
		List<Path> smallFiles = new ArrayList<>();

		for (Path file : allFiles) {
			try {
				long fileSize = Files.size(file);
				String extension = getFileExtension(file.getFileName().toString()).toLowerCase();

				if (fileSize > LARGE_FILE_THRESHOLD) {
					if (isTextFile(extension)) {
						largeTextFiles.add(file);
					}
					else {
						largeBinaryFiles.add(file);
					}
				}
				else if (fileSize > MEDIUM_FILE_THRESHOLD) {
					mediumFiles.add(file);
				}
				else {
					smallFiles.add(file);
				}
			}
			catch (Exception e) {
				log.warn("Failed to analyze file: {}", file, e);
			}
		}

		// Provide specific recommendations based on file analysis
		if (!largeTextFiles.isEmpty()) {
			recommendations.append("🚨 **LARGE TEXT FILES DETECTED**: ");
			recommendations.append(String.format("%d files >50MB\n", largeTextFiles.size()));
			recommendations.append("✅ **RECOMMENDED**: Use extract_relevant_content for MapReduce processing\n");
		}

		if (!largeBinaryFiles.isEmpty()) {
			recommendations.append("🚨 **LARGE BINARY FILES DETECTED**: ");
			recommendations.append(String.format("%d files >50MB\n", largeBinaryFiles.size()));
			recommendations.append("❌ **WARNING**: extract_relevant_content cannot process binary files\n");
			recommendations.append("🔧 **RECOMMENDED**: Use specialized tools for each file type\n");
		}

		if (!mediumFiles.isEmpty()) {
			recommendations.append("📁 **MEDIUM FILES**: ");
			recommendations.append(String.format("%d files 5-50MB\n", mediumFiles.size()));
			recommendations.append("⚡ Can use regular tools or extract_relevant_content for text files\n");
		}

		if (!smallFiles.isEmpty()) {
			recommendations.append("📄 **SMALL FILES**: ");
			recommendations.append(String.format("%d files <5MB\n", smallFiles.size()));
			recommendations.append("⚡ Can be processed directly with any appropriate tool\n");
		}

		// Optimized processing strategy by file type
		recommendations.append("\n🎯 **OPTIMIZED PROCESSING STRATEGY**:\n");

		// Document files - Tool Chain Processing
		if (filesByType.containsKey(".pdf")) {
			recommendations.append("📄 PDF files → doc_loader → AI model analysis\n");
		}
		if (filesByType.containsKey(".csv") || filesByType.containsKey(".xlsx") || filesByType.containsKey(".xls")) {
			recommendations.append("📊 Spreadsheet files → table_processor → AI model analysis\n");
		}
		if (filesByType.containsKey(".log")) {
			recommendations.append("📋 Log files → text_file_operator → AI model analysis\n");
		}
		if (filesByType.containsKey(".java") || filesByType.containsKey(".py") || filesByType.containsKey(".js")
				|| filesByType.containsKey(".ts")) {
			recommendations.append("💻 Code files → text_file_operator → AI model analysis\n");
		}
		if (filesByType.containsKey(".html") || filesByType.containsKey(".htm")) {
			recommendations.append("🌐 HTML files → browser_use → AI model analysis\n");
		}
		if (filesByType.containsKey(".json") || filesByType.containsKey(".xml")) {
			recommendations.append("📋 Structured files → text_file_operator → AI model analysis\n");
		}

		// Image files - Direct Model Processing
		if (hasImageFiles(filesByType)) {
			recommendations.append(
					"🖼️ **Image files → 🚫 DO NOT USE ANY TOOLS - Send directly to AI model for visual analysis**\n");
		}

		recommendations.append("\n💡 **PROCESSING OPTIONS**:\n");

		// Provide specific guidance based on file types
		if (!largeTextFiles.isEmpty() && largeBinaryFiles.isEmpty()) {
			recommendations.append("🚀 **BEST OPTION**: Use extract_relevant_content for all large files\n");
		}
		else if (!largeBinaryFiles.isEmpty()) {
			recommendations.append(
					"🔧 **MIXED APPROACH**: Use extract_relevant_content for text files + specialized tools for binary files\n");
		}
		else if (totalSize > 100 * 1024 * 1024) { // 100MB total
			recommendations.append("🚀 **RECOMMENDED**: Use extract_relevant_content for complex processing\n");
			recommendations.append("📝 **Alternative**: Use individual tools for each file type\n");
		}
		else {
			recommendations.append("🔧 **Individual tools**: Use specialized tools for each file type\n");
		}

		recommendations.append("\n📂 **FILE PROCESSING PATHS**:\n");
		recommendations.append("   📋 PDF files → doc_loader (absolute path required)\n");
		recommendations.append("   📊 Excel/CSV → table_processor (absolute path required)\n");
		recommendations.append("   📝 Text files → text_file_operator (absolute path required)\n");
		recommendations.append("   🖼️ Image files → 🚫 NO TOOLS - Direct AI analysis\n");
		recommendations.append("   📄 Large text files → extract_relevant_content (MapReduce processing)\n");

		recommendations.append(
				"\n⚠️ **Important**: This tool provides analysis and recommendations. Use recommended tools for actual processing.\n");

		return recommendations.toString();
	}

	/**
	 * Find the most recent temporary plan directory with uploaded files This is a
	 * fallback mechanism when the current planId doesn't have uploads Enhanced to
	 * prioritize temp- prefixed directories and recent file modifications Thread-safe
	 * implementation with proper error handling
	 * @return Path to the most recent uploads directory, or null if none found
	 */
	private synchronized Path findMostRecentTempPlanUploads() {
		// Check cache validity
		long currentTime = System.currentTimeMillis();
		if (cachedFallbackDir != null && Files.exists(cachedFallbackDir)
				&& (currentTime - cacheTimestamp) < CACHE_VALIDITY_MS) {
			log.debug("🚀 Using cached fallback directory: {}", cachedFallbackDir);
			return cachedFallbackDir;
		}

		try {
			// Fix: Use a more reasonable way to get the root directory
			Path innerStorageRoot = directoryManager.getInnerStorageRoot();
			if (innerStorageRoot == null || !Files.exists(innerStorageRoot)) {
				log.debug("Inner storage root directory not found or doesn't exist");
				cachedFallbackDir = null;
				return null;
			}

			Path mostRecentUploads = null;
			long mostRecentTime = 0;

			// Search through all plan directories, prioritizing temp- prefixed ones
			try (var stream = Files.list(innerStorageRoot)) {
				List<Path> allPlanDirs = stream.filter(Files::isDirectory).toList();

				// Sort to prioritize temp- prefixed directories
				allPlanDirs.sort((p1, p2) -> {
					String name1 = p1.getFileName().toString();
					String name2 = p2.getFileName().toString();
					boolean isTemp1 = name1.startsWith("temp-");
					boolean isTemp2 = name2.startsWith("temp-");

					if (isTemp1 && !isTemp2)
						return -1;
					if (!isTemp1 && isTemp2)
						return 1;
					return name2.compareTo(name1); // Reverse alphabetical for newer IDs
													// first
				});

				for (Path planDir : allPlanDirs) {
					Path uploadsDir = planDir.resolve("uploads");

					if (Files.exists(uploadsDir) && Files.isDirectory(uploadsDir)) {
						// Check if this uploads directory has files
						try (var fileStream = Files.list(uploadsDir)) {
							long fileCount = fileStream.filter(Files::isRegularFile).count();

							if (fileCount > 0) {
								// Get the most recent file modification time in this
								// directory
								// Re-list files to get modification times (stream was
								// consumed by count)
								try (var modTimeStream = Files.list(uploadsDir)) {
									long maxFileModified = modTimeStream.filter(Files::isRegularFile)
										.mapToLong(file -> {
											try {
												return Files.getLastModifiedTime(file).toMillis();
											}
											catch (Exception e) {
												log.warn("Failed to get modification time for file: {}", file, e);
												return 0;
											}
										})
										.max()
										.orElse(0);

									if (maxFileModified > mostRecentTime) {
										mostRecentTime = maxFileModified;
										mostRecentUploads = uploadsDir;
										log.debug("🔍 Found candidate uploads dir: {} with files modified at {}",
												uploadsDir, java.time.Instant.ofEpochMilli(maxFileModified));
									}
								}
							}
						}
					}
				}
			}

			if (mostRecentUploads != null) {
				log.info("🔍 Found fallback uploads directory: {} (modified: {})", mostRecentUploads,
						java.time.Instant.ofEpochMilli(mostRecentTime));
				// Update cache
				cachedFallbackDir = mostRecentUploads;
				cacheTimestamp = currentTime;
			}
			else {
				log.warn("⚠️ No uploads directories with files found in any plan directory");
				cachedFallbackDir = null;
			}

			return mostRecentUploads;

		}
		catch (IOException e) {
			log.error("💥 Error searching for recent uploads directories: {}", e.getMessage(), e);
			return null;
		}
	}

}
