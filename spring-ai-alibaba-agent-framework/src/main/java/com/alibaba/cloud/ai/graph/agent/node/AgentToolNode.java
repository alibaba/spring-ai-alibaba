/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.agent.DefaultBuilder.POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING;
import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

public class AgentToolNode implements NodeActionWithConfig {
	private static final Logger logger = LoggerFactory.getLogger(AgentToolNode.class);

	private final String agentName;

	private boolean enableActingLog;

	private List<ToolCallback> toolCallbacks;

	private Map<String, Object> toolContext;

	private List<ToolInterceptor> toolInterceptors = new ArrayList<>();

	private ToolCallbackResolver toolCallbackResolver;

	private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	public AgentToolNode(Builder builder) {
		this.agentName = builder.agentName;
		this.enableActingLog = builder.enableActingLog;
		this.toolCallbackResolver = builder.toolCallbackResolver;
		this.toolCallbacks = builder.toolCallbacks;
		this.toolContext = builder.toolContext;
        this.toolExecutionExceptionProcessor = builder.toolExecutionExceptionProcessor;
    }

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	public void setToolInterceptors(List<ToolInterceptor> toolInterceptors) {
		this.toolInterceptors = toolInterceptors;
	}

	void setToolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
		this.toolCallbackResolver = toolCallbackResolver;
	}

	public List<ToolCallback> getToolCallbacks() {
		return toolCallbacks;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		Message lastMessage = messages.get(messages.size() - 1);

		Map<String, Object> updatedState = new HashMap<>();
		Map<String, Object> extraStateFromToolCall = new HashMap<>();
		if (lastMessage instanceof AssistantMessage assistantMessage) {
			// execute the tool function
			List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting with {} tools.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, assistantMessage.getToolCalls().size());
			}

			Boolean returnDirect = null;
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				// Execute tool call with interceptor chain
				ToolCallResponse response = executeToolCallWithInterceptors(toolCall, state, config, extraStateFromToolCall);
				toolResponses.add(response.toToolResponse());
				returnDirect = shouldReturnDirect(toolCall, returnDirect);
			}

			ToolResponseMessage.Builder builder = ToolResponseMessage.builder()
					.responses(toolResponses);
			if (returnDirect != null && returnDirect) {
				builder.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON));
			}
			ToolResponseMessage toolResponseMessage = builder.build();

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting returned: {}", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, toolResponseMessage);
			}

			updatedState.put("messages", toolResponseMessage);
		} else if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
			if (messages.size() < 2) {
				throw new IllegalStateException("Cannot find AssistantMessage before ToolResponseMessage");
			}
			Message secondLastMessage = messages.get(messages.size() - 2);
			if (!(secondLastMessage instanceof AssistantMessage assistantMessage)) {
				throw new IllegalStateException("Message before ToolResponseMessage is not an AssistantMessage");
			}

			List<ToolResponseMessage.ToolResponse> existingResponses = toolResponseMessage.getResponses();
			List<ToolResponseMessage.ToolResponse> allResponses = new ArrayList<>(existingResponses);

			Set<String> executedToolIds = existingResponses.stream()
					.map(ToolResponseMessage.ToolResponse::id)
					.collect(Collectors.toSet());

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting with {} tools ({} tools provided results).", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, assistantMessage.getToolCalls().size(), existingResponses.size());
			}

			Boolean returnDirect = null;
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				if (executedToolIds.contains(toolCall.id())) {
					// For already executed tools, check their returnDirect status
					returnDirect = shouldReturnDirect(toolCall, returnDirect);
					continue;
				}

				// Execute tool call with interceptor chain
				ToolCallResponse response = executeToolCallWithInterceptors(toolCall, state, config, extraStateFromToolCall);
				allResponses.add(response.toToolResponse());
				returnDirect = shouldReturnDirect(toolCall, returnDirect);
			}

			List<Object> newMessages = new ArrayList<>();

			ToolResponseMessage.Builder builder = ToolResponseMessage.builder()
					.responses(allResponses);
			if (returnDirect != null && returnDirect) {
				builder.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON));
			}
			ToolResponseMessage newToolResponseMessage = builder.build();

			newMessages.add(newToolResponseMessage);
			newMessages.add(new RemoveByHash<>(toolResponseMessage));
			updatedState.put("messages", newMessages);

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting successfully returned.", config.threadId()
						.orElse(THREAD_ID_DEFAULT), agentName);
				if (logger.isDebugEnabled()) {
					logger.debug("[ThreadId {}] Agent {} acting returned: {}", config.threadId()
							.orElse(THREAD_ID_DEFAULT), agentName, toolResponseMessage);
				}
			}

		} else {
			throw new IllegalStateException("Last message is neither an AssistantMessage nor an ToolResponseMessage");
		}

		// Merge extra state from tool calls
		updatedState.putAll(extraStateFromToolCall);
		return updatedState;
	}

	@NotNull
	private Boolean shouldReturnDirect(AssistantMessage.ToolCall toolCall, Boolean returnDirect) {
		String toolName = toolCall.name();
		ToolCallback toolCallback = toolCallbacks.stream()
				.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
				.findFirst()
				.orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

		if (returnDirect == null) {
			returnDirect = toolCallback.getToolMetadata().returnDirect();
		}
		else {
			returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
		}
		return returnDirect;
	}

	/**
	 * Execute a tool call with interceptor chain support.
	 */
	private ToolCallResponse executeToolCallWithInterceptors(
			AssistantMessage.ToolCall toolCall,
			OverAllState state,
			RunnableConfig config,
			Map<String, Object> extraStateFromToolCall) {

		// Create ToolCallRequest
		ToolCallRequest request = ToolCallRequest.builder()
				.toolCall(toolCall)
				.context(config.metadata().orElse(new HashMap<>()))
				.executionContext(new ToolCallExecutionContext(config, state))
				.build();

		// Create base handler that actually executes the tool
		ToolCallHandler baseHandler = req -> {
			ToolCallback toolCallback = resolve(req.getToolName());

			if (toolCallback == null) {
				logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING, req.getToolName());
				throw new IllegalStateException("No ToolCallback found for tool name: " + req.getToolName());
			}

			if (enableActingLog) {
				logger.info("[ThreadId {}] Agent {} acting, executing tool {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, req.getToolName());
			}

			String result;
			try {
				Map<String, Object> toolContextMap = new HashMap<>(toolContext);
				toolContextMap.putAll(req.getContext());
				// Handle FunctionToolCallback and MethodToolCallback, which support passing state and config in ToolContext.
				if (toolCallback instanceof FunctionToolCallback<?, ?> || toolCallback instanceof MethodToolCallback) {
					toolContextMap.putAll(Map.of(AGENT_STATE_CONTEXT_KEY, state, AGENT_CONFIG_CONTEXT_KEY, config, AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraStateFromToolCall));
					result = toolCallback.call(req.getArguments(), new ToolContext(toolContextMap));
				} else {
					// MCP tools receive the merged request/context map but do not receive agent state or RunnableConfig keys in ToolContext.

					result = toolCallback.call(req.getArguments(), new ToolContext(toolContextMap));
				}

				if (enableActingLog) {
					logger.info("[ThreadId {}] Agent {} acting, tool {} finished", config.threadId()
									.orElse(THREAD_ID_DEFAULT), agentName, req.getToolName());
					if (logger.isDebugEnabled()) {
						logger.debug("Tool {} returned: {}", req.getToolName(), result);
					}
				}
			} catch (ToolExecutionException e) {
				logger.error("[ThreadId {}] Agent {} acting, tool {} execution failed, handle to {} processor to decide the next move (terminate or continue). "
						, config.threadId().orElse(THREAD_ID_DEFAULT), agentName, req.getToolName(), toolExecutionExceptionProcessor.getClass().getName(), e);
				result = toolExecutionExceptionProcessor.process(e);
			}

			return ToolCallResponse.of(req.getToolCallId(), req.getToolName(), result);
		};

		// Chain interceptors if any
		ToolCallHandler chainedHandler = InterceptorChain.chainToolInterceptors(
			toolInterceptors, baseHandler);

		// Execute the chained handler
		return chainedHandler.call(request);
	}

	private ToolCallback resolve(String toolName) {
		return toolCallbacks.stream()
			.filter(callback -> callback.getToolDefinition().name().equals(toolName))
			.findFirst()
			.orElseGet(() -> toolCallbackResolver == null ? null : toolCallbackResolver.resolve(toolName));
	}

	public String getName() {
		return AGENT_TOOL_NAME;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String agentName;

		private boolean enableActingLog;

		private List<ToolCallback> toolCallbacks = new ArrayList<>();

		private Map<String, Object> toolContext = new HashMap<>();

		private ToolCallbackResolver toolCallbackResolver;

		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

		private Builder() {
		}

		public Builder agentName(String agentName) {
			this.agentName = agentName;
			return this;
		}

		public Builder enableActingLog(boolean enableActingLog) {
			this.enableActingLog = enableActingLog;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		public Builder toolContext(Map<String, Object> toolContext) {
			this.toolContext = new HashMap<>(toolContext);
			return this;
		}

		public Builder toolExecutionExceptionProcessor(ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
			return this;
		}

		public AgentToolNode build() {
			return new AgentToolNode(this);
		}

	}

}
