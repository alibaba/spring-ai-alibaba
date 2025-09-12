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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single MCP server operation request VO (form method) for adding and updating single MCP
 * server configuration
 */
public class McpServerRequestVO {

	/**
	 * Server ID for distinguishing add/update operations. null means add, non-null means
	 * update
	 */
	private Long id;

	/**
	 * MCP server name
	 */
	@JsonProperty("mcpServerName")
	private String mcpServerName;

	/**
	 * Connection type: STUDIO, SSE, STREAMING
	 */
	@JsonProperty("connectionType")
	private String connectionType;

	/**
	 * Command (required for STUDIO type)
	 */
	private String command;

	/**
	 * URL (required for SSE/STREAMING type)
	 */
	private String url;

	/**
	 * Parameter list (optional for STUDIO type)
	 */
	private List<String> args;

	/**
	 * Environment variables (optional for STUDIO type)
	 */
	private Map<String, String> env;

	/**
	 * Status: ENABLE, DISABLE
	 */
	private String status;

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMcpServerName() {
		return mcpServerName;
	}

	public void setMcpServerName(String mcpServerName) {
		this.mcpServerName = mcpServerName;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Determine if it's an update operation
	 * @return true for update, false for add
	 */
	public boolean isUpdate() {
		return id != null;
	}

	/**
	 * Validate if request data is valid
	 * @return true if valid, false if invalid
	 */
	public boolean isValid() {
		return validateWithDetails().isEmpty();
	}

	/**
	 * Validate request data and return detailed error information
	 * @return Error message list, empty list means validation passed
	 */
	public List<String> validateWithDetails() {
		List<String> errors = new ArrayList<>();

		// Basic field validation
		if (mcpServerName == null || mcpServerName.trim().isEmpty()) {
			errors.add("MCP name cannot be empty");
		}

		if (connectionType == null || connectionType.trim().isEmpty()) {
			errors.add("Connection type cannot be empty");
		}

		// Validate required fields based on connection type
		if (connectionType != null) {
			String connectionTypeUpper = connectionType.toUpperCase();
			switch (connectionTypeUpper) {
				case "STUDIO":
					if (command == null || command.trim().isEmpty()) {
						errors.add("STUDIO type must provide command");
					}
					break;
				case "SSE":
					if (url == null || url.trim().isEmpty()) {
						errors.add("SSE type must provide URL");
					}
					else if (!isValidUrlFormat(url)) {
						errors.add("SSE type URL format is invalid: " + url);
					}
					else if (!isSSEUrl(url)) {
						errors.add("SSE type URL path must contain 'sse', current URL: " + url);
					}
					break;
				case "STREAMING":
					if (url == null || url.trim().isEmpty()) {
						errors.add("STREAMING type must provide URL");
					}
					else if (!isValidUrlFormat(url)) {
						errors.add("STREAMING type URL format is invalid: " + url);
					}
					else if (isSSEUrl(url)) {
						errors.add("STREAMING type URL path cannot contain 'sse', current URL: " + url);
					}
					break;
				default:
					errors.add("Unsupported connection type: " + connectionTypeUpper);
					break;
			}
		}

		return errors;
	}

	/**
	 * Validate if URL format is valid
	 * @param url Server URL
	 * @return Whether it's a valid URL format
	 */
	private boolean isValidUrlFormat(String url) {
		if (url == null || url.trim().isEmpty()) {
			return false;
		}

		try {
			new java.net.URL(url.trim());
			return true;
		}
		catch (java.net.MalformedURLException e) {
			return false;
		}
	}

	/**
	 * Determine if URL is SSE connection (consistent with McpServerConfig)
	 * @param url Server URL
	 * @return Whether it's SSE URL
	 */
	private boolean isSSEUrl(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		try {
			java.net.URL parsedUrl = new java.net.URL(url);
			String path = parsedUrl.getPath();

			// Check if path contains sse
			boolean pathContainsSse = path != null && path.toLowerCase().contains("sse");

			return pathContainsSse;
		}
		catch (java.net.MalformedURLException e) {
			// If URL format is invalid, return false
			return false;
		}
	}

	/**
	 * Build JSON configuration for single server
	 * @return JSON string
	 */
	public String buildConfigJson() {
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{");

		// Add command (if exists)
		if (command != null && !command.trim().isEmpty()) {
			jsonBuilder.append("\"command\":\"").append(command).append("\"");
		}

		// Add url (if exists)
		if (url != null && !url.trim().isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"url\":\"").append(url).append("\"");
		}

		// Add args (if exists)
		if (args != null && !args.isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"args\":[");
			for (int i = 0; i < args.size(); i++) {
				if (i > 0) {
					jsonBuilder.append(",");
				}
				jsonBuilder.append("\"").append(args.get(i)).append("\"");
			}
			jsonBuilder.append("]");
		}

		// Add env (if exists)
		if (env != null && !env.isEmpty()) {
			if (jsonBuilder.length() > 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\"env\":{");
			boolean first = true;
			for (Map.Entry<String, String> entry : env.entrySet()) {
				if (!first) {
					jsonBuilder.append(",");
				}
				jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
				first = false;
			}
			jsonBuilder.append("}");
		}

		jsonBuilder.append("}");
		return jsonBuilder.toString();
	}

	/**
	 * Build complete MCP configuration JSON (including mcpServers wrapper)
	 * @return Complete JSON string
	 */
	public String buildFullConfigJson() {
		StringBuilder fullJsonBuilder = new StringBuilder();
		fullJsonBuilder.append("{\n  \"mcpServers\": {\n");
		fullJsonBuilder.append("    \"").append(mcpServerName).append("\": ");
		fullJsonBuilder.append(buildConfigJson());
		fullJsonBuilder.append("\n  }\n}");
		return fullJsonBuilder.toString();
	}

}
