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

package com.alibaba.cloud.ai.example.graph.openmanus;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.Builder;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.example.graph.openmanus.OpenManusPrompt.PLANNING_SYSTEM_PROMPT;
import static com.alibaba.cloud.ai.example.graph.openmanus.OpenManusPrompt.STEP_SYSTEM_PROMPT;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/manus")
public class OpenmanusController {

	private final ChatClient planningClient;

	private final ChatClient stepClient;

	private CompiledGraph compiledGraph;

	// 也可以使用如下的方式注入 ChatClient
	public OpenmanusController(ChatModel chatModel) throws GraphStateException {

		this.planningClient = ChatClient.builder(chatModel)
			.defaultSystem(PLANNING_SYSTEM_PROMPT)
			// .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultToolCallbacks(Builder.getToolCallList())// tools registered will only
			// be used
			// as tool description
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.stepClient = ChatClient.builder(chatModel)
			.defaultSystem(STEP_SYSTEM_PROMPT)
			// .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultToolCallbacks(Builder.getManusAgentToolCalls())// tools registered
			// will only
			// be used as tool description
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		initGraph();
	}

	public void initGraph() throws GraphStateException {

		SupervisorAgent supervisorAgent = new SupervisorAgent(PlanningTool.INSTANCE);
		ReactAgent planningAgent = new ReactAgent("planningAgent", planningClient, Builder.getFunctionCallbackList(),
				10);
		planningAgent.getAndCompileGraph();
		ReactAgent stepAgent = new ReactAgent("stepAgent", stepClient, Builder.getManusAgentFunctionCallbacks(), 10);
		stepAgent.getAndCompileGraph();

		StateGraph graph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("plan", new ReplaceStrategy());
			strategies.put("step_prompt", new ReplaceStrategy());
			strategies.put("step_output", new ReplaceStrategy());
			strategies.put("final_output", new ReplaceStrategy());
			return strategies;
		}).addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
			.addNode("supervisor_agent", node_async(supervisorAgent))
			.addNode("step_executing_agent", stepAgent.asAsyncNodeAction("step_prompt", "step_output"))

			.addEdge(START, "planning_agent")
			.addEdge("planning_agent", "supervisor_agent")
			.addConditionalEdges("supervisor_agent", edge_async(supervisorAgent::think),
					Map.of("continue", "step_executing_agent", "end", END))
			.addEdge("step_executing_agent", "supervisor_agent");

		this.compiledGraph = graph.compile();

		GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");
	}

	/**
	 * ChatClient 简单调用
	 */
	@GetMapping("/chat")
	public String simpleChat(String query) throws GraphRunnerException {

		return compiledGraph.invoke(Map.of("input", query)).get().data().toString();
	}

}
