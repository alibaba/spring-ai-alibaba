/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态工具搜索拦截器
 */
public class ToolSearchModelInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolSearchModelInterceptor.class);

	private static final String TOOL_SEARCH_NAME = "tool_search";

	private static final String RECURSION_DEPTH_KEY = "tool_search_recursion_depth";

	private static final String INJECTED_TOOLS_KEY = "tool_search_injected_tools";

	private final ToolSearcher toolSearcher;

	private final int maxResults;

	private final int maxRecursionDepth;

	private final ToolCallback toolSearchTool;

	private final ObjectMapper objectMapper;

	/**
	 * 缓存找到的工具，供 ToolCallbackResolver 使用
	 */
	private final Map<String, ToolCallback> cachedTools = new ConcurrentHashMap<>();

	/**
	 * ToolCallbackResolver 实现，用于在工具执行时动态提供工具
	 */
	private final ToolCallbackResolver toolCallbackResolver = new ToolCallbackResolver() {
		@Override
		public ToolCallback resolve(String toolName) {
			ToolCallback tool = cachedTools.get(toolName);
			if (tool != null) {
				log.debug("Resolved tool '{}' from cache", toolName);
			} else {
				log.debug("Tool '{}' not found in cache", toolName);
			}
			return tool;
		}
	};

	private ToolSearchModelInterceptor(Builder builder) {
		this.toolSearcher = builder.toolSearcher;
		this.maxResults = builder.maxResults;
		this.maxRecursionDepth = builder.maxRecursionDepth;
		this.toolSearchTool = ToolSearchTool.builder(toolSearcher)
			.withMaxResults(maxResults)
			.build();
		this.objectMapper = new ObjectMapper();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "ToolSearchModelInterceptor";
	}

	@Override
	public List<ToolCallback> getTools() {
		return Collections.emptyList();
	}

	public ToolCallbackResolver getToolCallbackResolver() {
		return toolCallbackResolver;
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		int currentDepth = getRecursionDepth(request);

		log.debug("Processing request at recursion depth: {}", currentDepth);

		if (currentDepth >= maxRecursionDepth) {
			log.warn("Maximum recursion depth ({}) reached, stopping tool search", maxRecursionDepth);
			return handler.call(request);
		}


		ModelResponse response = handler.call(request);

		Object messageObj = response.getMessage();
		if (!(messageObj instanceof AssistantMessage)) {
			log.debug("Response is not an AssistantMessage, returning directly");
			return response;
		}

		AssistantMessage message = (AssistantMessage) messageObj;

		List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
		if (toolCalls == null || toolCalls.isEmpty()) {
			log.debug("No tool calls in response, returning directly");
			return response;
		}

		// 收集所有的 tool_search 调用
		List<AssistantMessage.ToolCall> toolSearchCalls = new ArrayList<>();
		for (AssistantMessage.ToolCall toolCall : toolCalls) {
			if (TOOL_SEARCH_NAME.equals(toolCall.name())) {
				toolSearchCalls.add(toolCall);
			}
		}

		if (toolSearchCalls.isEmpty()) {
			log.debug("No tool_search call found, returning directly");
			return response;
		}

		log.info("Detected {} tool_search calls", toolSearchCalls.size());

		// 处理所有的 tool_search 调用，收集所有找到的工具
		List<ToolCallback> allFoundTools = new ArrayList<>();

		for (AssistantMessage.ToolCall toolSearchCall : toolSearchCalls) {
			String query = extractSearchQuery(toolSearchCall);
			if (query == null || query.isEmpty()) {
				log.warn("Failed to extract search query from tool_search call");
				continue;
			}

			log.info("Processing tool_search call with query: {}", query);

			List<ToolCallback> foundTools = toolSearcher.search(query, maxResults);
			if (foundTools.isEmpty()) {
				log.warn("No tools found for query: {}", query);
				continue;
			}

			log.info("Found {} tools for query: {}", foundTools.size(), query);

			// 缓存找到的工具，供 ToolCallbackResolver 使用
			for (ToolCallback tool : foundTools) {
				String toolName = tool.getToolDefinition().name();
				cachedTools.put(toolName, tool);
				log.debug("Cached tool: {}", toolName);

				// 避免重复添加
				if (!allFoundTools.contains(tool)) {
					allFoundTools.add(tool);
				}
			}
		}

		if (allFoundTools.isEmpty()) {
			log.warn("No tools found from any tool_search calls");
			return response;
		}

		List<ToolCallback> injectedTools = getInjectedTools(request);

		List<ToolCallback> mergedTools = new ArrayList<>(injectedTools);
		for (ToolCallback tool : allFoundTools) {
			if (!mergedTools.contains(tool)) {
				mergedTools.add(tool);
			}
		}

		log.debug("Total tools after merge: {}", mergedTools.size());
		ModelRequest newRequest = ModelRequest.builder(request)
			.options(null)
			.dynamicToolCallbacks(mergedTools)
			.context(request.getContext())
			.build();

		newRequest.getContext().put(RECURSION_DEPTH_KEY, currentDepth + 1);
		newRequest.getContext().put(INJECTED_TOOLS_KEY, mergedTools);
		log.debug("Recursively calling handler with {} tools at depth {}", mergedTools.size(), currentDepth + 1);
		return handler.call(newRequest);
	}

	/**
	 * 从请求上下文中获取当前递归深度
	 */
	private int getRecursionDepth(ModelRequest request) {
		if (request.getContext() == null) {
			return 0;
		}
		Object depth = request.getContext().get(RECURSION_DEPTH_KEY);
		if (depth instanceof Integer) {
			return (Integer) depth;
		}
		return 0;
	}

	/**
	 * 从请求上下文中获取已注入的工具列表
	 */
	@SuppressWarnings("unchecked")
	private List<ToolCallback> getInjectedTools(ModelRequest request) {
		if (request.getContext() == null) {
			return Collections.emptyList();
		}
		Object tools = request.getContext().get(INJECTED_TOOLS_KEY);
		if (tools instanceof List) {
			return (List<ToolCallback>) tools;
		}
		return Collections.emptyList();
	}

	/**
	 * 从tool_search调用中提取搜索关键词
	 */
	private String extractSearchQuery(AssistantMessage.ToolCall toolCall) {
		try {
			String arguments = toolCall.arguments();
			if (arguments == null || arguments.isEmpty()) {
				return null;
			}

			JsonNode jsonNode = objectMapper.readTree(arguments);
			JsonNode queryNode = jsonNode.get("query");
			if (queryNode != null) {
				return queryNode.asText();
			}
			return null;
		}
		catch (Exception e) {
			log.error("Failed to extract query from tool call arguments", e);
			return null;
		}
	}

	public static class Builder {

		private ToolSearcher toolSearcher;

		private int maxResults = 5;

		private int maxRecursionDepth = 3;

		public Builder toolSearcher(ToolSearcher toolSearcher) {
			this.toolSearcher = toolSearcher;
			return this;
		}

		public Builder maxResults(int maxResults) {
			this.maxResults = maxResults;
			return this;
		}

		public Builder maxRecursionDepth(int maxRecursionDepth) {
			this.maxRecursionDepth = maxRecursionDepth;
			return this;
		}

	public ToolSearchModelInterceptor build() {
		if (toolSearcher == null) {
			throw new IllegalArgumentException("toolSearcher cannot be null");
		}
		return new ToolSearchModelInterceptor(this);
	}

}

}
