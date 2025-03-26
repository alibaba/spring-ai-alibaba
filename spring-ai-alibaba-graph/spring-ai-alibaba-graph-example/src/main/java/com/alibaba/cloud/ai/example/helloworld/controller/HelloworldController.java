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

package com.alibaba.cloud.ai.example.helloworld.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.example.helloworld.ControllerAgent;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/helloworld")
public class HelloworldController {

	String planningPrompt = "Your are a task planner, please analyze the task and plan the steps.";
	String stepPrompt = "Tools available: xxx";

	private final ChatClient planningClient;
	private final ChatClient stepClient;

	@Autowired
	private ToolCallbackResolver resolver;

	// 也可以使用如下的方式注入 ChatClient
	 public HelloworldController(ChatModel chatModel) {
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

	/**
	 * ChatClient 简单调用
	 */
	@GetMapping("/simple/chat")
	public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？")String query) throws GraphStateException {
		OverAllState state = new OverAllState();
		// prompt_for_next_step
		// result
		// messages

		ControllerAgent controllerAgent = new ControllerAgent();
		ReactAgent planningAgent = new ReactAgent(planningPrompt, planningClient, resolver, 10);
		ReactAgent stepAgent = new ReactAgent(stepPrompt, stepClient, resolver, 10);

		StateGraph graph2 = new StateGraph(state)
				.addSubgraph("planning_agent", planningAgent.getStateGraph())
				.addNode("controller_agent", node_async(controllerAgent))
				.addSubgraph("step_executing_agent", stepAgent.getStateGraph())

				.addEdge(START, "planning_agent")
				.addEdge("planning_agent", "controller_agent")
				.addConditionalEdges("controller_agent", edge_async(controllerAgent::think), Map.of("continue", "step_executing_agent", "end", END))
				.addEdge("step_executing_agent", "controller_agent");

		CompiledGraph compiledGraph = graph2.compile();
		Optional<OverAllState> result = compiledGraph.invoke(Map.of());
		return result.get().data().toString();
	}

}
