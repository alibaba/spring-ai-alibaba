/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.runner;

import java.util.List;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.apache.tika.utils.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class RoutingEdgeAction implements EdgeAction {

	private ChatClient chatClient;

	public RoutingEdgeAction(ChatModel chatModel, NodeAgent current, List<? extends BaseNodeAgent> subAgents) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system. Here's the task and instructions that you are responsible for: " );
		sb.append(StringUtils.isEmpty(current.instruction()) ? current.description : current.instruction());
		sb.append("\n\n");
		sb.append("There're a few agents that can handle this task, you can delegate the task to one of the following.");
		sb.append("The agents ability are listed in a 'name:description' format as below:\n");
		for (BaseNodeAgent agent : subAgents) {
			sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
		}
		String prompt = current.instruction();
		sb.append("\n\n");
		sb.append("Return the agent name to delegate the task to.");

		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(prompt)
				.build();
	}

	@Override
	public String apply(OverAllState state) throws Exception {
		return this.chatClient.prompt().call().content();
	}

}
