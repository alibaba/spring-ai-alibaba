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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dedicated node for merging routing results. Collects outputs from sub-agents routed by
 * LlmRoutingAgent, synthesizes them via LLM into a single coherent answer, and writes the
 * merged result to state.
 */
public class RoutingMergeNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RoutingMergeNode.class);

	/** Default key for the merged result in state. */
	public static final String DEFAULT_MERGED_OUTPUT_KEY = "merged_result";

	static final String DEFAULT_MESSAGES_OUTPUT_KEY = "messages";

	private static final String SYNTHESIZE_SYSTEM_TEMPLATE = """
			Synthesize these search results to answer the original question: "%s"

			- Combine information from multiple sources without redundancy
			- Highlight the most relevant and actionable information
			- Note any discrepancies between sources
			- Keep the response concise and well-organized
			""";

	private final ChatModel chatModel;

	private final RoutingOutputResolver outputResolver;

	private final String mergedOutputKey;

	public RoutingMergeNode(ChatModel chatModel, List<? extends Agent> subAgents) {
		this(chatModel, null, subAgents, DEFAULT_MERGED_OUTPUT_KEY);
	}

	public RoutingMergeNode(ChatModel chatModel, Agent routingAgent, List<? extends Agent> subAgents) {
		this(chatModel, routingAgent, subAgents, DEFAULT_MERGED_OUTPUT_KEY);
	}

	public RoutingMergeNode(ChatModel chatModel, List<? extends Agent> subAgents, String mergedOutputKey) {
		this(chatModel, null, subAgents, mergedOutputKey);
	}

	public RoutingMergeNode(ChatModel chatModel, Agent routingAgent, List<? extends Agent> subAgents,
			String mergedOutputKey) {
		this.chatModel = chatModel;
		String routingAgentName = routingAgent != null ? routingAgent.name() : null;
		this.outputResolver = new RoutingOutputResolver(routingAgentName, subAgents);
		this.mergedOutputKey = mergedOutputKey != null ? mergedOutputKey : DEFAULT_MERGED_OUTPUT_KEY;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("RoutingMergeNode: merging results from {} sub-agents", outputResolver.subAgentCount());

		RoutingOutputResolver.MergeContext context = outputResolver.createContext(state);
		List<String> formattedResults = new ArrayList<>();
		Set<String> collectedOutputKeys = new HashSet<>();
		Set<String> staleWrapperOutputKeys = new HashSet<>();
		String lastResult = null;

		for (Agent subAgent : outputResolver.subAgents()) {
			Optional<RoutingOutputResolver.CollectedAgentResult> collectedResult = outputResolver.collectAgentResult(
					state, subAgent, context, collectedOutputKeys);
			if (collectedResult.isPresent()) {
				RoutingOutputResolver.CollectedAgentResult result = collectedResult.get();
				formattedResults.add("**From " + capitalize(subAgent.name()) + ":**\n" + result.text());
				lastResult = result.text();
				outputResolver.markStaleWrapperIfNeeded(state, subAgent, result, staleWrapperOutputKeys);
			}
		}

		// When the router delegated to a single sub-agent, that agent's answer is already the
		// final response. Re-synthesizing it through the LLM would issue a redundant model call
		// and emit a second, rephrased copy of the same answer to the user (gh-4616). Pass the
		// single result through unchanged; only genuinely multi-source results need synthesis.
		if (formattedResults.size() == 1) {
			logger.debug("RoutingMergeNode: single routed result, returning it without re-synthesis");
			return mergeResult(lastResult, staleWrapperOutputKeys, context.exposeMergedResultToParent());
		}
		if (formattedResults.isEmpty()) {
			logger.debug("RoutingMergeNode: no routed results found");
			return mergeResult("No results found from any knowledge source.", staleWrapperOutputKeys,
					context.exposeMergedResultToParent());
		}

		String query = extractOriginalQuery(state);
		String finalAnswer = synthesize(query, formattedResults);
		logger.debug("RoutingMergeNode: synthesized {} sources into merged result", formattedResults.size());

		return mergeResult(finalAnswer, staleWrapperOutputKeys, context.exposeMergedResultToParent());
	}

	/**
	 * Builds the state update returned by the merge node.
	 * <p>
	 * Nested routing graphs also expose their merged answer through the subgraph wrapper
	 * key so a parent router can collect the child's synthesized answer instead of a raw
	 * inner-agent output.
	 * @param result the final text produced by the merge node
	 * @param staleWrapperOutputKeys wrapper keys that should be removed from state
	 * @param exposeMergedResultToParent whether to also write the result to this router's
	 * wrapper key
	 * @return the graph state update emitted by this node
	 */
	private Map<String, Object> mergeResult(String result, Set<String> staleWrapperOutputKeys,
			boolean exposeMergedResultToParent) {
		Map<String, Object> output = new HashMap<>();
		output.put(mergedOutputKey, result);
		if (exposeMergedResultToParent) {
			String wrapperOutputKey = outputResolver.wrapperOutputKey();
			logger.debug("RoutingMergeNode: exposing merged result through wrapper key {}", wrapperOutputKey);
			output.put(wrapperOutputKey, result);
		}
		for (String outputKey : staleWrapperOutputKeys) {
			logger.debug("RoutingMergeNode: marking stale wrapper output {} for removal", outputKey);
			output.put(outputKey, OverAllState.MARK_FOR_REMOVAL);
		}
		return output;
	}

	/**
	 * Uses the latest message as the synthesis question for multi-source routing results.
	 * @param state graph state that contains the shared messages list
	 * @return the text of the latest message, or an empty string when unavailable
	 */
	private String extractOriginalQuery(OverAllState state) {
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		if (messages.isEmpty()) {
			return "";
		}
		Message last = messages.get(messages.size() - 1);
		return last.getText() != null ? last.getText() : "";
	}

	/**
	 * Asks the configured chat model to combine multiple routed results into one answer.
	 * @param query original user query used as synthesis context
	 * @param formattedResults formatted routed results grouped by source agent
	 * @return synthesized answer text
	 */
	private String synthesize(String query, List<String> formattedResults) {
		String formatted = String.join("\n\n", formattedResults);
		String systemPrompt = SYNTHESIZE_SYSTEM_TEMPLATE.formatted(query);
		Prompt prompt = new Prompt(List.of(
				new SystemMessage(systemPrompt),
				new UserMessage(formatted)));
		ChatResponse response = chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}

	public static String extractText(Object output, String outputKey) {
		if (output instanceof Message message) {
			return message.getText();
		}
		if (output instanceof List<?> list) {
			return extractListText(list, outputKey);
		}
		if (output instanceof GraphResponse<?> gr) {
			Optional<?> val = gr.resultValue();
			if (val.isPresent()) {
				Object v = val.get();
				if (v instanceof Map<?, ?> map) {
					v = extractMapValue(map, outputKey);
				}
				if (v instanceof Message m) {
					return m.getText();
				}
				if (v instanceof List<?> list) {
					return extractListText(list, outputKey);
				}
				return v != null ? v.toString() : "";
			}
			return "";
		}
		if (output instanceof Map<?, ?> map) {
			Object v = extractMapValue(map, outputKey);
			if (v instanceof Message m) {
				return m.getText();
			}
			if (v instanceof List<?> list) {
				return extractListText(list, outputKey);
			}
			return v != null ? v.toString() : "";
		}
		return output != null ? output.toString() : "";
	}

	/**
	 * Converts list outputs into text, treating the shared messages key specially.
	 * @param list output list from graph state
	 * @param outputKey candidate output key being extracted
	 * @return extracted text for the list output
	 */
	private static String extractListText(List<?> list, String outputKey) {
		if (DEFAULT_MESSAGES_OUTPUT_KEY.equals(outputKey)) {
			return extractLastAssistantMessageText(list);
		}
		return list.toString();
	}

	/**
	 * Reads the last assistant answer from the shared messages list.
	 * @param list message history stored under the shared messages key
	 * @return the latest assistant message text, or an empty string if none exists
	 */
	private static String extractLastAssistantMessageText(List<?> list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			Object value = list.get(i);
			if (value instanceof AssistantMessage message) {
				return message.getText();
			}
		}
		return "";
	}

	/**
	 * Extracts the value that corresponds to a candidate output key from graph state maps.
	 * @param map graph state map or subgraph wrapper map
	 * @param outputKey candidate output key being extracted
	 * @return the selected value, the only map value, or the original map as fallback
	 */
	private static Object extractMapValue(Map<?, ?> map, String outputKey) {
		// A subgraph wrapper key namespaces a nested FlowAgent result in the parent graph.
		// When the nested graph is itself a routing graph, its final answer is stored under
		// the routing merge key inside that wrapper state.
		if (isSubGraphWrapperKey(outputKey) && map.containsKey(DEFAULT_MERGED_OUTPUT_KEY)) {
			return map.get(DEFAULT_MERGED_OUTPUT_KEY);
		}
		if (map.containsKey(outputKey)) {
			return map.get(outputKey);
		}
		if (map.size() == 1) {
			return map.values().iterator().next();
		}
		return map;
	}

	/**
	 * Detects the parent-state wrapper key used for embedded subgraph results.
	 * @param outputKey candidate output key
	 * @return true if the key is a subgraph wrapper key
	 */
	private static boolean isSubGraphWrapperKey(String outputKey) {
		return outputKey != null && outputKey.startsWith("subgraph_") && outputKey.endsWith("_compiled_graph");
	}

	/**
	 * Formats source labels used in synthesized multi-agent results.
	 * @param s source label to format
	 * @return capitalized source label
	 */
	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
