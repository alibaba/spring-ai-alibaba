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
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;

/**
 * Dedicated node for merging routing results. Collects outputs from sub-agents routed by
 * LlmRoutingAgent, synthesizes them via LLM into a single coherent answer, and writes the
 * merged result to state.
 */
public class RoutingMergeNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RoutingMergeNode.class);

	/** Default key for the merged result in state. */
	public static final String DEFAULT_MERGED_OUTPUT_KEY = "merged_result";

	private static final String DEFAULT_MESSAGES_OUTPUT_KEY = "messages";

	private static final String SYNTHESIZE_SYSTEM_TEMPLATE = """
			Synthesize these search results to answer the original question: "%s"

			- Combine information from multiple sources without redundancy
			- Highlight the most relevant and actionable information
			- Note any discrepancies between sources
			- Keep the response concise and well-organized
			""";

	private final ChatModel chatModel;
	private final List<Agent> subAgents;
	private final String mergedOutputKey;

	public RoutingMergeNode(ChatModel chatModel, List<? extends Agent> subAgents) {
		this(chatModel, subAgents, DEFAULT_MERGED_OUTPUT_KEY);
	}

	public RoutingMergeNode(ChatModel chatModel, List<? extends Agent> subAgents, String mergedOutputKey) {
		this.chatModel = chatModel;
		this.subAgents = List.copyOf(subAgents);
		this.mergedOutputKey = mergedOutputKey != null ? mergedOutputKey : DEFAULT_MERGED_OUTPUT_KEY;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("RoutingMergeNode: merging results from {} sub-agents", subAgents.size());

		List<String> formattedResults = new ArrayList<>();
		Set<String> collectedOutputKeys = new HashSet<>();
		String lastResult = null;
		Optional<Set<String>> routedAgentNames = currentRoutedAgentNames(state);
		long routedAgentCount = countRoutedAgents(state, routedAgentNames);
		boolean hasRoutingMarkers = routedAgentNames.isPresent() || routedAgentCount > 0;
		long routingMergedOutputOwnerCount = countRoutingMergedOutputOwners(state, hasRoutingMarkers, routedAgentNames);
		for (Agent subAgent : subAgents) {
			boolean topLevelRoutedAgent = isTopLevelRoutedAgent(state, subAgent, routedAgentNames);
			if (hasRoutingMarkers && !topLevelRoutedAgent) {
				logger.debug("Skipping unrouted sub-agent {}", subAgent.name());
				continue;
			}
			boolean allowDefaultMessagesOutput = routedAgentCount == 1 && topLevelRoutedAgent;
			boolean allowRoutingMergedOutput = routingMergedOutputOwnerCount == 1 && usesRoutingMergedOutput(subAgent);
			boolean preferWrapperOutput = shouldPreferWrapperOutput(state, subAgent, routedAgentCount, topLevelRoutedAgent,
					routingMergedOutputOwnerCount, routedAgentNames);
			List<String> outputKeys = resolveOutputKeys(subAgent, allowDefaultMessagesOutput,
					allowRoutingMergedOutput, preferWrapperOutput);
			List<String> sourceTexts = new ArrayList<>();
			boolean collectMultipleOutputs = collectsMultipleOutputs(subAgent);
			for (String outputKey : outputKeys) {
				if (collectedOutputKeys.contains(outputKey)) {
					continue;
				}
				boolean wrapperOutputKey = outputKeyToParent(subAgent.name()).equals(outputKey);
				if (collectMultipleOutputs && !sourceTexts.isEmpty()
						&& wrapperOutputKey) {
					continue;
				}
				Optional<Object> outputOpt = state.value(outputKey);
				if (outputOpt.isPresent()) {
					String text = extractText(outputOpt.get(), outputKey);
					if (text != null && !text.isBlank()) {
						collectedOutputKeys.add(outputKey);
						sourceTexts.add(text);
						logger.debug("Collected result from {} (key: {})", subAgent.name(), outputKey);
						if (!collectMultipleOutputs || (preferWrapperOutput && wrapperOutputKey)) {
							break;
						}
					}
				}
			}
			if (!sourceTexts.isEmpty()) {
				String text = String.join("\n\n", sourceTexts);
				String source = capitalize(subAgent.name());
				formattedResults.add("**From " + source + ":**\n" + text);
				lastResult = text;
			}
		}

		// When the router delegated to a single sub-agent, that agent's answer is already the
		// final response. Re-synthesizing it through the LLM would issue a redundant model call
		// and emit a second, rephrased copy of the same answer to the user (gh-4616). Pass the
		// single result through unchanged; only genuinely multi-source results need synthesis.
		if (formattedResults.size() == 1) {
			logger.debug("RoutingMergeNode: single routed result, returning it without re-synthesis");
			return Map.of(mergedOutputKey, lastResult);
		}
		if (formattedResults.isEmpty()) {
			logger.debug("RoutingMergeNode: no routed results found");
			return Map.of(mergedOutputKey, "No results found from any knowledge source.");
		}

		String query = extractOriginalQuery(state);
		String finalAnswer = synthesize(query, formattedResults);
		logger.debug("RoutingMergeNode: synthesized {} sources into merged result", formattedResults.size());

		return Map.of(mergedOutputKey, finalAnswer);
	}

	private List<String> resolveOutputKeys(Agent agent, boolean allowDefaultMessagesOutput,
			boolean allowRoutingMergedOutput, boolean preferWrapperOutput) {
		List<String> outputKeys = new ArrayList<>();
		String wrapperOutputKey = outputKeyToParent(agent.name());
		if (preferWrapperOutput) {
			outputKeys.add(wrapperOutputKey);
		}
		if (agent instanceof BaseAgent baseAgent) {
			String outputKey = baseAgent.getOutputKey();
			if (outputKey != null) {
				outputKeys.add(outputKey);
			}
			else if (allowDefaultMessagesOutput) {
				outputKeys.add(DEFAULT_MESSAGES_OUTPUT_KEY);
			}
		}
		else if (agent instanceof ParallelAgent parallelAgent) {
			if (parallelAgent.mergeOutputKey() != null) {
				outputKeys.add(parallelAgent.mergeOutputKey());
			}
			else {
				List<Agent> nestedAgents = parallelAgent.subAgents();
				if (nestedAgents != null) {
					for (Agent nestedAgent : nestedAgents) {
						outputKeys.addAll(resolveOutputKeys(nestedAgent, false, allowRoutingMergedOutput, false));
					}
				}
			}
		}
		else if (agent instanceof LlmRoutingAgent) {
			if (allowRoutingMergedOutput) {
				outputKeys.add(DEFAULT_MERGED_OUTPUT_KEY);
			}
		}
		else if (agent instanceof SequentialAgent sequentialAgent) {
			List<Agent> nestedAgents = sequentialAgent.subAgents();
			if (nestedAgents != null && !nestedAgents.isEmpty()) {
				outputKeys.addAll(resolveOutputKeys(nestedAgents.get(nestedAgents.size() - 1),
						allowDefaultMessagesOutput, allowRoutingMergedOutput, false));
			}
		}
		else if (agent instanceof FlowAgent flowAgent) {
			List<Agent> nestedAgents = flowAgent.subAgents();
			if (nestedAgents != null) {
				for (Agent nestedAgent : nestedAgents) {
					outputKeys.addAll(resolveOutputKeys(nestedAgent, allowDefaultMessagesOutput,
							allowRoutingMergedOutput, false));
				}
			}
		}
		if (!preferWrapperOutput) {
			outputKeys.add(wrapperOutputKey);
		}
		return outputKeys;
	}

	private boolean shouldPreferWrapperOutput(OverAllState state, Agent agent, long routedAgentCount,
			boolean topLevelRoutedAgent, long routingMergedOutputOwnerCount, Optional<Set<String>> routedAgentNames) {
		if (routedAgentCount <= 1 || !topLevelRoutedAgent || !(agent instanceof FlowAgent)
				|| state.value(outputKeyToParent(agent.name())).isEmpty()) {
			return false;
		}

		Set<String> outputKeys = resolvedNonWrapperOutputKeys(agent, routingMergedOutputOwnerCount);
		if (outputKeys.isEmpty()) {
			return true;
		}

		return subAgents.stream()
			.filter(other -> other != agent)
			.filter(other -> isTopLevelRoutedAgent(state, other, routedAgentNames))
			.map(other -> resolvedNonWrapperOutputKeys(other, routingMergedOutputOwnerCount))
			.anyMatch(otherOutputKeys -> otherOutputKeys.stream().anyMatch(outputKeys::contains));
	}

	private Set<String> resolvedNonWrapperOutputKeys(Agent agent, long routingMergedOutputOwnerCount) {
		boolean allowRoutingMergedOutput = routingMergedOutputOwnerCount == 1 && usesRoutingMergedOutput(agent);
		String wrapperOutputKey = outputKeyToParent(agent.name());
		Set<String> outputKeys = new HashSet<>();
		for (String outputKey : resolveOutputKeys(agent, false, allowRoutingMergedOutput, false)) {
			if (!wrapperOutputKey.equals(outputKey)) {
				outputKeys.add(outputKey);
			}
		}
		return outputKeys;
	}

	private long countRoutedAgents(OverAllState state, Optional<Set<String>> routedAgentNames) {
		if (routedAgentNames.isPresent()) {
			Set<String> selectedAgents = routedAgentNames.get();
			return subAgents.stream().filter(agent -> selectedAgents.contains(agent.name())).count();
		}
		return subAgents.stream().filter(agent -> isTopLevelRoutedAgent(state, agent, routedAgentNames)).count();
	}

	private long countRoutingMergedOutputOwners(OverAllState state, boolean hasRoutingMarkers,
			Optional<Set<String>> routedAgentNames) {
		return subAgents.stream()
			.filter(agent -> !hasRoutingMarkers || isTopLevelRoutedAgent(state, agent, routedAgentNames))
			.filter(this::usesRoutingMergedOutput)
			.count();
	}

	private boolean isTopLevelRoutedAgent(OverAllState state, Agent agent, Optional<Set<String>> routedAgentNames) {
		if (routedAgentNames.isPresent()) {
			return routedAgentNames.get().contains(agent.name());
		}
		return state.value(agent.name() + "_input").isPresent();
	}

	private Optional<Set<String>> currentRoutedAgentNames(OverAllState state) {
		Optional<Object> value = state.value(RoutingNode.ROUTED_AGENT_NAMES_KEY);
		if (value.isEmpty()) {
			return Optional.empty();
		}

		Set<String> agentNames = new LinkedHashSet<>();
		Object routedAgents = value.get();
		if (routedAgents instanceof Iterable<?> iterable) {
			for (Object agentName : iterable) {
				if (agentName instanceof String name && !name.isBlank()) {
					agentNames.add(name);
				}
			}
		}
		else if (routedAgents instanceof String name && !name.isBlank()) {
			agentNames.add(name);
		}
		return Optional.of(agentNames);
	}

	private boolean usesRoutingMergedOutput(Agent agent) {
		if (agent instanceof LlmRoutingAgent) {
			return true;
		}
		if (agent instanceof ParallelAgent parallelAgent && parallelAgent.mergeOutputKey() != null) {
			return false;
		}
		if (agent instanceof SequentialAgent sequentialAgent) {
			List<Agent> nestedAgents = sequentialAgent.subAgents();
			return nestedAgents != null && !nestedAgents.isEmpty()
					&& usesRoutingMergedOutput(nestedAgents.get(nestedAgents.size() - 1));
		}
		if (agent instanceof FlowAgent flowAgent) {
			List<Agent> nestedAgents = flowAgent.subAgents();
			return nestedAgents != null && nestedAgents.stream().anyMatch(this::usesRoutingMergedOutput);
		}
		return false;
	}

	private boolean collectsMultipleOutputs(Agent agent) {
		if (agent instanceof ParallelAgent parallelAgent) {
			return parallelAgent.mergeOutputKey() == null;
		}
		if (agent instanceof SequentialAgent sequentialAgent) {
			List<Agent> nestedAgents = sequentialAgent.subAgents();
			return nestedAgents != null && !nestedAgents.isEmpty()
					&& collectsMultipleOutputs(nestedAgents.get(nestedAgents.size() - 1));
		}
		if (agent instanceof LlmRoutingAgent) {
			return false;
		}
		if (agent instanceof FlowAgent flowAgent) {
			List<Agent> nestedAgents = flowAgent.subAgents();
			return nestedAgents != null && nestedAgents.size() == 1 && collectsMultipleOutputs(nestedAgents.get(0));
		}
		return false;
	}

	private String extractOriginalQuery(OverAllState state) {
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		if (messages.isEmpty()) {
			return "";
		}
		Message last = messages.get(messages.size() - 1);
		return last.getText() != null ? last.getText() : "";
	}

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

	private static String extractListText(List<?> list, String outputKey) {
		if (DEFAULT_MESSAGES_OUTPUT_KEY.equals(outputKey)) {
			return extractLastAssistantMessageText(list);
		}
		return list.toString();
	}

	private static String extractLastAssistantMessageText(List<?> list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			Object value = list.get(i);
			if (value instanceof AssistantMessage message) {
				return message.getText();
			}
		}
		return "";
	}

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

	private static boolean isSubGraphWrapperKey(String outputKey) {
		return outputKey != null && outputKey.startsWith("subgraph_") && outputKey.endsWith("_compiled_graph");
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
