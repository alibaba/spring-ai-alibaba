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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.util.json.JsonParser;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static java.lang.String.format;

/**
 * NodeAction that delegates to a mainAgent (ReactAgent) by invoking its compiled graph.
 * After the mainAgent runs, checks the last message in state "messages"; if it is an
 * AssistantMessage whose text is a JSON array of sub-agent names, extracts them and sets
 * {@value SupervisorNodeFromState#SUPERVISOR_NEXT_KEY} for downstream routing.
 */
public class MainAgentNodeAction implements NodeActionWithConfig {
	public static final Logger logger = LoggerFactory.getLogger(MainAgentNodeAction.class);

	private final ReactAgent mainAgent;
	private final List<Agent> subAgents;

	public MainAgentNodeAction(ReactAgent mainAgent, List<Agent> subAgents) {
		this.mainAgent = mainAgent;
		this.subAgents = subAgents != null ? subAgents : List.of();
	}

	@SuppressWarnings("unchecked")
	private static List<String> parseJsonArrayOfStrings(String text) {
		if (!StringUtils.hasText(text)) {
			return List.of();
		}
		try {
			Object parsed = JsonParser.fromJson(text, List.class);
			if (parsed == null || !(parsed instanceof List<?> list)) {
				return null;
			}
			List<String> result = new ArrayList<>();
			for (Object e : list) {
				if (e == null) {
					continue;
				}
				String s = String.valueOf(e).trim();
				if ("FINISH".equalsIgnoreCase(s)) {
					result.add(s);
					continue;
				}
				result.add(s);
			}
			return result;
		}
		catch (Exception e) {
			logger.warn("Failed to parse JSON array strings from returned sub agent list of Main Agent output text: {}", text, e);
			return null;
		}
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		RunnableConfig subGraphRunnableConfig = RunnableConfig.builder(config)
				.threadId(config.threadId()
						.map(threadId -> format("%s_%s", threadId, subGraphId(mainAgent.name())))
						.orElseGet(() -> subGraphId(mainAgent.name())))
				.nextNode(null)
				.checkPointId(null)
				.build();
		subGraphRunnableConfig.clearContext();

		logger.info("Invoking mainAgent '{}' compiled graph with threadId: {}",
				mainAgent.name(), subGraphRunnableConfig.threadId());

		CompiledGraph graph = mainAgent.getAndCompileGraph();
		Map<String, Object> mainStateData = graph.invoke(state, subGraphRunnableConfig)
				.map(result -> new HashMap<>(result.data()))
				.orElse(new HashMap<>());

		RoutingExtract result = extractRoutingFromMessages(mainStateData);
		if (result == null) {
			return Map.of();
		}
		Map<String, Object> out = new HashMap<>();
		out.put(SupervisorNodeFromState.SUPERVISOR_NEXT_KEY, result.routingValue());
		if (result.routingMessage() != null && !"FINISH".equals(result.routingValue())) {
			out.put("messages", result.routingMessage());
		}
		return out;
	}

	/**
	 * Checks mainStateData "messages", gets the last message; if it is an AssistantMessage
	 * and its text is a JSON array of strings that are all valid sub-agent names (or
	 * empty/FINISH), returns the value and the original message for downstream.
	 * @return RoutingExtract with list of sub-agent names or ["FINISH"] and the AssistantMessage, or null
	 */
	private RoutingExtract extractRoutingFromMessages(Map<String, Object> mainStateData) {
		Object messagesObj = mainStateData.get("messages");
		if (messagesObj == null || !(messagesObj instanceof List<?> messagesList) || messagesList.isEmpty()) {
			return null;
		}
		Object lastObj = messagesList.get(messagesList.size() - 1);
		if (!(lastObj instanceof AssistantMessage assistantMessage)) {
			return null;
		}
		String text = assistantMessage.getText();
		if (!StringUtils.hasText(text)) {
			logger.info("Empty text in last AssistantMessage, routing to FINISH");
			return new RoutingExtract(new ArrayList<>(List.of("FINISH")), assistantMessage);
		}
		List<String> agentNames = parseJsonArrayOfStrings(text.trim());
		if (agentNames == null) {
			logger.info("Failed to parse sub-agent names from last AssistantMessage text, routing to FINISH");
			return new RoutingExtract(new ArrayList<>(List.of("FINISH")), assistantMessage);
		}
		List<String> validNames = agentNames.stream()
				.filter(name -> subAgents.stream().anyMatch(a -> a.name().equals(name)))
				.toList();
		boolean allValid = validNames.size() == agentNames.size() && agentNames.stream()
				.noneMatch("FINISH"::equalsIgnoreCase);
		if (allValid && !validNames.isEmpty()) {
			logger.info("MainAgentNodeAction: {} from last AssistantMessage = {}", SupervisorNodeFromState.SUPERVISOR_NEXT_KEY, validNames);
			return new RoutingExtract(new ArrayList<>(validNames), assistantMessage);
		}
		boolean emptyOrFinish = agentNames.isEmpty() || agentNames.stream()
				.allMatch(s -> "FINISH".equalsIgnoreCase(s.trim()));
		if (emptyOrFinish) {
			logger.info("MainAgentNodeAction: {} = FINISH from last AssistantMessage", SupervisorNodeFromState.SUPERVISOR_NEXT_KEY);
			return new RoutingExtract(new ArrayList<>(List.of("FINISH")), assistantMessage);
		}

		logger.info("No valid sub-agent names found in last AssistantMessage, routing to FINISH");
		return new RoutingExtract(new ArrayList<>(List.of("FINISH")), assistantMessage);
	}

	private record RoutingExtract(Object routingValue, AssistantMessage routingMessage) { }
}
