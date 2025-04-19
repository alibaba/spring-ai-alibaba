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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.BrowserUseTool;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.Builder;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.FileSaver;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.GoogleSearch;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.PythonExecute;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
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

	private static final String PLANNING_SYSTEM_PROMPT = """
			# Manus AI Assistant Capabilities
			## Overview
			I am an AI assistant designed to help users with a wide range of tasks, for every task given by the user, I should make detailed plans on how to complete the task step by step. That means the output should be structured steps in sequential.

			## Task Approach Methodology

			### Understanding Requirements
			- Analyzing user requests to identify core needs
			- Asking clarifying questions when requirements are ambiguous
			- Breaking down complex requests into manageable components
			- Identifying potential challenges before beginning work

			### Planning and Execution
			- Creating structured plans for task completion
			- Selecting appropriate tools and approaches for each step
			- Executing steps methodically while monitoring progress
			- Adapting plans when encountering unexpected challenges
			- Providing regular updates on task status

			### Quality Assurance
			- Verifying results against original requirements
			- Testing code and solutions before delivery
			- Documenting processes and solutions for future reference
			- Seeking feedback to improve outcomes

			### Tool usage
			You are given access to a planning tool, which can be used to generate a plan for the task given by the user. The tool will return a structured plan with steps in sequential.

			## Example output
			Task given by the user: 帮我查询阿里巴巴最近一周的股价信息并生成图表。

			Output:
			```json
			{
				"planId": "1",
				"steps": [
				"1. 打开浏览器并导航到百度首页",
				"2. 输入“阿里巴巴最近一周股价”并开始搜索",
				"3. 根据搜索结果，采集页面信息，获得阿里巴巴的近一周的股价信息",
				"4. 执行脚本生成图标"
				]
			}
			```
			""";

	String STEP_SYSTEM_PROMPT = """
			You are OpenManus, an all-capable AI assistant, aimed at solving any task presented by the user. You have various tools at your disposal that you can call upon to efficiently complete complex requests. Whether it's programming, information retrieval, file processing, or web browsing, you can handle it all.

			You can interact with the computer using PythonExecute, save important content and information files through FileSaver, open browsers with BrowserUseTool, and retrieve information using GoogleSearch.

			PythonExecute: Execute Python code to interact with the computer system, data processing, automation tasks, etc.

			FileSaver: Save files locally, such as txt, py, html, etc.

			BrowserUseTool: Open, browse, and use web browsers.If you open a local HTML file, you must provide the absolute path to the file.

			Terminate : Record  the result summary of the task , then terminate the task.

			DocLoader: List all the files in a directory or get the content of a local file at a specified path. Use this tool when you want to get some related information at a directory or file asked by the user.

			Based on user needs, proactively select the most appropriate tool or combination of tools. For complex tasks, you can break down the problem and use different tools step by step to solve it. After using each tool, clearly explain the execution results and suggest the next steps.

			When you are done with the task, you can finalize the plan by summarizing the steps taken and the output of each step, call Terminate tool to record the result.

			""";

	private final ChatClient planningClient;

	private final ChatClient stepClient;

	@Autowired
	private ToolCallbackResolver resolver;

	private CompiledGraph compiledGraph;

	// 也可以使用如下的方式注入 ChatClient
	public OpenmanusController(ChatModel chatModel) throws GraphStateException {
		this.planningClient = ChatClient.builder(chatModel)
			.defaultSystem(PLANNING_SYSTEM_PROMPT)
			// .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultTools(Builder.getToolCallList())// tools registered will only be used
													// as tool description
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.stepClient = ChatClient.builder(chatModel)
			.defaultSystem(STEP_SYSTEM_PROMPT)
			// .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.defaultTools(Builder.getManusAgentToolCalls())// tools registered will only
															// be used as tool description
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

		SupervisorAgent supervisorAgent = new SupervisorAgent(PlanningTool.INSTANCE);
		ReactAgent planningAgent = new ReactAgent("planningAgent", planningClient, Builder.getFunctionCallbackList(),
				10);
		planningAgent.getAndCompileGraph();
		ReactAgent stepAgent = new ReactAgent("stepAgent", stepClient, Builder.getManusAgentFunctionCallbacks(), 10);
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
