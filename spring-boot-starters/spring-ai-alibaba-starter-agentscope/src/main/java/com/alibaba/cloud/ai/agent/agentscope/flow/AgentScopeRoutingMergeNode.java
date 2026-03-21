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
package com.alibaba.cloud.ai.agent.agentscope.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;

/**
 * Merge node for routing results that uses an AgentScope {@link ReActAgent} (assembled
 * with a Model and system prompt) to synthesize sub-agent outputs. Equivalent to the
 * framework's {@link RoutingMergeNode} which uses {@link org.springframework.ai.chat.model.ChatModel#call};
 * here we use ReActAgent's {@link ReActAgent#call(java.util.List)} to obtain the merged
 * text. No direct Model calls—all invocation goes through the ReActAgent.
 */
public class AgentScopeRoutingMergeNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(AgentScopeRoutingMergeNode.class);

	private static final String SYNTHESIZE_SYSTEM_TEMPLATE = """
			Synthesize these search results to answer the original question: "%s"

			- Combine information from multiple sources without redundancy
			- Highlight the most relevant and actionable information
			- Note any discrepancies between sources
			- Keep the response concise and well-organized
			""";

	private final io.agentscope.core.model.Model model;
	private final List<BaseAgent> subAgents;
	private final String mergedOutputKey;

	public AgentScopeRoutingMergeNode(io.agentscope.core.model.Model model, List<BaseAgent> subAgents) {
		this(model, subAgents, RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY);
	}

	public AgentScopeRoutingMergeNode(io.agentscope.core.model.Model model, List<BaseAgent> subAgents,
			String mergedOutputKey) {
		this.model = model;
		this.subAgents = subAgents;
		this.mergedOutputKey = mergedOutputKey != null ? mergedOutputKey : RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("AgentScopeRoutingMergeNode: merging results from {} sub-agents", subAgents.size());

		List<String> formattedResults = new ArrayList<>();
		for (BaseAgent subAgent : subAgents) {
			String outputKey = subAgent.getOutputKey();
			if (outputKey == null) {
				continue;
			}
			Optional<Object> outputOpt = state.value(outputKey);
			if (outputOpt.isPresent()) {
				String text = extractText(outputOpt.get());
				if (text != null && !text.isBlank()) {
					String source = capitalize(subAgent.name());
					formattedResults.add("**From " + source + ":**\n" + text);
					logger.debug("Collected result from {} (key: {})", subAgent.name(), outputKey);
				}
			}
		}

		String query = extractOriginalQuery(state);
		String finalAnswer = synthesize(query, formattedResults);
		logger.debug("AgentScopeRoutingMergeNode: synthesized {} sources into merged result", formattedResults.size());

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
		ReActAgent reactAgent = ReActAgent.builder()
				.name("merge")
				.model(model)
				.sysPrompt(systemPrompt)
				.memory(new InMemoryMemory())
				.build();
		List<Msg> msgs = List.of(
				Msg.builder().name("user").role(MsgRole.USER).textContent(formatted).build());
		Msg response = reactAgent.call(msgs).block();
		if (response == null) {
			return "";
		}
		String text = response.getTextContent();
		return text != null ? text : "";
	}

	private static String extractText(Object output) {
		if (output instanceof Message message) {
			return message.getText();
		}
		if (output instanceof GraphResponse<?> gr) {
			Optional<?> val = gr.resultValue();
			if (val.isPresent()) {
				Object v = val.get();
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
		if (s == null || s.isEmpty()) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
