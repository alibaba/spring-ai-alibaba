/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author yHong
 */

package com.alibaba.cloud.ai.studio.workflow.assistant.graph;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/24 14:29
 */
@Configuration
public class WritingAssistantAutoconfiguration {

	@Bean
	public StateGraph writingAssistantGraph(ChatModel chatModel) throws GraphStateException {

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();

			keyStrategyHashMap.put("original_text", new ReplaceStrategy());
			keyStrategyHashMap.put("summary", new ReplaceStrategy());
			keyStrategyHashMap.put("summary_feedback", new ReplaceStrategy());
			keyStrategyHashMap.put("reworded", new ReplaceStrategy());
			keyStrategyHashMap.put("title", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		StateGraph graph = new StateGraph("WritingAssistant", keyStrategyFactory)
			.addNode("summarizer", node_async(new SummarizerNode(chatClient)))
			.addNode("feedback_classifier", node_async(new SummaryFeedbackClassifierNode(chatClient, "summary")))
			.addNode("reworder", node_async(new RewordingNode(chatClient)))
			.addNode("title_generator", node_async(new TitleGeneratorNode(chatClient)))

			.addEdge(START, "summarizer")
			.addEdge("summarizer", "feedback_classifier")
			.addConditionalEdges("feedback_classifier", edge_async(new FeedbackDispatcher()),
					Map.of("positive", "reworder", "negative", "summarizer"))
			.addEdge("reworder", "title_generator")
			.addEdge("title_generator", END);

		// 添加 PlantUML 打印
		GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML,
				"writing assistant flow");
		System.out.println("\n=== Writing Assistant UML Flow ===");
		System.out.println(representation.content());
		System.out.println("==================================\n");

		return graph;
	}

	static class SummarizerNode implements NodeAction {

		private final ChatClient chatClient;

		public SummarizerNode(ChatClient chatClient) {
			this.chatClient = chatClient;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String text = (String) state.value("original_text").orElse("");
			String prompt = "请对以下中文文本进行简洁明了的摘要：\n\n" + text;

			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
			String summary = response.getResult().getOutput().getText();

			Map<String, Object> result = new HashMap<>();
			result.put("summary", summary);
			return result;
		}

	}

	static class SummaryFeedbackClassifierNode implements NodeAction {

		private final ChatClient chatClient;

		private final String inputKey;

		public SummaryFeedbackClassifierNode(ChatClient chatClient, String inputKey) {
			this.chatClient = chatClient;
			this.inputKey = inputKey;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String summary = (String) state.value(inputKey).orElse("");
			if (!StringUtils.hasText(summary)) {
				throw new IllegalArgumentException("summary is empty in state");
			}

			String prompt = """
					以下是一个自动生成的中文摘要。请你判断它是否让用户满意。如果满意，请返回 "positive"，否则返回 "negative"：

					摘要内容：
					%s
					""".formatted(summary);

			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
			String output = response.getResult().getOutput().getText();

			String classification = output.toLowerCase().contains("positive") ? "positive" : "negative";

			Map<String, Object> updated = new HashMap<>();
			updated.put("summary_feedback", classification);

			return updated;
		}

	}

	static class RewordingNode implements NodeAction {

		private final ChatClient chatClient;

		public RewordingNode(ChatClient chatClient) {
			this.chatClient = chatClient;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String summary = (String) state.value("summary").orElse("");
			String prompt = "请将以下摘要用更优美、生动的语言改写，同时保持信息不变：\n\n" + summary;

			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
			String reworded = response.getResult().getOutput().getText();

			Map<String, Object> result = new HashMap<>();
			result.put("reworded", reworded);
			return result;
		}

	}

	static class TitleGeneratorNode implements NodeAction {

		private final ChatClient chatClient;

		public TitleGeneratorNode(ChatClient chatClient) {
			this.chatClient = chatClient;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String content = (String) state.value("reworded").orElse("");
			String prompt = "请为以下内容生成一个简洁有吸引力的中文标题：\n\n" + content;

			ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
			String title = response.getResult().getOutput().getText();

			Map<String, Object> result = new HashMap<>();
			result.put("title", title);
			return result;
		}

	}

	static class FeedbackDispatcher implements EdgeAction {

		@Override
		public String apply(OverAllState state) {
			String feedback = (String) state.value("summary_feedback").orElse("");
			if (feedback.contains("positive")) {
				return "positive";
			}
			return "negative";
		}

	}
	@Bean
	public SAAGraphFlow workflowAssistantFlow(StateGraph writingAssistantGraph) {
		return SAAGraphFlow.builder()
				.id("001")
				.title("Workflow Assistant")
				.description("A workflow assistant that helps users with their workflows.")
				.ownerID("saa") // Or any owner you see fit
				.addTag("assistant")
				.addTag("workflow-generation")
				.stateGraph(writingAssistantGraph) // Correctly inject the StateGraph bean
				.build();
	}
}
