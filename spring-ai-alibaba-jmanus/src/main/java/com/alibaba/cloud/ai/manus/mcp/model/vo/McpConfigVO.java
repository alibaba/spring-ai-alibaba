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

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VO object for frontend display of McpConfig
 */
public class McpConfigVO {

	private Long id;

	private String mcpServerName;

	private McpConfigType connectionType;

	private String connectionConfig;

	private List<String> toolNames; // Add tool name list for frontend display

	private final ObjectMapper objectMapper;

	// New field-based properties
	private String command;

	private String url;

	private List<String> args;

	private Map<String, String> env;

	private McpConfigStatus status;

	/**
	 * Default constructor for Jackson deserialization
	 */
	public McpConfigVO() {
		this.objectMapper = new ObjectMapper();
		this.toolNames = new ArrayList<>();
	}

	public McpConfigVO(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.toolNames = new ArrayList<>();
	}

	public McpConfigVO(McpConfigEntity entity, ObjectMapper objectMapper) {
		this.id = entity.getId();
		this.mcpServerName = entity.getMcpServerName();
		this.connectionType = entity.getConnectionType();
		this.connectionConfig = entity.getConnectionConfig();
		this.status = entity.getStatus();
		this.objectMapper = objectMapper;
		this.toolNames = new ArrayList<>(); // Initialize as empty list, may need to get
											// from other places in actual use

		// Parse connectionConfig to field-based properties
		parseConnectionConfig();
	}

	/**
	 * Parse connectionConfig JSON to field-based properties
	 */
	private void parseConnectionConfig() {
		if (connectionConfig == null || connectionConfig.trim().isEmpty()) {
			return;
		}

		try {

			objectMapper.registerModule(new JavaTimeModule());
			JsonNode configNode = objectMapper.readTree(connectionConfig);

			// Parse command
			if (configNode.has("command")) {
				this.command = configNode.get("command").asText();
			}

			// Parse url
			if (configNode.has("url")) {
				this.url = configNode.get("url").asText();
			}

			// Parse args
			if (configNode.has("args")) {
				this.args = objectMapper.readValue(configNode.get("args").toString(),
						objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
			}

			// Parse env
			if (configNode.has("env")) {
				this.env = objectMapper.readValue(configNode.get("env").toString(),
						objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
			}

		}
		catch (JsonProcessingException e) {
			// If parsing fails, keep fields empty
			System.err.println("Failed to parse connectionConfig: " + e.getMessage());
		}
	}

	// Static method to convert VO list to entity list, add ObjectMapper parameter
	public static List<McpConfigVO> fromEntities(List<McpConfigEntity> entities, ObjectMapper objectMapper) {
		List<McpConfigVO> vos = new ArrayList<>();
		if (entities != null) {
			for (McpConfigEntity entity : entities) {
				vos.add(new McpConfigVO(entity, objectMapper));
			}
		}
		return vos;
	}

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

	public McpConfigType getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(McpConfigType connectionType) {
		this.connectionType = connectionType;
	}

	public String getConnectionConfig() {
		return connectionConfig;
	}

	public void setConnectionConfig(String connectionConfig) {
		this.connectionConfig = connectionConfig;
	}

	public List<String> getToolNames() {
		return toolNames;
	}

	public void setToolNames(List<String> toolNames) {
		this.toolNames = toolNames;
	}

	// Getter and setter for new fields
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

	public McpConfigStatus getStatus() {
		return status;
	}

	public void setStatus(McpConfigStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "McpConfigVO{" + "id=" + id + ", mcpServerName='" + mcpServerName + '\'' + ", connectionType="
				+ connectionType + ", connectionConfig='" + connectionConfig + '\'' + ", toolNames=" + toolNames
				+ ", command='" + command + '\'' + ", url='" + url + '\'' + ", args='" + args + '\'' + ", env='" + env
				+ '\'' + ", status=" + status + '}';
	}

}
