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
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/29 15:12
 */
@Configuration
public class ParallelGraphConfiguration {

	@Bean
	public StateGraph parallelGraph(ChatModel chatModel) throws GraphStateException {
		ChatClient client = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
		// 状态工厂注册字段与策略
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("inputText", new ReplaceStrategy());
			keyStrategyHashMap.put("sentiment", new ReplaceStrategy());
			keyStrategyHashMap.put("keywords", new ReplaceStrategy());
			keyStrategyHashMap.put("analysis", new ReplaceStrategy());
			return keyStrategyHashMap;
		};
		StateGraph graph = new StateGraph("ParallelDemo", keyStrategyFactory)
			// 注册节点
			.addNode("start", node_async(new InputNode()))
			.addNode("sentiment", node_async(new SentimentAnalysisNode(client, "inputText")))
			.addNode("keyword", node_async(new KeywordExtractionNode(client, "inputText")))
			.addNode("merge", node_async(new MergeResultsNode()))

			// 构建并行边：使用单条边携带多目标
			.addEdge(START, "sentiment")
			.addEdge(START, "keyword")
			// 限制：sentiment/keyword 并行后必须合并到同一节点
			.addEdge("sentiment", "merge")
			.addEdge("keyword", "merge")

			// 在 merge 后结束
			.addEdge("merge", END);

		// 可视化
		GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "parallel demo flow");
		System.out.println("\n=== Parallel Demo UML Flow ===");
		System.out.println(representation.content());
		System.out.println("==================================\n");

		return graph;
	}

	static class InputNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String text = (String) state.value("inputText").orElse("");
			// 可在此校验或预处理
			return Map.of("inputText", text);
		}

	}

	static class SentimentAnalysisNode implements NodeAction {

		private final ChatClient client;

		private final String key;

		public SentimentAnalysisNode(ChatClient client, String key) {
			this.client = client;
			this.key = key;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			String text = (String) state.value(key).orElse("");
			// 调用 LLM
			ChatResponse resp = client.prompt().user("emotion analysis from: " + text).call().chatResponse();
			String sentiment = resp.getResult().getOutput().getText();
			return Map.of("sentiment", sentiment);
		}

	}

	static class KeywordExtractionNode implements NodeAction {

		private final ChatClient client;

		private final String key;

		public KeywordExtractionNode(ChatClient client, String key) {
			this.client = client;
			this.key = key;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			String text = (String) state.value(key).orElse("");
			ChatResponse resp = client.prompt().user("Extract keywords from: " + text).call().chatResponse();
			String kws = resp.getResult().getOutput().getText();
			return Map.of("keywords", List.of(kws.split(",\\s*")));
		}

	}

	static class MergeResultsNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String sent = (String) state.value("sentiment").orElse("unknown");
			List<?> kws = (List<?>) state.value("keywords").orElse(List.of());
			return Map.of("analysis", Map.of("sentiment", sent, "keywords", kws));
		}

	}

}
