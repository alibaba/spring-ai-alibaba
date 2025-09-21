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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Agent节点
public class AgentNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(AgentNode.class);

	private final ChatClient chatClient;

	private final ToolCallback[] toolCallbacks;

	private final Strategy strategy;

	// Prompt内可以有变量，格式为{varName}，将在正式调用Client前替换占位变量
	private final String systemPrompt;

	private final String userPrompt;

	private final Integer maxIterations;

	private final String outputKey;

	private final RetryTemplate retryTemplate;

	public enum Strategy {

		REACT, TOOL_CALLING

	}

	public AgentNode(ChatClient chatClient, ToolCallback[] toolCallbacks, Strategy strategy, String systemPrompt,
			String userPrompt, Integer maxIterations, String outputKey) {
		this.chatClient = chatClient;
		this.strategy = strategy == null ? Strategy.REACT : strategy;
		this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
		this.userPrompt = userPrompt == null ? "" : userPrompt;
		this.maxIterations = maxIterations == null ? 1 : maxIterations;
		this.outputKey = outputKey == null ? "agent_output" : outputKey;
		if (this.chatClient == null) {
			throw new IllegalArgumentException("ChatClient is required");
		}

		// 初始化retryTemplate
		this.retryTemplate = new RetryTemplate();
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(this.maxIterations);
		this.retryTemplate.setRetryPolicy(retryPolicy);

		// 初始化toolCallbacks
		this.toolCallbacks = Arrays.stream(toolCallbacks).map(toolCallback -> {
			// ToolCalling策略调用完工具后直接返回，需要包装一层使得returnDirect为true
			if (this.strategy == Strategy.TOOL_CALLING && !toolCallback.getToolMetadata().returnDirect()) {
				final ToolMetadata toolMetadata = ToolMetadata.builder().returnDirect(true).build();
				return new ToolCallback() {
					@Override
					public ToolDefinition getToolDefinition() {
						return toolCallback.getToolDefinition();
					}

					@Override
					public ToolMetadata getToolMetadata() {
						// returnDirect为true的ToolMetadata
						return toolMetadata;
					}

					@Override
					public String call(String toolInput) {
						return toolCallback.call(toolInput);
					}

					@Override
					public String call(String toolInput, @Nullable ToolContext tooContext) {
						return toolCallback.call(toolInput, tooContext);
					}

				};
			}
			else {
				return toolCallback;
			}
		}).toArray(ToolCallback[]::new);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String userPrompt = new PromptTemplate(this.userPrompt).render(state.data());
		String systemPrompt = new PromptTemplate(this.systemPrompt).render(state.data());
		String output = switch (this.strategy) {
			case TOOL_CALLING, REACT -> {
				// 重试机制
				try {
					yield this.retryTemplate.execute(retryContext -> {
						String content = this.chatClient.prompt(systemPrompt)
							.toolCallbacks(this.toolCallbacks)
							.user(userPrompt)
							.call()
							.content();
						if (content == null) {
							logger.warn("ChatClient Call Return Null...");
							throw new RuntimeException("ChatClient Call Return Null...");
						}
						return content;
					});
				}
				catch (Exception e) {
					logger.error("Attempted to the maximum number of times but still failed!");
					yield null;
				}
			}
		};
		return Map.of(this.outputKey, output == null ? "" : output);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private ChatClient chatClient;

		private ToolCallback[] toolCallbacks;

		private Strategy strategy;

		private String systemPrompt;

		private String userPrompt;

		private Integer maxIterations;

		private String outputKey;

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder toolCallbacks(ToolCallback[] toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder toolCallBacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks.toArray(ToolCallback[]::new);
			return this;
		}

		public Builder strategy(Strategy strategy) {
			this.strategy = strategy;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder userPrompt(String userPrompt) {
			this.userPrompt = userPrompt;
			return this;
		}

		public Builder maxIterations(Integer maxIterations) {
			this.maxIterations = maxIterations;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public AgentNode build() {
			return new AgentNode(chatClient, toolCallbacks, strategy, systemPrompt, userPrompt, maxIterations,
					outputKey);
		}

	}

}
