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
package com.alibaba.cloud.ai.manus.mcp.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Batch MCP server import request VO (JSON method) for batch importing MCP server
 * configurations
 */
public class McpServersRequestVO {

	private final ObjectMapper objectMapper;

	/**
	 * Default constructor for Jackson deserialization
	 */
	public McpServersRequestVO() {
		this.objectMapper = new ObjectMapper();
	}

	public McpServersRequestVO(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Complete JSON configuration format: {"mcpServers": {"server-name": {"command":
	 * "...", "args": [...], "env": {...}}}}
	 */
	@JsonProperty("configJson")
	private String configJson;

	/**
	 * Whether to override existing configuration
	 */
	@JsonProperty("overwrite")
	private boolean overwrite = false;

	// Getters and Setters
	public String getConfigJson() {
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Validate if JSON format is valid
	 * @return true if valid, false if invalid
	 */
	public boolean isValidJson() {
		if (configJson == null || configJson.trim().isEmpty()) {
			return false;
		}

		try {
			JsonNode jsonNode = objectMapper.readTree(configJson);

			// Check if contains mcpServers field
			if (!jsonNode.has("mcpServers")) {
				return false;
			}

			// Check if mcpServers is an object
			JsonNode mcpServersNode = jsonNode.get("mcpServers");
			if (!mcpServersNode.isObject()) {
				return false;
			}

			// Check if has at least one server configuration
			if (mcpServersNode.size() == 0) {
				return false;
			}

			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get server configuration count
	 * @return Server count
	 */
	public int getServerCount() {
		if (!isValidJson()) {
			return 0;
		}

		try {
			JsonNode jsonNode = objectMapper.readTree(configJson);
			JsonNode mcpServersNode = jsonNode.get("mcpServers");
			return mcpServersNode.size();
		}
		catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Get server name list
	 * @return Server name array
	 */
	public String[] getServerNames() {
		if (!isValidJson()) {
			return new String[0];
		}

		try {
			JsonNode jsonNode = objectMapper.readTree(configJson);
			JsonNode mcpServersNode = jsonNode.get("mcpServers");

			String[] names = new String[mcpServersNode.size()];
			int index = 0;
			java.util.Iterator<String> fieldNames = mcpServersNode.fieldNames();
			while (fieldNames.hasNext()) {
				names[index++] = fieldNames.next();
			}
			return names;
		}
		catch (Exception e) {
			return new String[0];
		}
	}

	/**
	 * Normalize JSON format. If input is short format JSON, automatically convert to
	 * complete format
	 * @return Normalized JSON string
	 */
	public String getNormalizedConfigJson() {
		if (configJson == null || configJson.trim().isEmpty()) {
			return configJson;
		}

		try {
			JsonNode jsonNode = objectMapper.readTree(configJson);

			// If already contains mcpServers field, return directly
			if (jsonNode.has("mcpServers")) {
				return configJson;
			}

			// If it's short format JSON, convert to complete format
			StringBuilder fullJsonBuilder = new StringBuilder();
			fullJsonBuilder.append("{\n  \"mcpServers\": ");
			fullJsonBuilder.append(configJson);
			fullJsonBuilder.append("\n}");

			return fullJsonBuilder.toString();
		}
		catch (Exception e) {
			// If parsing fails, return original JSON
			return configJson;
		}
	}

	/**
	 * Validate if request data is valid
	 * @return true if valid, false if invalid
	 */
	public boolean isValid() {
		return isValidJson();
	}

}
