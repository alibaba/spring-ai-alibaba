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
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
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
import java.util.Map;
import java.util.Optional;

/**
 * Dedicated node for merging routing results. Collects outputs from sub-agents routed by
 * LlmRoutingAgent, synthesizes them via LLM into a single coherent answer, and writes the
 * merged result to state.
 */
public class RoutingMergeNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RoutingMergeNode.class);

	/** Default key for the merged result in state. */
	public static final String DEFAULT_MERGED_OUTPUT_KEY = "merged_result";

	private static final String SYNTHESIZE_SYSTEM_TEMPLATE = """
			Synthesize these search results to answer the original question: "%s"

			- Combine information from multiple sources without redundancy
			- Highlight the most relevant and actionable information
			- Note any discrepancies between sources
			- Keep the response concise and well-organized
			""";

	private final ChatModel chatModel;
	private final List<BaseAgent> subAgents;
	private final String mergedOutputKey;

	public RoutingMergeNode(ChatModel chatModel, List<BaseAgent> subAgents) {
		this(chatModel, subAgents, DEFAULT_MERGED_OUTPUT_KEY);
	}

	public RoutingMergeNode(ChatModel chatModel, List<BaseAgent> subAgents, String mergedOutputKey) {
		this.chatModel = chatModel;
		this.subAgents = subAgents;
		this.mergedOutputKey = mergedOutputKey != null ? mergedOutputKey : DEFAULT_MERGED_OUTPUT_KEY;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("RoutingMergeNode: merging results from {} sub-agents", subAgents.size());

		List<String> formattedResults = new ArrayList<>();
		for (BaseAgent subAgent : subAgents) {
			String outputKey = subAgent.getOutputKey();
			if (outputKey == null) {
				continue;
			}
			Optional<Object> outputOpt = state.value(outputKey);
			if (outputOpt.isPresent()) {
				String text = extractText(outputOpt.get(), outputKey);
				if (text != null && !text.isBlank()) {
					String source = capitalize(subAgent.name());
					formattedResults.add("**From " + source + ":**\n" + text);
					logger.debug("Collected result from {} (key: {})", subAgent.name(), outputKey);
				}
			}
		}

		String query = extractOriginalQuery(state);
		String finalAnswer = synthesize(query, formattedResults);
		logger.debug("RoutingMergeNode: synthesized {} sources into merged result", formattedResults.size());

		return Map.of(mergedOutputKey, finalAnswer);
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
		if (formattedResults == null || formattedResults.isEmpty()) {
			return "No results found from any knowledge source.";
		}
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
		if (output instanceof GraphResponse<?> gr) {
			Optional<?> val = gr.resultValue();
			if (val.isPresent()) {
				Object v = val.get();
				if (v instanceof Map<?, ?> map) {
					v = map.get(outputKey);
				}
				if (v instanceof Message m) {
					return m.getText();
				}
				return v.toString();
			}
			return "";
		}
		return output != null ? output.toString() : "";
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
