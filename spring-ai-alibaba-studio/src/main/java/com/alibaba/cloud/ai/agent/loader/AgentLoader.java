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
package com.alibaba.cloud.ai.agent.loader;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;

import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Interface for loading agents.
 *
 * <p>Users implement this interface to register their agents.
 *
 * <p><strong>Thread Safety:</strong> Implementation must be thread-safe as it will be used as
 * Spring singleton beans and accessed concurrently by multiple HTTP requests.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public class MyAgentLoader implements AgentLoader {
 *   @Override
 *   public ImmutableList<String> listAgents() {
 *     return ImmutableList.of("chat_bot", "code_assistant");
 *   }
 *
 *   @Override
 *   public BaseAgent loadAgent(String name) {
 *     switch (name) {
 *       case "chat_bot": return createChatBot();
 *       case "code_assistant": return createCodeAssistant();
 *       default: throw new java.util.NoSuchElementException("Agent not found: " + name);
 *     }
 *   }
 * }
 * }</pre>
 *
 */
public interface AgentLoader {

	/**
	 * Returns a list of available agent names.
	 *
	 * @return ImmutableList of agent names. Must not return null - return an empty list if no agents
	 *     are available.
	 */
	@Nonnull
	List<String> listAgents();

	/**
	 * Loads the BaseAgent instance for the specified agent name.
	 *
	 * @param name the name of the agent to load
	 * @return BaseAgent instance for the given name
	 * @throws java.util.NoSuchElementException if the agent doesn't exist
	 * @throws IllegalStateException if the agent exists but fails to load
	 */
	BaseAgent loadAgent(String name);
}
