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
package com.alibaba.cloud.ai.example.graph.workflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/customer")
public class CustomerServiceController {

	private final ChatClient chatClient;
	private CompiledGraph compiledGraph;

	CustomerServiceController(ChatModel chatModel) throws GraphStateException {
		this.chatClient = ChatClient.builder(chatModel)
//				.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
				.defaultAdvisors(new SimpleLoggerAdvisor())
				.build();

		initGraph();
	}

	public void initGraph() throws GraphStateException {
		AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("classifier_output", new ReplaceStrategy());
			state.registerKeyAndStrategy("solution", new ReplaceStrategy());

			state.input(inputs);
			return state;
		};

		QuestionClassifierNode feedbackClassifier = QuestionClassifierNode.builder()
				.chatClient(chatClient)
				.inputTextKey("input")
				.categories(List.of("positive feedback", "negative feedback"))
				.classificationInstructions(List.of("Try to understand the user's feeling when he/she is giving the feedback."))
				.build();

		QuestionClassifierNode specificQuestionClassifier = QuestionClassifierNode.builder()
				.chatClient(chatClient)
				.inputTextKey("input")
				.categories(List.of("after-sale service", "transportation", "product quality", "others"))
				.classificationInstructions(List.of("What kind of service or help the customer is trying to get from us? Classify the question based on your understanding."))
				.build();

		StateGraph graph = new StateGraph(stateFactory)
				.addNode("feedback_classifier", node_async(feedbackClassifier))
				.addNode("specific_question_classifier", node_async(specificQuestionClassifier))
				.addNode("recorder", node_async(new RecordingNode()))

				.addEdge(START, "feedback_classifier")
				.addConditionalEdges("feedback_classifier", edge_async(new FeedbackQuestionDispatcher()),
						Map.of("positive", "recorder", "negative", "specific_question_classifier"))
				.addConditionalEdges("specific_question_classifier", edge_async(new SpecificQuestionDispatcher()),
						Map.of("after-sale", "recorder", "transportation", "recorder", "quality", "recorder", "others", "recorder"))
				.addEdge("recorder", END);


		this.compiledGraph = graph.compile();

		GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");
	}

	@GetMapping("/chat")
	public String simpleChat(String query)
			throws GraphStateException {
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
			} else if (classifierOutput.contains("quality")) {
				return "quality";
			} else if (classifierOutput.contains("transportation")) {
				return "transportation";
			} else {
				return "others";
			}
		}
	}
}
