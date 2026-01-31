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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * Edge action that reads the mainAgent (ReactAgent) output from state and routes either
 * to END (when "FINISH" or empty) or to the SupervisorNode (when a list of sub-agent
 * names is present). Expects state key {@value SupervisorNodeFromState#SUPERVISOR_NEXT_KEY}
 * or a custom routing key.
 */
public class MainAgentToSupervisorEdgeAction implements AsyncEdgeAction {

	private final String routingKey;
	private final String supervisorNodeName;

	public MainAgentToSupervisorEdgeAction(String routingKey, String supervisorNodeName) {
		this.routingKey = routingKey != null ? routingKey : SupervisorNodeFromState.SUPERVISOR_NEXT_KEY;
		this.supervisorNodeName = supervisorNodeName;
	}

	public MainAgentToSupervisorEdgeAction(String supervisorNodeName) {
		this(SupervisorNodeFromState.SUPERVISOR_NEXT_KEY, supervisorNodeName);
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		Object value = state.value(routingKey).orElse(null);
		if (isFinishOrEmpty(value)) {
			return CompletableFuture.completedFuture(END);
		}
		return CompletableFuture.completedFuture(supervisorNodeName);
	}

	private static boolean isFinishOrEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof AssistantMessage assistantMessage) {
			String text = assistantMessage.getText();
			if (!StringUtils.hasText(text)) {
				return true;
			}
			return isFinishOrEmptyText(text.trim());
		}
		if (value instanceof List list) {
			return list.isEmpty() || list.stream().allMatch(
					e -> e == null || "FINISH".equalsIgnoreCase(String.valueOf(e).trim()));
		}
		String s = String.valueOf(value).trim();
		return s.isEmpty() || "FINISH".equalsIgnoreCase(s);
	}

	private static boolean isFinishOrEmptyText(String text) {
		if (!StringUtils.hasText(text) || "FINISH".equalsIgnoreCase(text) || "[]".equals(text)) {
			return true;
		}
		try {
			List<?> list = JsonParser.fromJson(text, List.class);
			return list == null || list.isEmpty() || list.stream().allMatch(
					e -> e == null || "FINISH".equalsIgnoreCase(String.valueOf(e).trim()));
		}
		catch (Exception e) {
			return false;
		}
	}
}
