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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 批量MCP服务器导入请求VO（JSON方式） 用于批量导入MCP服务器配置
 */
public class McpServersRequestVO {

	/**
	 * 完整的JSON配置 格式：{"mcpServers": {"server-name": {"command": "...", "args": [...],
	 * "env": {...}}}}
	 */
	@JsonProperty("configJson")
	private String configJson;

	/**
	 * 是否覆盖现有配置
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
	 * 验证JSON格式是否有效
	 * @return true表示有效，false表示无效
	 */
	public boolean isValidJson() {
		if (configJson == null || configJson.trim().isEmpty()) {
			return false;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(configJson);

			// 检查是否包含mcpServers字段
			if (!jsonNode.has("mcpServers")) {
				return false;
			}

			// 检查mcpServers是否为对象
			JsonNode mcpServersNode = jsonNode.get("mcpServers");
			if (!mcpServersNode.isObject()) {
				return false;
			}

			// 检查是否至少有一个服务器配置
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
	 * 获取服务器配置数量
	 * @return 服务器数量
	 */
	public int getServerCount() {
		if (!isValidJson()) {
			return 0;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(configJson);
			JsonNode mcpServersNode = jsonNode.get("mcpServers");
			return mcpServersNode.size();
		}
		catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 获取服务器名称列表
	 * @return 服务器名称数组
	 */
	public String[] getServerNames() {
		if (!isValidJson()) {
			return new String[0];
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
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
	 * 标准化JSON格式 如果输入的是短格式JSON，自动转换为完整格式
	 * @return 标准化后的JSON字符串
	 */
	public String getNormalizedConfigJson() {
		if (configJson == null || configJson.trim().isEmpty()) {
			return configJson;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(configJson);

			// 如果已经包含mcpServers字段，直接返回
			if (jsonNode.has("mcpServers")) {
				return configJson;
			}

			// 如果是短格式JSON，转换为完整格式
			StringBuilder fullJsonBuilder = new StringBuilder();
			fullJsonBuilder.append("{\n  \"mcpServers\": ");
			fullJsonBuilder.append(configJson);
			fullJsonBuilder.append("\n}");

			return fullJsonBuilder.toString();
		}
		catch (Exception e) {
			// 如果解析失败，返回原始JSON
			return configJson;
		}
	}

	/**
	 * 验证请求数据是否有效
	 * @return true表示有效，false表示无效
	 */
	public boolean isValid() {
		return isValidJson();
	}

}
