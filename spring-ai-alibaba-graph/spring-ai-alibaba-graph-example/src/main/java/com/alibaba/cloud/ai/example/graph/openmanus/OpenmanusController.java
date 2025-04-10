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

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/manus")
public class OpenmanusController {

	String planningPrompt = "Your are a task planner, please analyze the task and plan the steps.";

	String stepPrompt = "Tools available: xxx";

	private final ChatClient planningClient;

	private final ChatClient stepClient;

	@Autowired
	private ToolCallbackResolver resolver;

	private CompiledGraph compiledGraph;

	// 也可以使用如下的方式注入 ChatClient
	public OpenmanusController(ChatModel chatModel) throws GraphStateException {
		this.planningClient = ChatClient.builder(chatModel)
			.defaultSystem(planningPrompt)
			.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.stepClient = ChatClient.builder(chatModel)
			.defaultSystem(stepPrompt)
			.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		initGraph();
	}

	public void initGraph() throws GraphStateException {
		AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("plan", new ReplaceStrategy());
			state.registerKeyAndStrategy("step_prompt", new ReplaceStrategy());
			state.registerKeyAndStrategy("step_output", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_output", new ReplaceStrategy());

			state.input(inputs);
			return state;
		};

		SupervisorAgent supervisorAgent = new SupervisorAgent();
		ReactAgent planningAgent = new ReactAgent("planningAgent", "请帮助用户完成他接下来输入的任务规划。", planningClient, resolver, 10);
		planningAgent.getAndCompileGraph();
		ReactAgent stepAgent = new ReactAgent("stepAgent", "请帮助用户完成他接下来输入的任务规划。", stepClient, resolver, 10);
		stepAgent.getAndCompileGraph();

		StateGraph graph = new StateGraph(stateFactory)
			.addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
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
	public String simpleChat(String query) throws GraphStateException {
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", query));
		return result.get().data().toString();
	}

}
