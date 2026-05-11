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
package com.alibaba.cloud.ai.graph.agent.tools.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.micrometer.observation.ObservationRegistry;

/**
 * Factory that converts {@link AgentSpec} into {@link ReactAgent} instances.
 * <p>
 * Use this when loading agent specs from files - the factory creates ReactAgents
 * with the spec's system prompt and optionally filtered tools.
 *
 * <pre>{@code
 * AgentSpecReactAgentFactory factory = AgentSpecReactAgentFactory.builder()
 *     .chatModel(chatModel)
 *     .defaultTools(grepTool, globTool, readTool)
 *     .build();
 *
 * List<AgentSpec> specs = AgentSpecLoader.loadFromDirectory(".claude/agents");
 * Map<String, ReactAgent> subAgents = new HashMap<>();
 * for (AgentSpec spec : specs) {
 *     subAgents.put(spec.name(), factory.create(spec));
 * }
 * }</pre>
 *
 * @author Spring AI Alibaba
 */
public final class AgentSpecReactAgentFactory {

	private static final Logger logger = LoggerFactory.getLogger(AgentSpecReactAgentFactory.class);

	private final ChatModel chatModel;

	private final ChatClient chatClient;

	private final List<ToolCallback> defaultTools;

	private final ObservationRegistry observationRegistry;

	private AgentSpecReactAgentFactory(Builder builder) {
		this.chatModel = builder.chatModel;
		this.chatClient = builder.chatClient;
		this.defaultTools = builder.defaultTools != null ? List.copyOf(builder.defaultTools) : List.of();
		this.observationRegistry = builder.observationRegistry != null
				? builder.observationRegistry
				: ObservationRegistry.NOOP;
	}

	/**
	 * Create a ReactAgent from this spec.
	 */
	public ReactAgent create(AgentSpec spec) {
		Assert.notNull(spec, "spec must not be null");

		ChatClient client = this.chatClient;
		if (client == null && this.chatModel != null) {
			client = ChatClient.builder(this.chatModel).build();
		}
		Assert.notNull(client, "Either chatModel or chatClient must be provided");

		List<ToolCallback> tools = resolveTools(spec);

		var agentBuilder = ReactAgent.builder()
				.name(spec.name())
				.description(spec.description())
				.systemPrompt(StringUtils.hasText(spec.systemPrompt()) ? spec.systemPrompt() : "")
				.chatClient(client)
				.tools(tools);

		ReactAgent agent = agentBuilder.build();

		if (StringUtils.hasText(spec.model())) {
			logger.debug("Agent spec model override not yet supported: {}", spec.model());
		}

		return agent;
	}

	private List<ToolCallback> resolveTools(AgentSpec spec) {
		if (CollectionUtils.isEmpty(this.defaultTools)) {
			return List.of();
		}
		if (CollectionUtils.isEmpty(spec.toolNames())) {
			return this.defaultTools;
		}
		return this.defaultTools.stream()
				.filter(tc -> spec.toolNames().contains(tc.getToolDefinition().name()))
				.collect(Collectors.toList());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private ChatModel chatModel;

		private ChatClient chatClient;

		private List<ToolCallback> defaultTools = new ArrayList<>();

		private ObservationRegistry observationRegistry;

		public Builder chatModel(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder defaultTools(List<ToolCallback> tools) {
			if (tools != null) {
				this.defaultTools = new ArrayList<>(tools);
			}
			return this;
		}

		public Builder defaultTools(ToolCallback... tools) {
			if (tools != null) {
				this.defaultTools = new ArrayList<>(List.of(tools));
			}
			return this;
		}

		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public AgentSpecReactAgentFactory build() {
			Assert.isTrue(this.chatModel != null || this.chatClient != null,
					"Either chatModel or chatClient must be provided");
			return new AgentSpecReactAgentFactory(this);
		}
	}
}
