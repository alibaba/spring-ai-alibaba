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
 * 实现按需加载工具的能力，显著降低Token成本
 *
 * 工作原理：
 * 1. 第一次调用时只注入ToolSearchTool，不注入defaultTools
 * 2. 监听LLM响应，检测是否调用了tool_search
 * 3. 如果调用了，提取搜索关键词，执行搜索
 * 4. 将搜索到的工具动态添加到下一轮请求中
 * 5. 递归调用LLM，直到没有tool_search调用或达到最大深度
 *
 * 示例用法：
 * <pre>
 * ToolSearcher searcher = new LuceneToolSearcher();
 * searcher.indexTools(allTools);
 *
 * ToolSearchModelInterceptor interceptor = ToolSearchModelInterceptor.builder()
 *     .toolSearcher(searcher)
 *     .maxResults(5)
 *     .maxRecursionDepth(3)
 *     .build();
 * </pre>
 *
 * @author ikeike443
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
	 * Key: tool name, Value: ToolCallback
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
		// 拦截器提供ToolSearchTool
		return Collections.singletonList(toolSearchTool);
	}

	/**
	 * 获取 ToolCallbackResolver，用于动态解析工具
	 * 这个 resolver 应该被设置到 AgentToolNode 中
	 *
	 * @return ToolCallbackResolver 实例
	 */
	public ToolCallbackResolver getToolCallbackResolver() {
		return toolCallbackResolver;
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		// 获取当前递归深度
		int currentDepth = getRecursionDepth(request);

		log.debug("Processing request at recursion depth: {}", currentDepth);

		// 检查是否超过最大递归深度
		if (currentDepth >= maxRecursionDepth) {
			log.warn("Maximum recursion depth ({}) reached, stopping tool search", maxRecursionDepth);
			return handler.call(request);
		}

		// 第一次调用：只注入ToolSearchTool，不注入其他工具
		if (currentDepth == 0) {
			log.debug("First call, injecting only ToolSearchTool");
			request = ModelRequest.builder(request)
				.dynamicToolCallbacks(Collections.singletonList(toolSearchTool))
				.build();
		}

		// 调用LLM
		ModelResponse response = handler.call(request);

		// 检查响应是否包含工具调用
		Object messageObj = response.getMessage();
		if (!(messageObj instanceof AssistantMessage)) {
			log.debug("Response is not an AssistantMessage, returning directly");
			return response;
		}

		AssistantMessage message = (AssistantMessage) messageObj;

		// 检查是否调用了tool_search
		List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
		if (toolCalls == null || toolCalls.isEmpty()) {
			log.debug("No tool calls in response, returning directly");
			return response;
		}

		// 查找tool_search调用
		AssistantMessage.ToolCall toolSearchCall = null;
		for (AssistantMessage.ToolCall toolCall : toolCalls) {
			if (TOOL_SEARCH_NAME.equals(toolCall.name())) {
				toolSearchCall = toolCall;
				break;
			}
		}

		if (toolSearchCall == null) {
			log.debug("No tool_search call found, returning directly");
			return response;
		}

		// 提取搜索关键词
		String query = extractSearchQuery(toolSearchCall);
		if (query == null || query.isEmpty()) {
			log.warn("Failed to extract search query from tool_search call");
			return response;
		}

		log.info("Detected tool_search call with query: {}", query);

		// 搜索工具
		List<ToolCallback> foundTools = toolSearcher.search(query, maxResults);
		if (foundTools.isEmpty()) {
			log.warn("No tools found for query: {}", query);
			return response;
		}

		log.info("Found {} tools for query: {}", foundTools.size(), query);

		// 获取已注入的工具列表
		List<ToolCallback> injectedTools = getInjectedTools(request);

		// 合并新找到的工具
		List<ToolCallback> mergedTools = new ArrayList<>(injectedTools);
		mergedTools.add(toolSearchTool); // 保留ToolSearchTool
		for (ToolCallback tool : foundTools) {
			if (!mergedTools.contains(tool)) {
				mergedTools.add(tool);
			}
		}

		log.debug("Total tools after merge: {}", mergedTools.size());

		// 创建新的请求，增加递归深度
		// 重要：不传递 options，避免与 dynamicToolCallbacks 中的工具重复
		// AgentLlmNode 会使用 dynamicToolCallbacks 创建新的 options
		ModelRequest newRequest = ModelRequest.builder(request)
			.options(null)  // 清空 options，避免重复
			.dynamicToolCallbacks(mergedTools)
			.context(request.getContext())
			.build();

		// 更新上下文
		newRequest.getContext().put(RECURSION_DEPTH_KEY, currentDepth + 1);
		newRequest.getContext().put(INJECTED_TOOLS_KEY, mergedTools);

		// 递归调用
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

