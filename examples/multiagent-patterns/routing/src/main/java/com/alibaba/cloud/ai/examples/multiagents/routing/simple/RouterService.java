/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.routing.simple;

import com.alibaba.cloud.ai.examples.multiagents.routing.simple.state.AgentOutput;
import com.alibaba.cloud.ai.examples.multiagents.routing.simple.state.Classification;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the router workflow using LlmRoutingAgent: classify + parallel specialist agents,
 * then synthesizes results into a single answer.
 */
public class RouterService {

	private static final Logger log = LoggerFactory.getLogger(RouterService.class);

	private static final String[] OUTPUT_KEYS = { "github_key", "notion_key", "slack_key" };

	private static final String SYNTHESIZE_SYSTEM_TEMPLATE = """
			Synthesize these search results to answer the original question: "%s"

			- Combine information from multiple sources without redundancy
			- Highlight the most relevant and actionable information
			- Note any discrepancies between sources
			- Keep the response concise and well-organized
			""";

	private final ChatModel chatModel;
	private final LlmRoutingAgent routerAgent;

	public RouterService(ChatModel chatModel, LlmRoutingAgent routerAgent) {
		this.chatModel = chatModel;
		this.routerAgent = routerAgent;
	}

	/**
	 * Run the full router pipeline: classify + parallel agents (via LlmRoutingAgent) → synthesize.
	 */
	public RouterResult run(String query) throws GraphRunnerException {
		Optional<OverAllState> resultOpt = routerAgent.invoke(query);
		if (resultOpt.isEmpty()) {
			return new RouterResult(query, List.of(), List.of(), "No result from router.");
		}

		OverAllState state = resultOpt.get();
		String finalAnswer;

		List<Classification> classifications = collectClassifications(state);
		List<AgentOutput> results = collectAgentOutputs(state);
		log.debug("Routed to {} sources: {}", classifications.size(), classifications);

		Optional<Object> mergedOpt = state.value(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY);
		if (mergedOpt.isPresent()) {
			finalAnswer = extractText(mergedOpt.get());
		} else {
			finalAnswer = synthesize(query, results);
		}
		return new RouterResult(query, classifications, results, finalAnswer);
	}

	private List<Classification> collectClassifications(OverAllState state) {
		List<Classification> list = new ArrayList<>();
		for (String outputKey : OUTPUT_KEYS) {
			Optional<Object> outputOpt = state.value(outputKey);
			if (outputOpt.isPresent()) {
				String agentName = outputKey.replace("_key", "");
				String query = state.value(agentName + "_input")
						.map(Object::toString)
						.orElse("");
				list.add(new Classification(agentName, query));
			}
		}
		return list;
	}

	private List<AgentOutput> collectAgentOutputs(OverAllState state) {
		List<AgentOutput> list = new ArrayList<>();
		for (String outputKey : OUTPUT_KEYS) {
			Optional<Object> outputOpt = state.value(outputKey);
			if (outputOpt.isPresent()) {
				String agentName = outputKey.replace("_key", "");
				String result = RoutingMergeNode.extractText(outputOpt.get(), outputKey);
				list.add(new AgentOutput(agentName, result));
			}
		}
		return list;
	}

	private static String extractText(Object output) {
		if (output instanceof Message message) {
			return message.getText();
		}
		return output != null ? output.toString() : "";
	}

	/**
	 * Synthesize collected results into a single coherent answer.
	 */
	public String synthesize(String query, List<AgentOutput> results) {
		if (results == null || results.isEmpty()) {
			return "No results found from any knowledge source.";
		}
		String formatted = results.stream()
				.map(r -> "**From " + capitalize(r.source()) + ":**\n" + r.result())
				.reduce((a, b) -> a + "\n\n" + b)
				.orElse("");
		String systemPrompt = SYNTHESIZE_SYSTEM_TEMPLATE.formatted(query);
		Prompt prompt = new Prompt(List.of(
				new SystemMessage(systemPrompt),
				new UserMessage(formatted)));
		ChatResponse response = chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * Result of a full router run: original query, classifications, agent outputs, and final answer.
	 */
	public record RouterResult(String query, List<Classification> classifications,
			List<AgentOutput> results, String finalAnswer) {
	}
}
