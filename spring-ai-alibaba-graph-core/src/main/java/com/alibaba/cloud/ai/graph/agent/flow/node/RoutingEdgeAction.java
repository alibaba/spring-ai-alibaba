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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.apache.tika.utils.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class RoutingEdgeAction implements AsyncEdgeAction {

	private ChatClient chatClient;

	private String taskKey;

	public RoutingEdgeAction(ChatModel chatModel, BaseAgent current, List<BaseAgent> subAgents) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system.\n");

		if (current instanceof ReactAgent reactAgent) {
			sb.append("The instruction that you should follow is to finish this task is: ");
			sb.append(StringUtils.isEmpty(reactAgent.instruction()) ? reactAgent.description()
					: reactAgent.instruction());
		}
		else {
			sb.append("Your role seen by the user is: ");
			sb.append(current.description());
		}

		sb.append("\n\n");
		sb.append(
				"There're a few agents that can handle this task, you can delegate the task to one of the following.");
		sb.append("The agents ability are listed in a 'name:description' format as below:\n");
		for (BaseAgent agent : subAgents) {
			sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
		}
		sb.append("\n\n");
		sb.append("Return the agent name to delegate the task to.");
		sb.append("\n\n");
		sb.append(
				"It should be emphasized that the returned result only requires the agent name and no other content.");
		sb.append("\n\n");
		sb.append(
				"For example, if you want to delegate the task to the agent named 'agent1', you should return 'agent1'.");

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
		this.taskKey = current.outputKey();
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			String taskDetail = state.value(taskKey, "");
			result.complete(this.chatClient.prompt(taskDetail).call().content());
		}
		catch (Exception e) {
			result.completeExceptionally(e);
		}
		return result;
	}

}
