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

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.node.McpNode;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class WorkflowAutoconfiguration {

	@Bean
	public StateGraph workflowGraph(ChatModel chatModel) throws GraphStateException {

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("classifier_output", new ReplaceStrategy());
			state.registerKeyAndStrategy("solution", new ReplaceStrategy());
			return state;
		};

		QuestionClassifierNode feedbackClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.categories(List.of("positive feedback", "negative feedback"))
			.classificationInstructions(
					List.of("Try to understand the user's feeling when he/she is giving the feedback."))
			.build();

		QuestionClassifierNode specificQuestionClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.categories(List.of("after-sale service", "transportation", "product quality", "others"))
			.classificationInstructions(List
				.of("What kind of service or help the customer is trying to get from us? Classify the question based on your understanding."))
			.build();

		// 示例：添加 MCP Node
		McpNode mcpNode = McpNode.builder()
			.url("http://localhost:8181/sse") // MCP Server SSE 地址
			.tool("getWeatherForecastByLocation") // MCP 工具名（需根据实际 MCP Server 配置）
			.param("latitude",39.9042) // 工具参数
			.param("longitude",116.4074) // 工具参数
				.header("clientId", "111222") // 可选：添加请求头
			.outputKey("mcp_result")
			.build();

		StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", stateFactory)
				.addNode("mcp_node", node_async(mcpNode))
				.addNode("feedback_classifier", node_async(feedbackClassifier))
			.addNode("specific_question_classifier", node_async(specificQuestionClassifier))
			.addNode("recorder", node_async(new RecordingNode()))

			.addEdge(START, "mcp_node")
			.addEdge("mcp_node", "feedback_classifier")
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
