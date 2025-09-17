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
package com.alibaba.cloud.ai.manus.tool.mapreduce;

import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Finalize tool for MapReduce workflow Supports copying the reduce output file to a new
 * file with user-specified name
 */
public class FinalizeTool extends AbstractBaseTool<FinalizeTool.FinalizeInput> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(FinalizeTool.class);

	// ==================== Configuration Constants ====================

	/**
	 * Supported operation type: export reduce output to new file
	 */
	private static final String ACTION_EXPORT = "export";

	/**
	 * Source file name for finalize operations (same as ReduceOperationTool)
	 */
	private static final String REDUCE_FILE_NAME = "reduce_output.md";

	/**
	 * Internal input class for defining Finalize tool input parameters
	 */
	public static class FinalizeInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("new_file_name")
		private String newFileName;

		public FinalizeInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getNewFileName() {
			return newFileName;
		}

		public void setNewFileName(String newFileName) {
			this.newFileName = newFileName;
		}

	}

	private static final String TOOL_NAME = "mapreduce_finalize_tool";

	private static final String TOOL_DESCRIPTION = """
			Finalize tool for MapReduce workflow output processing.
			Supports copying the reduce output file to a new file with user-specified name.
			Source file: %s
			Supported operations:
			- export: Copy the reduce output file to a new file with the specified name in the same directory.

			This tool is specifically designed for finalizing MapReduce workflow results by:
			- Creating a final output file with a meaningful name
			- Preserving the original reduce output file
			- Ensuring the final result is properly named and accessible
			""".formatted(REDUCE_FILE_NAME);

	private static final String PARAMETERS_JSON = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "const": "export"
			        },
			        "new_file_name": {
			            "type": "string",
						"description": "New file name (with extension), used to save the final output result"
			        }
			    },
			    "required": ["action", "new_file_name"],
			    "additionalProperties": false
			}
			""";

	private UnifiedDirectoryManager unifiedDirectoryManager;

	private ManusProperties manusProperties;

	// Shared state manager for managing shared state between multiple Agent instances
	private MapReduceSharedStateManager sharedStateManager;

	// Track if any operation has completed, allowing termination
	private volatile boolean operationCompleted = false;

	public FinalizeTool(String planId, ManusProperties manusProperties, MapReduceSharedStateManager sharedStateManager,
			UnifiedDirectoryManager unifiedDirectoryManager) {
		this.currentPlanId = planId;
		this.manusProperties = manusProperties;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.sharedStateManager = sharedStateManager;

	}

	/**
	 * Set shared state manager
	 */
	public void setSharedStateManager(MapReduceSharedStateManager sharedStateManager) {
		this.sharedStateManager = sharedStateManager;
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	/**
	 * Get task directory list
	 */
	public List<String> getSplitResults() {
		if (sharedStateManager != null && currentPlanId != null) {
			return sharedStateManager.getSplitResults(currentPlanId);
		}
		return new ArrayList<>();
	}

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return PARAMETERS_JSON;
	}

	@Override
	public Class<FinalizeInput> getInputType() {
		return FinalizeInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "data-processing";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS_JSON);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Finalize operation
	 */
	@Override
	public ToolExecuteResult run(FinalizeInput input) {
		log.info("FinalizeTool input: action={}, newFileName={}", input.getAction(), input.getNewFileName());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("Error: action parameter is required");
			}

			ToolExecuteResult result = switch (action) {
				case ACTION_EXPORT -> {
					String newFileName = input.getNewFileName();
					if (newFileName == null || newFileName.trim().isEmpty()) {
						yield new ToolExecuteResult("Error: new_file_name parameter is required");
					}
					yield exportFile(newFileName);
				}
				default ->
					new ToolExecuteResult("Unknown operation: " + action + ". Supported operations: " + ACTION_EXPORT);
			};

			// Mark operation as completed for termination capability
			this.operationCompleted = true;
			return result;

		}
		catch (Exception e) {
			log.error("FinalizeTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Export (copy) the reduce output file to a new file with the specified name
	 */
	private ToolExecuteResult exportFile(String newFileName) {
		try {
			// Get plan directory with hierarchical structure
			Path planDir = getPlanDirectory();
			Path sourceFilePath = planDir.resolve(REDUCE_FILE_NAME);

			// Target file path - export to parent directory or root storage directory
			Path targetFilePath;
			if (rootPlanId != null && !rootPlanId.equals(currentPlanId)) {
				// If hierarchical structure exists, export to parent directory
				// (rootPlanId level)
				targetFilePath = getInnerStorageRoot().resolve(rootPlanId).resolve(newFileName);
			}
			else {
				// If no hierarchy, throw an exception
				return new ToolExecuteResult(
						"Error: Cannot export file - no hierarchical structure found. The tool requires a root plan ID different from the current plan ID.");
			}

			// Check if source file exists
			if (!Files.exists(sourceFilePath)) {
				return new ToolExecuteResult("Error: Source file does not exist: " + REDUCE_FILE_NAME);
			}

			// Check if target file already exists
			if (Files.exists(targetFilePath)) {
				return new ToolExecuteResult("Error: Target file already exists: " + newFileName);
			}

			// Copy the file
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.COPY_ATTRIBUTES);

			log.info("File exported successfully: {} -> {}", REDUCE_FILE_NAME, newFileName);

			// Read the target file and check its size
			List<String> lines = Files.readAllLines(targetFilePath);
			StringBuilder fileContent = new StringBuilder();
			for (String line : lines) {
				fileContent.append(line).append("\n");
			}

			int charLimit = getInfiniteContextTaskContextSize();
			int contentLength = fileContent.length();

			// If content size is less than required content size, include the entire
			// content in the result
			if (contentLength <= charLimit) {
				StringBuilder result = new StringBuilder();
				result.append(String.format(
						"The function call was successful. The content has been saved to the file(%s). the file content is :\n",
						newFileName));
				result.append(fileContent.toString());
				return new ToolExecuteResult(result.toString());
			}
			// If the file content size exceeds the required context size, indicate that
			// the content is still too large
			else {
				StringBuilder result = new StringBuilder();
				result.append("The function call was successful. The content has been saved to the file(");
				result.append(newFileName);
				result.append("). The file content size is ");
				result.append(contentLength);
				result.append(" characters, which exceeds the limit of ");
				result.append(charLimit);
				result.append(" characters. The content is still too large and has been saved to the new file. ");
				result.append(
						"The user can read the file directly or use other functions to further process the oversized file.");
				return new ToolExecuteResult(result.toString());
			}
		}
		catch (IOException e) {
			log.error("Failed to export file", e);
			return new ToolExecuteResult("Failed to export file: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		if (sharedStateManager != null && currentPlanId != null) {
			return sharedStateManager.getCurrentToolStateString(currentPlanId);
		}

		// Fallback solution
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// Clean up shared state
		if (sharedStateManager != null && planId != null) {
			sharedStateManager.cleanupPlanState(planId);
		}
		log.info("FinalizeTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(FinalizeInput input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Get inner storage root directory path
	 */
	private Path getInnerStorageRoot() {
		return unifiedDirectoryManager.getInnerStorageRoot();
	}

	/**
	 * Get plan directory path with hierarchical structure support
	 */
	private Path getPlanDirectory() {
		Path innerStorageRoot = getInnerStorageRoot();
		if (rootPlanId != null && !rootPlanId.equals(currentPlanId)) {
			// Use hierarchical structure: inner_storage/{rootPlanId}/{currentPlanId}
			return innerStorageRoot.resolve(rootPlanId).resolve(currentPlanId);
		}
		else {
			// Use flat structure: inner_storage/{planId}
			return innerStorageRoot.resolve(currentPlanId);
		}
	}

	/**
	 * Get infinite context task context size
	 * @return Context size for infinite context tasks
	 */
	private Integer getInfiniteContextTaskContextSize() {
		if (manusProperties != null) {
			Integer contextSize = manusProperties.getInfiniteContextTaskContextSize();
			return contextSize != null ? contextSize : 20000; // Default 20000 characters
		}
		return 20000; // Default 20000 characters
	}

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// FinalizeTool can be terminated after the export operation has completed
		// This marks the end of the MapReduce workflow
		return operationCompleted;
	}

}
