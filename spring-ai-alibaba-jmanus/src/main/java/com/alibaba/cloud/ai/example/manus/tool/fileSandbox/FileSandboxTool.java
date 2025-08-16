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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File Sandbox Tool - Provides secure file handling capabilities for Agent Allows Agent
 * to safely process user-uploaded files within a sandboxed environment
 */
public class FileSandboxTool extends AbstractBaseTool<FileSandboxTool.SandboxInput> {

	private static final Logger log = LoggerFactory.getLogger(FileSandboxTool.class);

	/**
	 * Input class for file sandbox operations
	 */
	public static class SandboxInput {

		private String action;

		private String sandboxId;

		private String fileName;

		private String filePath;

		private String content;

		private Map<String, Object> parameters;

		private List<String> allowedOperations;

		public SandboxInput() {
		}

		// Getters and Setters
		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getSandboxId() {
			return sandboxId;
		}

		public void setSandboxId(String sandboxId) {
			this.sandboxId = sandboxId;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public Map<String, Object> getParameters() {
			return parameters;
		}

		public void setParameters(Map<String, Object> parameters) {
			this.parameters = parameters;
		}

		public List<String> getAllowedOperations() {
			return allowedOperations;
		}

		public void setAllowedOperations(List<String> allowedOperations) {
			this.allowedOperations = allowedOperations;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

	}

	private final FileSandboxManager sandboxManager;

	public FileSandboxTool(FileSandboxManager sandboxManager) {
		this.sandboxManager = sandboxManager;
	}

	private final String PARAMETERS = """
			{
			  "oneOf": [
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "list_files"
					},
					"sandboxId": {
					  "type": "string",
					  "description": "Sandbox instance ID (optional, uses current plan's sandbox if not specified)"
					}
				  },
				  "required": ["action"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "read_file"
					},
					"fileName": {
					  "type": "string",
					  "description": "Name of the file to read"
					},
					"sandboxId": {
					  "type": "string",
					  "description": "Sandbox instance ID (optional)"
					}
				  },
				  "required": ["action", "fileName"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "get_file_info"
					},
					"fileName": {
					  "type": "string",
					  "description": "Name of the file to get information about"
					},
					"sandboxId": {
					  "type": "string",
					  "description": "Sandbox instance ID (optional)"
					}
				  },
				  "required": ["action", "fileName"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "process_file"
					},
					"fileName": {
					  "type": "string",
					  "description": "Name of the file to process"
					},
					"parameters": {
					  "type": "object",
					  "description": "Processing parameters specific to file type",
					  "properties": {
						"operation": {
						  "type": "string",
						  "enum": ["parse", "analyze", "extract", "convert"],
						  "description": "Type of processing to perform"
						},
						"format": {
						  "type": "string",
						  "description": "Target format for conversion operations"
						}
					  }
					},
					"sandboxId": {
					  "type": "string",
					  "description": "Sandbox instance ID (optional)"
					}
				  },
				  "required": ["action", "fileName"],
				  "additionalProperties": false
				},
				{
				  "type": "object",
				  "properties": {
					"action": {
					  "type": "string",
					  "const": "create_file"
					},
					"fileName": {
					  "type": "string",
					  "description": "Name of the file to create"
					},
					"content": {
					  "type": "string",
					  "description": "File content"
					},
					"sandboxId": {
					  "type": "string",
					  "description": "Sandbox instance ID (optional)"
					}
				  },
				  "required": ["action", "fileName", "content"],
				  "additionalProperties": false
				}
			  ]
			}
			""";

	@Override
	public String getName() {
		return "file_sandbox";
	}

	@Override
	public String getDescription() {
		return "Provides secure access to user-uploaded files within a sandboxed environment. "
				+ "Supports reading, processing, and analyzing files safely. "
				+ "All file operations are restricted to the current plan's sandbox for security.";
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public ToolExecuteResult run(SandboxInput input) {
		try {
			log.info("FileSandboxTool input: action={}, fileName={}, sandboxId={}", input.getAction(),
					input.getFileName(), input.getSandboxId());

			// Use current plan's sandbox if sandboxId not specified
			String activeSandboxId = input.getSandboxId() != null ? input.getSandboxId() : currentPlanId;

			switch (input.getAction()) {
				case "list_files":
					return listFiles(activeSandboxId);
				case "read_file":
					return readFile(activeSandboxId, input.getFileName());
				case "get_file_info":
					return getFileInfo(activeSandboxId, input.getFileName());
				case "process_file":
					return processFile(activeSandboxId, input.getFileName(), input.getParameters());
				case "create_file":
					return createFile(activeSandboxId, input.getFileName(), input.getContent());
				default:
					return new ToolExecuteResult("Error: Unknown action: " + input.getAction());
			}
		}
		catch (Exception e) {
			log.error("Error in FileSandboxTool", e);
			return new ToolExecuteResult("Error: File sandbox operation failed: " + e.getMessage());
		}
	}

	private ToolExecuteResult listFiles(String sandboxId) {
		try {
			List<SandboxFile> files = sandboxManager.listFiles(sandboxId);

			StringBuilder result = new StringBuilder("Files in sandbox:\n");
			if (files.isEmpty()) {
				result.append("No files found in the sandbox.");
			}
			else {
				for (SandboxFile file : files) {
					result.append(String.format("- %s (%s, %d bytes, uploaded: %s)\n", file.getName(), file.getType(),
							file.getSize(), file.getUploadTime()));
				}
			}

			return new ToolExecuteResult(result.toString());
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error: Failed to list files: " + e.getMessage());
		}
	}

	private ToolExecuteResult readFile(String sandboxId, String fileName) {
		try {
			String content = sandboxManager.readFile(sandboxId, fileName);
			return new ToolExecuteResult("File content:\n" + content);
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error: Failed to read file: " + e.getMessage());
		}
	}

	private ToolExecuteResult getFileInfo(String sandboxId, String fileName) {
		try {
			SandboxFile fileInfo = sandboxManager.getFileInfo(sandboxId, fileName);

			String info = String.format("""
					File Information:
					- Name: %s
					- Type: %s
					- Size: %d bytes
					- Upload Time: %s
					- MIME Type: %s
					- Status: %s
					""", fileInfo.getName(), fileInfo.getType(), fileInfo.getSize(), fileInfo.getUploadTime(),
					fileInfo.getMimeType(), fileInfo.getStatus());

			return new ToolExecuteResult(info);
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error: Failed to get file info: " + e.getMessage());
		}
	}

	private ToolExecuteResult processFile(String sandboxId, String fileName, Map<String, Object> parameters) {
		try {
			String operation = parameters != null ? (String) parameters.get("operation") : "analyze";
			String result = sandboxManager.processFile(sandboxId, fileName, operation, parameters);
			return new ToolExecuteResult("File processing result:\n" + result);
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error: Failed to process file: " + e.getMessage());
		}
	}

	private ToolExecuteResult createFile(String sandboxId, String fileName, String content) {
		try {
			sandboxManager.createFile(sandboxId, fileName, content);
			return new ToolExecuteResult("File created successfully: " + fileName);
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error: Failed to create file: " + e.getMessage());
		}
	}

	@Override
	public Class<SandboxInput> getInputType() {
		return SandboxInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "file-tools";
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder status = new StringBuilder();
			status.append("FileSandboxTool Status:\n");
			status.append("- Current Plan ID: ").append(currentPlanId != null ? currentPlanId : "Not set").append("\n");

			if (currentPlanId != null) {
				try {
					var files = sandboxManager.listFiles(currentPlanId);
					status.append("- Files in Sandbox: ").append(files.size()).append("\n");
					for (var file : files) {
						status.append("  * ").append(file.getName()).append(" (").append(file.getType()).append(")\n");
					}
				}
				catch (Exception e) {
					status.append("- Files in Sandbox: Error loading (").append(e.getMessage()).append(")\n");
				}
			}

			return status.toString();
		}
		catch (Exception e) {
			return "FileSandboxTool Status: Error getting status - " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up file sandbox resources for plan: {}", planId);
			try {
				sandboxManager.cleanupSandbox(planId);
			}
			catch (Exception e) {
				log.error("Error cleaning up sandbox for plan: {}", planId, e);
			}
		}
	}

}
