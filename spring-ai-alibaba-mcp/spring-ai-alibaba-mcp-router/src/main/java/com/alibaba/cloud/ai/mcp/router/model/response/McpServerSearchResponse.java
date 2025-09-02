/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.model.response;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MCP Server 搜索响应结果 用于封装搜索 MCP Server 的结果，包含匹配的服务列表和搜索元信息
 *
 * @author spring-ai-alibaba
 * @since 2025.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpServerSearchResponse {

	/**
	 * 搜索是否成功
	 */
	@JsonProperty("success")
	private boolean success;

	/**
	 * 错误消息（当 success 为 false 时）
	 */
	@JsonProperty("error_message")
	private String errorMessage;

	/**
	 * 搜索查询
	 */
	@JsonProperty("search_query")
	private String searchQuery;

	/**
	 * 匹配的服务数量
	 */
	@JsonProperty("total_count")
	private int totalCount;

	/**
	 * 匹配的 MCP Server 列表
	 */
	@JsonProperty("servers")
	private List<McpServerSearchResult> servers;

	/**
	 * 搜索建议（当没有找到结果时）
	 */
	@JsonProperty("suggestions")
	private List<String> suggestions;

	public McpServerSearchResponse() {
	}

	public McpServerSearchResponse(boolean success, String searchQuery, List<McpServerSearchResult> servers) {
		this.success = success;
		this.searchQuery = searchQuery;
		this.servers = servers;
		this.totalCount = servers != null ? servers.size() : 0;
	}

	/**
	 * 创建成功响应
	 */
	public static McpServerSearchResponse success(String searchQuery, List<McpServerSearchResult> servers) {
		return new McpServerSearchResponse(true, searchQuery, servers);
	}

	/**
	 * 创建失败响应
	 */
	public static McpServerSearchResponse error(String searchQuery, String errorMessage) {
		McpServerSearchResponse response = new McpServerSearchResponse();
		response.success = false;
		response.searchQuery = searchQuery;
		response.errorMessage = errorMessage;
		response.totalCount = 0;
		return response;
	}

	/**
	 * 搜索结果项
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class McpServerSearchResult {

		/**
		 * 服务名称
		 */
		@JsonProperty("name")
		private String name;

		/**
		 * 服务描述
		 */
		@JsonProperty("description")
		private String description;

		/**
		 * 协议类型
		 */
		@JsonProperty("protocol")
		private String protocol;

		/**
		 * 服务版本
		 */
		@JsonProperty("version")
		private String version;

		/**
		 * 服务端点
		 */
		@JsonProperty("endpoint")
		private String endpoint;

		/**
		 * 是否启用
		 */
		@JsonProperty("enabled")
		private Boolean enabled;

		/**
		 * 服务标签
		 */
		@JsonProperty("tags")
		private List<String> tags;

		/**
		 * 相似度分数 (0.0 - 1.0)
		 */
		@JsonProperty("similarity_score")
		private double similarityScore;

		public McpServerSearchResult() {
		}

		public McpServerSearchResult(McpServerInfo serverInfo) {
			this.name = serverInfo.getName();
			this.description = serverInfo.getDescription();
			this.protocol = serverInfo.getProtocol();
			this.version = serverInfo.getVersion();
			this.endpoint = serverInfo.getEndpoint();
			this.enabled = serverInfo.getEnabled();
			this.tags = serverInfo.getTags();
			this.similarityScore = serverInfo.getScore();
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public double getSimilarityScore() {
			return similarityScore;
		}

		public void setSimilarityScore(double similarityScore) {
			this.similarityScore = similarityScore;
		}

	}

	// Getters and Setters
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public List<McpServerSearchResult> getServers() {
		return servers;
	}

	public void setServers(List<McpServerSearchResult> servers) {
		this.servers = servers;
		this.totalCount = servers != null ? servers.size() : 0;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<String> suggestions) {
		this.suggestions = suggestions;
	}

	@Override
	public String toString() {
		return "McpServerSearchResponse{" + "success=" + success + ", errorMessage='" + errorMessage + '\''
				+ ", searchQuery='" + searchQuery + '\'' + ", totalCount=" + totalCount + ", servers=" + servers
				+ ", suggestions=" + suggestions + '}';
	}

}
