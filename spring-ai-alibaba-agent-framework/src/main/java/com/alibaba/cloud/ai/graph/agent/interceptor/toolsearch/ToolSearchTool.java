/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 工具搜索工具
 */
public class ToolSearchTool implements BiFunction<ToolSearchTool.Request, ToolContext, ToolSearchTool.Response> {

	private static final Logger log = LoggerFactory.getLogger(ToolSearchTool.class);

	public static final String DEFAULT_TOOL_DESCRIPTION = """
			Search for available tools by keyword or description.
			Use this when you need a tool but it's not currently available.

			Example queries:
			- "weather" - find weather-related tools
			- "database query" - find tools for querying databases
			- "file operations" - find file manipulation tools
			""";

	private final ToolSearcher toolSearcher;

	private final int maxResults;

	private final ObjectMapper objectMapper;

	public ToolSearchTool(ToolSearcher toolSearcher, int maxResults) {
		this.toolSearcher = toolSearcher;
		this.maxResults = maxResults;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		try {
			String query = request.query();

			log.info("Searching for tools with query: {}", query);

			// 搜索工具
			List<ToolCallback> foundTools = toolSearcher.search(query, maxResults);

			if (foundTools.isEmpty()) {
				log.warn("No tools found for query: {}", query);
				return new Response(query, 0, List.of(),
						String.format("No tools found matching '%s'. Try a different search term.", query));
			}

		// 构建工具Schema列表
		List<Map<String, Object>> schemas = foundTools.stream().map(tool -> {
			try {
				String schemaJson = toolSearcher.getToolSchema(tool);
				TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
				return objectMapper.readValue(schemaJson, typeRef);
			}
			catch (Exception e) {
				log.error("Failed to parse schema for tool: {}", tool.getToolDefinition().name(), e);
				return new HashMap<String, Object>();
			}
		}).collect(Collectors.toList());			String message = String.format("Found %d tools matching '%s'. These tools are now available for use.",
					foundTools.size(), query);

			log.info("Successfully found {} tools for query: {}", foundTools.size(), query);

			return new Response(query, foundTools.size(), schemas, message);
		}
		catch (Exception e) {
			log.error("Error searching for tools", e);
			return new Response("", 0, List.of(), "Error: Failed to search tools - " + e.getMessage());
		}
	}

	@JsonClassDescription("Request to search for tools by keyword or description")
	public record Request(
			@JsonProperty(required = true, value = "query") @JsonPropertyDescription("The search keyword or natural language description of the needed tool") String query) {
	}

	public record Response(@JsonPropertyDescription("The search query") String query,
			@JsonPropertyDescription("Number of tools found") int count,
			@JsonPropertyDescription("List of found tool schemas") List<Map<String, Object>> found_tools,
			@JsonPropertyDescription("Status message") String message) {
	}

	public static Builder builder(ToolSearcher toolSearcher) {
		return new Builder(toolSearcher);
	}

	public static class Builder {

		private final ToolSearcher toolSearcher;

		private String name = "tool_search";

		private String description = DEFAULT_TOOL_DESCRIPTION;

		private int maxResults = 5;

		public Builder(ToolSearcher toolSearcher) {
			this.toolSearcher = toolSearcher;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder withMaxResults(int maxResults) {
			this.maxResults = maxResults;
			return this;
		}

	public ToolCallback build() {
		return FunctionToolCallback.builder(name, new ToolSearchTool(toolSearcher, maxResults))
			.description(description)
			.inputType(Request.class)
			.build();
	}

}

}
