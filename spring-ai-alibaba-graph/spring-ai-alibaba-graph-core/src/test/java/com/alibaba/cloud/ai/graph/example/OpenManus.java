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
package com.alibaba.cloud.ai.graph.example;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.node.StateAdaptorNode;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class OpenManus {
	public static void main(String[] args) throws GraphStateException {
		OverAllState state = new OverAllState();
		// prompt_for_next_step
		// result
		// messages

		ControllerAgent controllerAgent = new ControllerAgent();

		String planningPrompt = "Your are a task planner, please analyze the task and plan the steps.";
		List<FunctionCallback> tools = List.of();
		ToolCallbackResolver resolver = null;
		ChatModel chatModel = null;
		ChatClient chatClient = ChatClient.builder(chatModel).defaultOptions(DashScopeChatOptions.builder().build()).defaultTools(tools).build();
		ReactAgent planningAgent = new ReactAgent(planningPrompt, chatClient, tools, 10);

		String stepPrompt = "Tools available: xxx";
		ReactAgent stepAgent = new ReactAgent(stepPrompt, chatClient, resolver, 10);

		StateGraph graph2 = new StateGraph(state)
				.addSubgraph("planning_agent", planningAgent.getStateGraph())
				.addNode("controller_agent", node_async(controllerAgent))
				.addSubgraph("step_executing_agent", stepAgent.getStateGraph())

				.addEdge(START, "planning_agent")
				.addEdge("planning_agent", "controller_agent")
				.addEdge("controller_agent", "step_executing_agent");

		CompiledGraph compiledGraph = graph2.compile();
		compiledGraph.invoke(Map.of());
	}
}
