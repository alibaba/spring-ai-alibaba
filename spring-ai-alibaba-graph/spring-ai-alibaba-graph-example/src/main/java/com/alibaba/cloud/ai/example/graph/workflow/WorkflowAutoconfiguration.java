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

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class WorkflowAutoconfiguration {

	@Bean
	public StateGraph workflowGraph(ChatModel chatModel) throws GraphStateException {

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		QuestionClassifierNode feedbackClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.outputKey("classifier_output")
			.categories(List.of("positive feedback", "negative feedback"))
			.classificationInstructions(
					List.of("Try to understand the user's feeling when he/she is giving the feedback."))
			.build();

		QuestionClassifierNode specificQuestionClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.outputKey("classifier_output")
			.categories(List.of("after-sale service", "transportation", "product quality", "others"))
			.classificationInstructions(List
				.of("What kind of service or help the customer is trying to get from us? Classify the question based on your understanding."))
			.build();

		StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("classifier_output", new ReplaceStrategy());
			strategies.put("solution", new ReplaceStrategy());
			return strategies;
		}).addNode("feedback_classifier", node_async(feedbackClassifier))
			.addNode("specific_question_classifier", node_async(specificQuestionClassifier))
			.addNode("recorder", node_async(new RecordingNode()))

			.addEdge(START, "feedback_classifier")
			.addConditionalEdges("feedback_classifier",
					edge_async(new CustomerServiceController.FeedbackQuestionDispatcher()),
					Map.of("positive", "recorder", "negative", "specific_question_classifier"))
			.addConditionalEdges("specific_question_classifier",
					edge_async(new CustomerServiceController.SpecificQuestionDispatcher()),
					Map.of("after-sale", "recorder", "transportation", "recorder", "quality", "recorder", "others",
							"recorder"))
			.addEdge("recorder", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		return stateGraph;
	}

}
