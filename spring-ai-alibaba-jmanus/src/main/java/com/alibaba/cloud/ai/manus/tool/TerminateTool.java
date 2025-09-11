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
package com.alibaba.cloud.ai.manus.tool;

import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TerminateTool extends AbstractBaseTool<Map<String, Object>> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(TerminateTool.class);

	public static final String name = "terminate";

	private final String expectedReturnInfo;

	private String lastTerminationMessage = "";

	private boolean isTerminated = false;

	private String terminationTimestamp = "";

	private static String getDescriptions(String expectedReturnInfo) {
		// Simple description to avoid generating overly long content
		return "Terminate the current execution step with structured data. "
				+ "Provide data in JSON format with 'message' field and optional 'fileList' array containing file information.";
	}

	private static String generateParametersJson(String expectedReturnInfo) {
		String template = """
				{
				  "type": "object",
				  "properties": {
				    "message": {
				      "type": "string",
				      "description": "Comprehensive termination message that should include all relevant facts, viewpoints, details, and conclusions from the execution step. This message should provide a complete summary of what was accomplished, any important observations, key findings, and final outcomes. The message must explicitly mention and describe the data corresponding to the expected return information: %s"
				    },
				    "fileList": {
				      "type": "array",
				      "items": {
				        "type": "object",
				        "properties": {
				          "fileName": {
				            "type": "string",
				            "description": "Name of the file"
				          },
				          "fileDescription": {
				            "type": "string",
				            "description": "Detailed description of what the file contains. This should include a comprehensive summary of all content generated during this agent execution cycle. Every file created during this execution must be listed here with complete and accurate information about its contents."
				          }
				        },
				        "required": ["fileName", "fileDescription"]
				      },
				      "description": "Complete list of all files generated during this agent execution cycle. Every file created must be included with its name and a detailed description of its contents. This is mandatory for full transparency and auditing purposes."
				    },
				    "folderList": {
				      "type": "array",
				      "items": {
				        "type": "object",
				        "properties": {
				          "folderName": {
				            "type": "string",
				            "description": "Name of the folder"
				          },
				          "folderDescription": {
				            "type": "string",
				            "description": "Detailed description of what the folder contains. This should include a comprehensive summary of all content within this folder generated during this agent execution cycle."
				          }
				        },
				        "required": ["folderName", "folderDescription"]
				      },
				      "description": "Complete list of all folders generated during this agent execution cycle. Every folder created must be included with its name and a detailed description of its contents."
				    }
				  },
				  "required": ["message"]
				}
				""";

		return String.format(template, expectedReturnInfo != null ? expectedReturnInfo : "N/A");
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Termination Tool Status:
				- Current State: %s
				- Last Termination: %s
				- Termination Message: %s
				- Timestamp: %s
				- Plan ID: %s
				- Expected Return Info: %s
				""", isTerminated ? "🛑 Terminated" : "⚡ Active",
				isTerminated ? "Process was terminated" : "No termination recorded",
				lastTerminationMessage.isEmpty() ? "N/A" : lastTerminationMessage,
				terminationTimestamp.isEmpty() ? "N/A" : terminationTimestamp,
				currentPlanId != null ? currentPlanId : "N/A", expectedReturnInfo != null ? expectedReturnInfo : "N/A");
	}

	public TerminateTool(String planId, String expectedReturnInfo) {
		this.currentPlanId = planId;
		// If expectedReturnInfo is null or empty, use "message" as default
		this.expectedReturnInfo = (expectedReturnInfo == null || expectedReturnInfo.isEmpty()) ? "message"
				: expectedReturnInfo;
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		log.info("Terminate with input: {}", input);

		// Extract message from the structured data
		String message = formatStructuredData(input);
		this.lastTerminationMessage = message;
		this.isTerminated = true;
		this.terminationTimestamp = java.time.LocalDateTime.now().toString();

		return new ToolExecuteResult(message);
	}

	private String formatStructuredData(Map<String, Object> input) {
		// Convert input to JSON format
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
			return objectMapper.writeValueAsString(input);
		}
		catch (Exception e) {
			log.error("Failed to convert input to JSON format", e);
			// Fallback to simple string representation
			return input.toString();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return getDescriptions(this.expectedReturnInfo);
	}

	@Override
	public String getParameters() {
		return generateParametersJson(this.expectedReturnInfo);
	}

	@Override
	public Class<Map<String, Object>> getInputType() {
		@SuppressWarnings("unchecked")
		Class<Map<String, Object>> clazz = (Class<Map<String, Object>>) (Class<?>) Map.class;
		return clazz;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	// ==================== TerminableTool interface implementation ====================

	@Override
	public boolean canTerminate() {
		// TerminateTool can always be terminated as its purpose is to terminate execution
		return true;
	}

}
