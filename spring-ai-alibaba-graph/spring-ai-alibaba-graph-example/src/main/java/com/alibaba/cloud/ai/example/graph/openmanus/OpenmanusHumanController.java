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

package com.alibaba.cloud.ai.example.graph.openmanus;

import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/manus/human")
public class OpenmanusHumanController {

	String planningPrompt = "Your are a task planner, please analyze the task and plan the steps.";

	String stepPrompt = "Tools available: xxx";

	private final ChatClient planningClient;

	private final ChatClient stepClient;

	@Autowired
	private ToolCallbackResolver resolver;

	private CompiledGraph compiledGraph;

	private PlanningTool planningTool = new PlanningTool(Map.of());


	// 也可以使用如下的方式注入 ChatClient
	public OpenmanusHumanController(ChatModel chatModel) {
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
	}

	@GetMapping("/init")
	public void initGraph() throws GraphStateException {
		AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("plan", (o1, o2) -> o2);
			state.registerKeyAndStrategy("step_prompt", (o1, o2) -> o2);
			state.registerKeyAndStrategy("step_output", (o1, o2) -> o2);
			state.registerKeyAndStrategy("final_output", (o1, o2) -> o2);

			state.input(inputs);
			return state;
		};

		SupervisorAgent controllerAgent = new SupervisorAgent();
		ReactAgent planningAgent = new ReactAgent("请完成用户接下来输入的任务规划。",planningClient, resolver, 10);
		planningAgent.getAndCompileGraph();
		ReactAgent stepAgent = new ReactAgent("请完成用户接下来输入的任务规划。",stepClient, resolver, 10);
		stepAgent.getAndCompileGraph();
		HumanNode humanNode = new HumanNode();

		StateGraph graph2 = new StateGraph(stateFactory)
				.addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
				.addNode("human", node_async(humanNode))
				.addNode("controller_agent", node_async(controllerAgent))
				.addNode("step_executing_agent", stepAgent.asAsyncNodeAction("step_prompt", "step_output"))

				.addEdge(START, "planning_agent")
				.addEdge("planning_agent", "human")
				.addConditionalEdges("human", edge_async(humanNode::think), Map.of("planning_agent", "planning_agent", "controller_agent", "controller_agent"))
				.addConditionalEdges("controller_agent", edge_async(controllerAgent::think),
						Map.of("continue", "step_executing_agent", "end", END))
				.addEdge("step_executing_agent", "controller_agent");

		this.compiledGraph = graph2.compile();

		GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");
	}

	@GetMapping("/chat")
	public String simpleChat(String query) {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("1").build();
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", query), runnableConfig);
		// send back to user and wait for plan approval
		return result.get().data().toString();
	}

	@GetMapping("/resume")
	public String resume() {
		Map<String, Object> data = Map.of("input", "请帮我查询最近的新闻");
		String nextNode = "planning_agent";

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("1").build();

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(data, nextNode));

		Optional<OverAllState> result = compiledGraph.invoke(state, runnableConfig);
		// send back to user and wait for plan approval

		return result.get().data().toString();
	}

	@GetMapping("/resume-to-next-step")
	public String resumtToNextStep() {
		String nextNode = "controller_agent";

		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("1").build();

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(Map.of(), nextNode));

		Optional<OverAllState> result = compiledGraph.invoke(state, runnableConfig);
		// send back to user and wait for plan approval

		return result.get().data().toString();
	}

}
