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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenience builder for configuring both TaskTool and TaskOutputTool with a shared TaskRepository.
 * <p>
 * Supports two modes:
 * <ol>
 * <li><strong>Programmatic</strong>: Pass {@link ReactAgent} instances via {@link #subAgents(Map)}
 * or {@link #subAgent(String, ReactAgent)}.</li>
 * <li><strong>File-based</strong>: Load agent specs from Markdown files via {@link #addAgentDirectory(String)}
 * or {@link #addAgentResource(Resource)}. Specs are converted to ReactAgents using
 * {@link AgentSpecReactAgentFactory}.</li>
 * </ol>
 *
 * <pre>{@code
 * // Programmatic mode
 * List<ToolCallback> taskTools = TaskToolsBuilder.builder()
 *     .taskRepository(repo)
 *     .subAgents(Map.of("Explore", exploreAgent))
 *     .build();
 *
 * // File-based mode
 * List<ToolCallback> taskTools = TaskToolsBuilder.builder()
 *     .taskRepository(repo)
 *     .chatModel(chatModel)
 *     .defaultTools(grepTool, globTool, readTool)
 *     .addAgentDirectory(".claude/agents")
 *     .build();
 * }</pre>
 *
 * @author Spring AI Alibaba
 */
public final class TaskToolsBuilder {

	private TaskRepository taskRepository = new DefaultTaskRepository();

	private Map<String, ReactAgent> subAgents;

	private final List<String> agentDirectories = new ArrayList<>();

	private final List<Resource> agentResources = new ArrayList<>();

	private AgentSpecReactAgentFactory agentSpecFactory;

	private ChatModel chatModel;

	private ChatClient chatClient;

	private final List<ToolCallback> defaultTools = new ArrayList<>();

	private TaskToolsBuilder() {
	}

	/**
	 * Create a new builder.
	 */
	public static TaskToolsBuilder builder() {
		return new TaskToolsBuilder();
	}

	/**
	 * Set the task repository (required for background execution).
	 */
	public TaskToolsBuilder taskRepository(TaskRepository taskRepository) {
		Assert.notNull(taskRepository, "taskRepository must not be null");
		this.taskRepository = taskRepository;
		return this;
	}

	/**
	 * Set the map of sub-agent types to ReactAgent instances (programmatic mode).
	 */
	public TaskToolsBuilder subAgents(Map<String, ReactAgent> subAgents) {
		Assert.notEmpty(subAgents, "subAgents must not be empty");
		this.subAgents = subAgents;
		return this;
	}

	/**
	 * Add a single sub-agent (programmatic mode).
	 */
	public TaskToolsBuilder subAgent(String type, ReactAgent agent) {
		Assert.hasText(type, "type must not be empty");
		Assert.notNull(agent, "agent must not be null");
		if (this.subAgents == null) {
			this.subAgents = new HashMap<>();
		}
		this.subAgents.put(type, agent);
		return this;
	}

	/**
	 * Add a directory to load agent specs from (file-based mode).
	 * Scans for .md files recursively. Requires {@link #chatModel(ChatModel)} or
	 * {@link #chatClient(ChatClient)} and {@link #defaultTools(ToolCallback...)}.
	 */
	public TaskToolsBuilder addAgentDirectory(String directoryPath) {
		if (StringUtils.hasText(directoryPath)) {
			this.agentDirectories.add(directoryPath);
		}
		return this;
	}

	/**
	 * Add a Spring Resource to load agent specs from (file-based mode).
	 * For classpath resources, use {@code resourceLoader.getResource("classpath:.claude/agents")}.
	 */
	public TaskToolsBuilder addAgentResource(Resource resource) {
		if (resource != null) {
			this.agentResources.add(resource);
		}
		return this;
	}

	/**
	 * Set the factory for converting specs to ReactAgents (file-based mode).
	 * If not set, one is built from chatModel/chatClient and defaultTools in build().
	 */
	public TaskToolsBuilder agentSpecFactory(AgentSpecReactAgentFactory factory) {
		this.agentSpecFactory = factory;
		return this;
	}

	/**
	 * Set ChatModel for file-based mode (used to build ReactAgents from specs).
	 */
	public TaskToolsBuilder chatModel(ChatModel chatModel) {
		this.chatModel = chatModel;
		return this;
	}

	/**
	 * Set ChatClient for file-based mode (used to build ReactAgents from specs).
	 */
	public TaskToolsBuilder chatClient(ChatClient chatClient) {
		this.chatClient = chatClient;
		return this;
	}

	/**
	 * Set default tools for sub-agents (file-based mode).
	 */
	public TaskToolsBuilder defaultTools(ToolCallback... tools) {
		if (tools != null) {
			this.defaultTools.addAll(List.of(tools));
		}
		return this;
	}

	/**
	 * Build both TaskTool and TaskOutputTool as a list of ToolCallbacks.
	 */
	public List<ToolCallback> build() {
		Assert.notNull(this.taskRepository, "taskRepository must be provided");

		Map<String, ReactAgent> resolved = resolveSubAgents();
		Assert.notEmpty(resolved, "At least one sub-agent must be configured (via subAgents or addAgentDirectory)");

		List<ToolCallback> tools = new ArrayList<>();
		tools.add(TaskTool.builder()
				.subAgents(resolved)
				.taskRepository(this.taskRepository)
				.build());
		tools.add(TaskOutputTool.builder()
				.taskRepository(this.taskRepository)
				.build());
		return tools;
	}

	private Map<String, ReactAgent> resolveSubAgents() {
		if (this.subAgents != null && !this.subAgents.isEmpty()) {
			Map<String, ReactAgent> result = new HashMap<>(this.subAgents);
			loadFromFilesAndMerge(result);
			return result;
		}
		Map<String, ReactAgent> fromFiles = loadFromFiles();
		return fromFiles != null ? fromFiles : Map.of();
	}

	private void loadFromFilesAndMerge(Map<String, ReactAgent> into) {
		Map<String, ReactAgent> fromFiles = loadFromFiles();
		if (fromFiles != null) {
			into.putAll(fromFiles);
		}
	}

	private Map<String, ReactAgent> loadFromFiles() {
		if (agentDirectories.isEmpty() && agentResources.isEmpty()) {
			return null;
		}

		AgentSpecReactAgentFactory factory = this.agentSpecFactory;
		if (factory == null) {
			AgentSpecReactAgentFactory.Builder fb = AgentSpecReactAgentFactory.builder();
			if (this.chatModel != null) {
				fb.chatModel(this.chatModel);
			}
			else if (this.chatClient != null) {
				fb.chatClient(this.chatClient);
			}
			else {
				throw new IllegalStateException(
						"chatModel or chatClient must be provided for file-based agent loading");
			}
			if (!this.defaultTools.isEmpty()) {
				fb.defaultTools(this.defaultTools);
			}
			factory = fb.build();
		}

		Map<String, ReactAgent> result = new HashMap<>();
		for (String dir : agentDirectories) {
			try {
				List<AgentSpec> specs = AgentSpecLoader.loadFromDirectory(dir);
				for (AgentSpec spec : specs) {
					result.put(spec.name(), factory.create(spec));
				}
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to load agent specs from " + dir, e);
			}
		}
		for (Resource resource : agentResources) {
			try {
				AgentSpec spec = AgentSpecLoader.loadFromResource(resource);
				if (spec != null) {
					result.put(spec.name(), factory.create(spec));
				}
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to load agent spec from " + resource, e);
			}
		}
		return result.isEmpty() ? null : result;
	}
}
