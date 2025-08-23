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
package com.alibaba.cloud.ai.graph.agent.flow;

import java.util.HashMap;
import java.util.List;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

/**
 * Example demonstrating the improved Builder pattern for FlowAgent classes. This class
 * shows how the refactored builders provide a consistent, type-safe, and fluent
 * interface.
 */
public class BuilderExample {

	/**
	 * Demonstrates the usage of the refactored SequentialAgent.Builder
	 */
	public static SequentialAgent createSequentialAgentExample(ChatClient chatClient, ToolCallbackResolver resolver)
			throws Exception {

		// Create a sub-agent to use in the flow
		ReactAgent subAgent = ReactAgent.builder()
			.name("dataProcessor")
			.description("Processes data sequentially")
			.outputKey("processed_data")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(resolver)
			.build();

		// Create SequentialAgent using the improved builder
		return SequentialAgent.builder()
			.name("sequentialWorkflow")
			.description("A workflow that processes data in sequence")
			.outputKey("final_result")
			.inputKey("initial_data")
			.subAgents(List.of(subAgent)) // Type-safe - no wildcards!
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("workflow_state", new AppendStrategy());
				return strategies;
			})
			.build();
	}

	/**
	 * Demonstrates the usage of the refactored LlmRoutingAgent.Builder
	 */
	public static LlmRoutingAgent createLlmRoutingAgentExample(ChatClient chatClient, ChatModel chatModel,
			ToolCallbackResolver resolver) throws Exception {

		// Create multiple sub-agents for routing
		ReactAgent analysisAgent = ReactAgent.builder()
			.name("analysisAgent")
			.description("Analyzes data and provides insights")
			.outputKey("analysis_result")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(resolver)
			.build();

		ReactAgent reportAgent = ReactAgent.builder()
			.name("reportAgent")
			.description("Generates reports from data")
			.outputKey("report_result")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("messages", new AppendStrategy());
				return strategies;
			})
			.resolver(resolver)
			.build();

		// Create LlmRoutingAgent using the improved builder
		return LlmRoutingAgent.builder()
			.name("intelligentRouter")
			.description("Routes tasks to appropriate agents based on content")
			.outputKey("routed_result")
			.inputKey("task_description")
			.subAgents(List.of(analysisAgent, reportAgent)) // Type-safe!
			.model(chatModel) // LLM-specific configuration
			.state(() -> {
				HashMap<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("routing_state", new AppendStrategy());
				return strategies;
			})
			.build();
	}

	/**
	 * Benefits of the refactored Builder pattern:
	 *
	 * 1. Code Reuse: Common builder logic is centralized in FlowAgentBuilder 2. Type
	 * Safety: Removed wildcard generics, providing better type checking 3. Consistency:
	 * All FlowAgent builders follow the same pattern 4. Validation: Centralized
	 * validation logic with extensibility for specific agents 5. Fluent Interface:
	 * Self-returning methods enable method chaining 6. Extensibility: Easy to add new
	 * FlowAgent types by extending FlowAgentBuilder
	 */

}
