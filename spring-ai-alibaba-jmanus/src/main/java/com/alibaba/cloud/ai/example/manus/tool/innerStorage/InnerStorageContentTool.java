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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.ToolPromptManager;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Internal storage content retrieval tool specialized for intelligent content extraction
 * and structured output, supporting AI intelligent analysis and data extraction functions
 */
public class InnerStorageContentTool extends AbstractBaseTool<InnerStorageContentTool.InnerStorageContentInput> {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageContentTool.class);

	/**
	 * Internal storage content retrieval input class
	 */
	public static class InnerStorageContentInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_name")
		private String fileName;

		@com.fasterxml.jackson.annotation.JsonProperty("folder_name")
		private String folderName;

		@com.fasterxml.jackson.annotation.JsonProperty("query_key")
		private String queryKey;

		private List<String> columns;

		@com.fasterxml.jackson.annotation.JsonProperty("start_line")
		private Integer startLine;

		@com.fasterxml.jackson.annotation.JsonProperty("end_line")
		private Integer endLine;

		public InnerStorageContentInput() {
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

		public String getFolderName() {
			return folderName;
		}

		public void setFolderName(String folderName) {
			this.folderName = folderName;
		}

		public String getQueryKey() {
			return queryKey;
		}

		public void setQueryKey(String queryKey) {
			this.queryKey = queryKey;
		}

		public List<String> getColumns() {
			return columns;
		}

		public void setColumns(List<String> columns) {
			this.columns = columns;
		}

		public Integer getStartLine() {
			return startLine;
		}

		public void setStartLine(Integer startLine) {
			this.startLine = startLine;
		}

		public Integer getEndLine() {
			return endLine;
		}

		public void setEndLine(Integer endLine) {
			this.endLine = endLine;
		}

	}

	private final UnifiedDirectoryManager directoryManager;

	private final SummaryWorkflow summaryWorkflow;

	private final PlanExecutionRecorder planExecutionRecorder;

	private final ToolPromptManager toolPromptManager;

	public InnerStorageContentTool(UnifiedDirectoryManager directoryManager, SummaryWorkflow summaryWorkflow,
			PlanExecutionRecorder planExecutionRecorder, ToolPromptManager toolPromptManager) {
		this.directoryManager = directoryManager;
		this.summaryWorkflow = summaryWorkflow;
		this.planExecutionRecorder = planExecutionRecorder;
		this.toolPromptManager = toolPromptManager;
	}

	private static final String TOOL_NAME = "inner_storage_content_tool";

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return toolPromptManager.getToolDescription("inner_storage_content_tool");
	}

	@Override
	public String getParameters() {
		return toolPromptManager.getToolParameters("inner_storage_content_tool");
	}

	@Override
	public Class<InnerStorageContentInput> getInputType() {
		return InnerStorageContentInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	public OpenAiApi.FunctionTool getToolDefinition() {
		String description = getDescription();
		String parameters = getParameters();
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute internal storage content retrieval operation
	 */
	@Override
	public ToolExecuteResult run(InnerStorageContentInput input) {
		log.info("InnerStorageContentTool input: action={}, fileName={}, folderName={}, queryKey={}, columns={}",
				input.getAction(), input.getFileName(), input.getFolderName(), input.getQueryKey(), input.getColumns());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("Error: action parameter is required");
			}

			return switch (action) {
				case "get_content" -> getStoredContent(input.getFileName(), input.getQueryKey(), input.getColumns());
				case "get_folder_content" ->
					getFolderContent(input.getFolderName(), input.getQueryKey(), input.getColumns());
				default -> new ToolExecuteResult("Error: Unsupported operation type '" + action
						+ "'. Supported operations: get_content, get_folder_content");
			};
		}
		catch (Exception e) {
			log.error("InnerStorageContentTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Get stored content by filename, supports AI intelligent extraction and structured
	 * output
	 */
	private ToolExecuteResult getStoredContent(String fileName, String queryKey, List<String> columns) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return new ToolExecuteResult("Error: file_name parameter is required");
		}
		if (queryKey == null || queryKey.trim().isEmpty()) {
			return new ToolExecuteResult(
					"Error: query_key parameter is required to specify content keywords to extract");
		}
		if (columns == null || columns.isEmpty()) {
			return new ToolExecuteResult(
					"Error: columns parameter is required to specify structured column names for return results");
		}
		try {
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			Path targetFile = null;

			// First try exact relative path matching
			if (fileName.contains("/")) {
				Path exactPath = planDir.resolve(fileName);
				if (Files.exists(exactPath) && Files.isRegularFile(exactPath)) {
					targetFile = exactPath;
				}
			}
			else {
				// If no path separator, exact match filename in root directory
				List<Path> files = Files.list(planDir).filter(Files::isRegularFile).toList();
				for (Path filePath : files) {
					if (filePath.getFileName().toString().equals(fileName)) {
						targetFile = filePath;
						break;
					}
				}
			}

			if (targetFile == null) {
				return new ToolExecuteResult(
						"File '" + fileName + "' not found. Please provide exact filename or relative path.");
			}

			String fileContent = Files.readString(targetFile);
			String actualFileName = planDir.relativize(targetFile).toString();

			log.info("Delegating to SummaryWorkflow for file content extraction: file={}, query keywords={}",
					actualFileName, queryKey);
			Long thinkActRecordId = getCurrentThinkActRecordId();
			String terminateColumnsString = String.join(",", columns);
			String result = summaryWorkflow
				.executeSummaryWorkflow(rootPlanId, actualFileName, fileContent, queryKey, thinkActRecordId,
						terminateColumnsString)
				.get();
			return new ToolExecuteResult(result);
		}
		catch (IOException e) {
			log.error("Failed to get storage content", e);
			return new ToolExecuteResult("Failed to get content: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("SummaryWorkflow execution failed", e);
			return new ToolExecuteResult("Content processing failed: " + e.getMessage());
		}
	}

	/**
	 * Get information from all files in specified folder
	 */
	private ToolExecuteResult getFolderContent(String folderName, String queryKey, List<String> columns) {
		if (folderName == null || folderName.trim().isEmpty()) {
			return new ToolExecuteResult("Error: folder_name parameter is required");
		}
		if (queryKey == null || queryKey.trim().isEmpty()) {
			return new ToolExecuteResult(
					"Error: query_key parameter is required to specify content keywords to extract");
		}
		if (columns == null || columns.isEmpty()) {
			return new ToolExecuteResult(
					"Error: columns parameter is required to specify structured column names for return results");
		}
		try {
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			Path targetFolder = planDir.resolve(folderName);

			if (!Files.exists(targetFolder)) {
				return new ToolExecuteResult("Folder '" + folderName + "' does not exist.");
			}

			if (!Files.isDirectory(targetFolder)) {
				return new ToolExecuteResult("'" + folderName + "' is not a folder.");
			}

			// Get all files in the folder
			List<Path> files = Files.list(targetFolder).filter(Files::isRegularFile).toList();

			if (files.isEmpty()) {
				return new ToolExecuteResult("No files in folder '" + folderName + "'.");
			}

			// Combine all file contents
			StringBuilder combinedContent = new StringBuilder();
			for (Path file : files) {
				String relativePath = planDir.relativize(file).toString();
				combinedContent.append("=== File: ").append(relativePath).append(" ===\n");
				combinedContent.append(Files.readString(file));
				combinedContent.append("\n\n");
			}

			log.info(
					"Delegating to SummaryWorkflow for folder content extraction: folder={}, file count={}, query keywords={}",
					folderName, files.size(), queryKey);

			Long thinkActRecordId = getCurrentThinkActRecordId();
			String terminateColumnsString = String.join(",", columns);
			String result = summaryWorkflow
				.executeSummaryWorkflow(rootPlanId, folderName, combinedContent.toString(), queryKey, thinkActRecordId,
						terminateColumnsString)
				.get();
			return new ToolExecuteResult(result);

		}
		catch (IOException e) {
			log.error("Failed to get folder content", e);
			return new ToolExecuteResult("Failed to get folder content: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("SummaryWorkflow execution failed", e);
			return new ToolExecuteResult("Content processing failed: " + e.getMessage());
		}
	}

	/**
	 * Get current think-act record ID
	 * @return Current think-act record ID, return null if none
	 */
	private Long getCurrentThinkActRecordId() {
		try {
			Long thinkActRecordId = planExecutionRecorder.getCurrentThinkActRecordId(currentPlanId, rootPlanId);
			if (thinkActRecordId != null) {
				log.info("Current think-act record ID: {}", thinkActRecordId);
				return thinkActRecordId;
			}
			else {
				log.warn("No current think-act record ID");
			}
		}
		catch (Exception e) {
			log.warn("Failed to get current think-act record ID: {}", e.getMessage());
		}

		return null;
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("InnerStorageContent current status:\n");
			sb.append("- Storage root directory: ")
				.append(directoryManager.getRootPlanDirectory(rootPlanId))
				.append("\n");
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			List<Path> files = Files.exists(planDir) ? Files.list(planDir).filter(Files::isRegularFile).toList()
					: List.of();
			if (files.isEmpty()) {
				sb.append("- Internal files: None\n");
			}
			else {
				sb.append("- Internal files (").append(files.size()).append(" files)\n");
			}
			return sb.toString();
		}
		catch (Exception e) {
			log.error("Failed to get tool status", e);
			return "InnerStorageContent status retrieval failed: " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		// Content retrieval tool does not need to perform cleanup operations
		log.info("InnerStorageContentTool cleanup for plan: {}", planId);
	}

}
