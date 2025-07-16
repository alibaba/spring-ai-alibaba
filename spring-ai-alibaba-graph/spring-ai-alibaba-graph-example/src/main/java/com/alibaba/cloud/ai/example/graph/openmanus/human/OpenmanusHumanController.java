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

package com.alibaba.cloud.ai.example.graph.openmanus.human;

import com.alibaba.cloud.ai.example.graph.openmanus.SupervisorAgent;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

	private PlanningTool planningTool = new PlanningTool();

	// 也可以使用如下的方式注入 ChatClient
	public OpenmanusHumanController(ChatModel chatModel) {
		this.planningClient = ChatClient.builder(chatModel)
			.defaultSystem(planningPrompt)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.stepClient = ChatClient.builder(chatModel)
			.defaultSystem(stepPrompt)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();
	}

	@GetMapping("/init")
	public void initGraph() throws GraphStateException {

		SupervisorAgent supervisorAgent = new SupervisorAgent(planningTool);
		ReactAgent planningAgent = new ReactAgent("planningAgent", planningClient, resolver, 10);
		planningAgent.getAndCompileGraph();
		ReactAgent stepAgent = new ReactAgent("stepAgent", stepClient, resolver, 10);
		stepAgent.getAndCompileGraph();
		HumanNode humanNode = new HumanNode();

		StateGraph graph2 = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("plan", new ReplaceStrategy());
			strategies.put("step_prompt", new ReplaceStrategy());
			strategies.put("step_output", new ReplaceStrategy());
			strategies.put("final_output", new ReplaceStrategy());
			return strategies;
		}).addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
			.addNode("human", node_async(humanNode))
			.addNode("supervisor_agent", node_async(supervisorAgent))
			.addNode("step_executing_agent", stepAgent.asAsyncNodeAction("step_prompt", "step_output"))

			.addEdge(START, "planning_agent")
			.addEdge("planning_agent", "human")
			.addConditionalEdges("human", edge_async(humanNode::think),
					Map.of("planning_agent", "planning_agent", "supervisor_agent", "supervisor_agent"))
			.addConditionalEdges("supervisor_agent", edge_async(supervisorAgent::think),
					Map.of("continue", "step_executing_agent", "end", END))
			.addEdge("step_executing_agent", "supervisor_agent");

		this.compiledGraph = graph2.compile();

		GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");
	}

	@GetMapping("/chat")
	public String simpleChat(String query) throws GraphRunnerException {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId("1").build();
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", query), runnableConfig);
		// send back to user and wait for plan approval
		return result.get().data().toString();
	}

	@GetMapping("/resume")
	public String resume() throws GraphRunnerException {
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
	public String resumeToNextStep() throws GraphRunnerException {
		String nextNode = "supervisor_agent";

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
