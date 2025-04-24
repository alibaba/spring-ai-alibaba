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

package com.alibaba.cloud.ai.example.graph.bigtool.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.example.graph.bigtool.agent.CalculateAgent;
import com.alibaba.cloud.ai.example.graph.bigtool.agent.Tool;
import com.alibaba.cloud.ai.example.graph.bigtool.agent.ToolAgent;
import com.alibaba.cloud.ai.example.graph.bigtool.service.VectorStoreService;
import com.alibaba.cloud.ai.example.graph.bigtool.utils.MethodUtils;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;

import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("bigtool")
public class BigToolController {

	private static final Logger logger = LoggerFactory.getLogger(BigToolController.class);

	private final VectorStoreService vectorStoreService;

	private CompiledGraph compiledGraph;

	private List<Document> documents = new ArrayList<>();

	public BigToolController(VectorStoreService vectorStoreService, ChatModel chatModel) throws GraphStateException {
		this.vectorStoreService = vectorStoreService;
		this.initializeVectorStore();
		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy(INPUT_KEY, new ReplaceStrategy());
			state.registerKeyAndStrategy(HIT_TOOL, new ReplaceStrategy());
			state.registerKeyAndStrategy(SOLUTION, new ReplaceStrategy());
			state.registerKeyAndStrategy(TOOL_LIST, new ReplaceStrategy());
			state.input(inputs);
			return state;
		};

		ToolAgent tools = new ToolAgent(chatClient, INPUT_KEY, vectorStoreService);

		CalculateAgent calculateAgent = new CalculateAgent(chatClient, INPUT_KEY);

		StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", stateFactory)
			.addNode("tools", node_async(tools))
			.addNode("calculate_agent", node_async(calculateAgent))
			.addEdge(START, "tools")
			.addEdge("tools", "calculate_agent")
			.addEdge("calculate_agent", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.MERMAID,
				"workflow graph");

		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		this.compiledGraph = stateGraph.compile();

	}

	private void initializeVectorStore() {
		List<Tool> allTools = new ArrayList<>();
		for (Method method : Math.class.getMethods()) {
			if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
				Tool tool = MethodUtils.convertMethodToTool(method);
				if (tool != null) {
					allTools.add(tool);
				}
			}
		}

		allTools.forEach(tool -> documents.add(new Document(IdUtil.fastSimpleUUID(), tool.getDescription(),
				Map.of(METHOD_NAME, tool.getName(), METHOD_PARAMETER_TYPES, tool.getParameterTypes()))));

		vectorStoreService.addDocuments(documents);

	}

	@GetMapping("/search")
	public String search(@RequestParam String query) {
		Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query, TOOL_LIST, documents));
		return invoke.get().value("solution").get().toString();
	}

}
