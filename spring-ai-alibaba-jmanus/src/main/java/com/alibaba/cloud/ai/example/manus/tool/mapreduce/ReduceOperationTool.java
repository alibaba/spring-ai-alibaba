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
package com.alibaba.cloud.ai.example.manus.tool.mapreduce;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Reduce operation tool for MapReduce workflow Supports append operation for file
 * manipulation in root plan directory
 */
public class ReduceOperationTool extends AbstractBaseTool<ReduceOperationTool.ReduceOperationInput>
		implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(ReduceOperationTool.class);

	// ==================== Configuration Constants ====================

	/**
	 * Fixed file name for reduce operations
	 */
	private static final String REDUCE_FILE_NAME = "reduce_output.md";

	/**
	 * Internal input class for defining Reduce operation tool input parameters
	 */
	public static class ReduceOperationInput {

		private List<List<Object>> data;

		@com.fasterxml.jackson.annotation.JsonProperty("has_value")
		private boolean hasValue;

		public ReduceOperationInput() {
		}

		public List<List<Object>> getData() {
			return data;
		}

		public void setData(List<List<Object>> data) {
			this.data = data;
		}

		public boolean isHasValue() {
			return hasValue;
		}

		public void setHasValue(boolean hasValue) {
			this.hasValue = hasValue;
		}

	}

	private static final String TOOL_NAME = "reduce_operation_tool";

	private static String getToolDescription(List<String> terminateColumns) {
		String baseDescription = """
				Reduce operation tool for MapReduce workflow file manipulation.
				Aggregates and merges data from multiple Map tasks and generates final consolidated output.

				**Important Parameter Description:**
				- has_value: Boolean value indicating whether there is valid data to write
				  - If no valid data is found, set to false
				  - If there is data to output, set to true
				- data: Must provide data when has_value is true

				**IMPORTANT**: Tool will automatically terminate after operation completion.
				Please complete all content output in a single call.
				""";

		if (terminateColumns != null && !terminateColumns.isEmpty()) {
			String columnsFormat = String.join(", ", terminateColumns);
			baseDescription += String.format("""

					**Data Format Requirements (when has_value=true):**
					You must provide data in the following fixed format, each line containing: [%s]

					Example format:
					[
					  ["%s Example 1", "%s Example 1"],
					  ["%s Example 2", "%s Example 2"]
					]
					""", columnsFormat, terminateColumns.get(0),
					terminateColumns.size() > 1 ? terminateColumns.get(1) : "data", terminateColumns.get(0),
					terminateColumns.size() > 1 ? terminateColumns.get(1) : "data");
		}

		return baseDescription;
	}

	/**
	 * Generate parameters JSON for ReduceOperationTool with predefined columns format
	 * @param terminateColumns the columns specification (e.g., "url,description")
	 * @return JSON string for parameters schema
	 */
	private static String generateParametersJson(List<String> terminateColumns) {
		// Generate columns description from terminateColumns
		String columnsDesc = "data row list";
		if (terminateColumns != null && !terminateColumns.isEmpty()) {
			columnsDesc = "data row list, each row in the following format: [" + String.join(", ", terminateColumns)
					+ "]";
		}

		return """
				{
				    "type": "object",
				    "properties": {
				        "has_value": {
				            "type": "boolean",
				            "description": "Whether there is valid data to write. Set to false if no valid data is found, set to true when there is data"
				        },
				        "data": {
				            "type": "array",
				            "items": {
				                "type": "array",
				                "items": {"type": "string"}
				            },
				            "description": "%s (only required when has_value is true)"
				        }
				    },
				    "required": ["has_value"],
				    "additionalProperties": false
				}
				"""
			.formatted(columnsDesc);
	}

	private UnifiedDirectoryManager unifiedDirectoryManager;

	// Shared state manager for managing shared state between multiple Agent instances
	private MapReduceSharedStateManager sharedStateManager;

	// Class-level terminate columns configuration - takes precedence over input
	// parameters
	private final List<String> terminateColumns;

	// ==================== TerminableTool Related Fields ====================

	// Thread-safe lock to protect append operations and termination state
	private final ReentrantLock operationLock = new ReentrantLock();

	// Termination state related fields
	private volatile boolean isTerminated = false;

	private String lastTerminationMessage = "";

	private String terminationTimestamp = "";

	public ReduceOperationTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager, UnifiedDirectoryManager unifiedDirectoryManager,
			List<String> terminateColumns) {
		this.currentPlanId = planId;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.sharedStateManager = sharedStateManager;
		this.terminateColumns = terminateColumns;
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
		return getToolDescription(terminateColumns);
	}

	@Override
	public String getParameters() {
		return generateParametersJson(terminateColumns);
	}

	@Override
	public Class<ReduceOperationInput> getInputType() {
		return ReduceOperationInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "data-processing";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		// Use default terminate columns for static tool definition
		return getToolDefinition(null);
	}

	public static OpenAiApi.FunctionTool getToolDefinition(List<String> terminateColumns) {
		String parameters = generateParametersJson(terminateColumns);
		String description = getToolDescription(terminateColumns);
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Reduce operation
	 */
	@Override
	public ToolExecuteResult run(ReduceOperationInput input) {
		log.info("ReduceOperationTool input: hasValue={}", input.isHasValue());
		try {
			List<List<Object>> data = input.getData();
			boolean hasValue = input.isHasValue();

			// Use class-level terminateColumns
			List<String> effectiveTerminateColumns = this.terminateColumns;
			if (effectiveTerminateColumns == null || effectiveTerminateColumns.isEmpty()) {
				return new ToolExecuteResult("Error: terminate columns not configured for this tool");
			}

			// Check hasValue logic
			if (hasValue) {
				// When hasValue is true, data must be provided
				if (data == null || data.isEmpty()) {
					return new ToolExecuteResult("Error: data parameter is required when has_value is true");
				}

				// Validate data structure
				ToolExecuteResult validationResult = validateDataStructure(data, effectiveTerminateColumns);
				if (validationResult != null) {
					return validationResult; // Return validation error
				}

				// Convert structured data to JSON format and append
				String jsonContent = formatStructuredDataAsJson(effectiveTerminateColumns, data);
				return appendToFile(REDUCE_FILE_NAME, jsonContent);
			}
			else {
				// When hasValue is false, do not append anything but still mark as
				// terminated
				this.isTerminated = true;
				this.lastTerminationMessage = "No data to append, operation completed";
				this.terminationTimestamp = java.time.LocalDateTime.now().toString();
				log.info("Tool marked as terminated (no data to append) for planId: {}", currentPlanId);
				return new ToolExecuteResult("No data to append, operation completed successfully");
			}

		}
		catch (Exception e) {
			log.error("ReduceOperationTool execution failed", e);
			// Mark as terminated even on failure
			this.isTerminated = true;
			this.lastTerminationMessage = "Operation failed: " + e.getMessage();
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Validate data structure against expected terminate columns
	 * @param data the input data
	 * @param terminateColumns expected column structure
	 * @return ToolExecuteResult with error message if validation fails, null if valid
	 */
	private ToolExecuteResult validateDataStructure(List<List<Object>> data, List<String> terminateColumns) {
		int expectedColumnCount = terminateColumns.size();

		for (int i = 0; i < data.size(); i++) {
			List<Object> row = data.get(i);
			if (row.size() != expectedColumnCount) {
				String error = String.format("""
						Data structure inconsistent!
						Expected column count: %d
						Actual column count for row %d: %d

						**Required data structure:**
						Each row must contain: [%s]

						Example format:
						[
						  ["%s Example1", "%s Example1"],
						  ["%s Example2", "%s Example2"]
						]
						""", expectedColumnCount, i + 1, row.size(), String.join(", ", terminateColumns),
						terminateColumns.get(0), terminateColumns.size() > 1 ? terminateColumns.get(1) : "data",
						terminateColumns.get(0), terminateColumns.size() > 1 ? terminateColumns.get(1) : "data");
				return new ToolExecuteResult(error);
			}
		}
		return null; // Validation passed
	}

	/**
	 * Format structured data as JSON list format
	 * @param terminateColumns the column names
	 * @param data the data rows
	 * @return JSON formatted string representation of the structured data
	 */
	private String formatStructuredDataAsJson(List<String> terminateColumns, List<List<Object>> data) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"columns\": ").append(java.util.Arrays.toString(terminateColumns.toArray())).append(",\n");
		sb.append("  \"data\": [\n");

		for (int i = 0; i < data.size(); i++) {
			List<Object> row = data.get(i);
			sb.append("    ").append(java.util.Arrays.toString(row.toArray()));
			if (i < data.size() - 1) {
				sb.append(",");
			}
			sb.append("\n");
		}

		sb.append("  ]\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Append content to file in root plan directory Similar to
	 * InnerStorageTool.appendToFile() but operates on root plan directory This method is
	 * thread-safe and will set termination status after execution
	 */
	private ToolExecuteResult appendToFile(String fileName, String content) {
		operationLock.lock();
		try {
			if (content == null) {
				content = "";
			}

			// Get file from root plan directory
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			ensureDirectoryExists(rootPlanDir);

			// Get file path and append content
			Path filePath = rootPlanDir.resolve(fileName);

			String resultMessage;
			// If file doesn't exist, create new file
			if (!Files.exists(filePath)) {
				Files.writeString(filePath, content);
				log.info("File created and content added: {}", fileName);
				resultMessage = String.format("File created successfully and content added: %s", fileName);
			}
			else {
				// Append content (add newline)
				Files.writeString(filePath, "\n" + content, StandardOpenOption.APPEND);
				log.info("Content appended to file: {}", fileName);
				resultMessage = String.format("Content appended successfully: %s", fileName);
			}

			// Read the file and get last 3 lines with line numbers
			List<String> lines = Files.readAllLines(filePath);
			StringBuilder result = new StringBuilder();
			result.append(resultMessage).append("\n\n");
			result.append("Last 3 lines of file:\n");
			result.append("-".repeat(30)).append("\n");

			int totalLines = lines.size();
			int startLine = Math.max(0, totalLines - 3);

			for (int i = startLine; i < totalLines; i++) {
				result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
			}

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(currentPlanId, resultStr);
			}

			// Set termination status
			this.isTerminated = true;
			this.lastTerminationMessage = "Append operation completed successfully";
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			log.info("Tool marked as terminated after append operation for planId: {}", currentPlanId);

			return new ToolExecuteResult(resultStr);

		}
		catch (IOException e) {
			log.error("Failed to append to file", e);
			// Set termination status even if failed
			this.isTerminated = true;
			this.lastTerminationMessage = "Append operation failed: " + e.getMessage();
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			return new ToolExecuteResult("Failed to append to file: " + e.getMessage());
		}
		finally {
			operationLock.unlock();
		}
	}

	@Override
	public void cleanup(String planId) {
		// Clean up shared state
		if (sharedStateManager != null && planId != null) {
			sharedStateManager.cleanupPlanState(planId);
		}
		log.info("ReduceOperationTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(ReduceOperationInput input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Get inner storage root directory path
	 */
	private Path getInnerStorageRoot() {
		return unifiedDirectoryManager.getInnerStorageRoot();
	}

	/**
	 * Get plan directory path
	 */
	private Path getPlanDirectory(String planId) {
		return getInnerStorageRoot().resolve(planId);
	}

	/**
	 * Ensure directory exists
	 */
	private void ensureDirectoryExists(Path directory) throws IOException {
		unifiedDirectoryManager.ensureDirectoryExists(directory);
	}

	/**
	 * Get class-level terminate columns configuration
	 * @return terminate columns as comma-separated string
	 */
	public String getTerminateColumns() {
		if (this.terminateColumns == null || this.terminateColumns.isEmpty()) {
			return null;
		}
		return String.join(",", this.terminateColumns);
	}

	/**
	 * Get class-level terminate columns configuration as List
	 * @return terminate columns as List<String>
	 */
	public List<String> getTerminateColumnsList() {
		return this.terminateColumns == null ? null : new ArrayList<>(this.terminateColumns);
	}

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// Check if append operation has been executed, if so then can terminate
		return isTerminated;
	}

	/**
	 * Get termination status information, including original status and
	 * termination-related status
	 */
	@Override
	public String getCurrentToolStateString() {
		StringBuilder sb = new StringBuilder();

		// Original shared state information
		if (sharedStateManager != null && currentPlanId != null) {
			sb.append(sharedStateManager.getCurrentToolStateString(currentPlanId));
			sb.append("\n\n");
		}

		// Simplified termination status information
		sb.append(String.format("ReduceOperationTool: %s", isTerminated ? "🛑 Terminated" : "⚡ Active"));

		return sb.toString();
	}

	/**
	 * Check if tool has already terminated
	 */
	public boolean isTerminated() {
		return isTerminated;
	}

	/**
	 * Get last termination message
	 */
	public String getLastTerminationMessage() {
		return lastTerminationMessage;
	}

	/**
	 * Get termination timestamp
	 */
	public String getTerminationTimestamp() {
		return terminationTimestamp;
	}

}
