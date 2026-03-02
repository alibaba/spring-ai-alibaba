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

import java.util.List;

/**
 * Specification for a sub-agent, parsed from Markdown files with YAML front matter.
 * <p>
 * Compatible with spring-ai-agent-utils agent spec format. The markdown body becomes
 * the system prompt; front matter defines name, description, and optional tool filtering.
 *
 * @author Spring AI Alibaba
 */
public record AgentSpec(
		/**
		 * Unique identifier for the sub-agent (used as subagent_type in Task tool).
		 */
		String name,

		/**
		 * Natural language description of when and how to use this agent.
		 */
		String description,

		/**
		 * System prompt content (markdown body, used as ReactAgent system prompt).
		 */
		String systemPrompt,

		/**
		 * Optional list of tool names this agent can use. Empty means all tools.
		 */
		List<String> toolNames,

		/**
		 * Optional model override (e.g., "sonnet", "opus"). Not yet supported.
		 */
		String model) {

	/**
	 * Create a minimal spec with required fields only.
	 */
	public static AgentSpec of(String name, String description, String systemPrompt) {
		return new AgentSpec(name, description, systemPrompt, List.of(), null);
	}
}
