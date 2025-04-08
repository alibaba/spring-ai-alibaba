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
package com.alibaba.cloud.ai.example.graph.react;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.example.graph.workflow.RecordingNode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

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
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/react")
public class ReactController {

	private final ChatClient chatClient;

	private CompiledGraph compiledGraph;

	@Autowired
	private ToolCallbackResolver resolver;

	ReactController(ChatModel chatModel) throws GraphStateException {
		this.chatClient = ChatClient.builder(chatModel)
			.defaultTools("getWeatherFunction")
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		initGraph();
	}

	public void initGraph() throws GraphStateException {
		AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("messages", new AppendStrategy());
			state.input(inputs);
			return state;
		};

		ReactAgent planningAgent = new ReactAgent("请帮助用户完成他接下来输入的任务规划。", chatClient, resolver, 10);
		this.compiledGraph = planningAgent.getAndCompileGraph();

		GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");
	}

	@GetMapping("/chat")
	public String simpleChat(String query) throws GraphStateException {
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", query));
		return result.get().value("solution").get().toString();
	}

	public static class FeedbackQuestionDispatcher implements EdgeAction {

		@Override
		public String apply(OverAllState state) throws Exception {
			String classifierOutput = (String) state.value("classifier_output").orElse("");
			System.out.println("classifierOutput: " + classifierOutput);
			if (classifierOutput.contains("positive")) {
				return "positive";
			}
			return "negative";
		}

	}

	public static class SpecificQuestionDispatcher implements EdgeAction {

		@Override
		public String apply(OverAllState state) throws Exception {
			String classifierOutput = (String) state.value("classifier_output").orElse("");
			System.out.println("classifierOutput: " + classifierOutput);
			if (classifierOutput.contains("after-sale")) {
				return "after-sale";
			}
			else if (classifierOutput.contains("quality")) {
				return "quality";
			}
			else if (classifierOutput.contains("transportation")) {
				return "transportation";
			}
			else {
				return "others";
			}
		}

	}

}
