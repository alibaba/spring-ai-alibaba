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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class RemoteAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// 先创建 DashScopeApi 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// 创建 DashScope ChatModel 实例
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testRemoteAgent() throws Exception {
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("article", new ReplaceStrategy());
			keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		// A2aRemoteAgent currencyExchangeAgent = A2aRemoteAgent.builder()
		// .name("writer_agent")
		// .agentCard(RemoteAgentCard.builder().url("http://0.0.0.0:8080").build())
		// .description("可以写文章。")
		// .outputKey("output")
		// .build();
		//
		// try {
		// // Start streaming and consume chunks as they arrive
		// AsyncGenerator<NodeOutput> generator =
		// currencyExchangeAgent.stream(Map.of("input", "你好，给我写个100字的散文"));
		// int chunkCount = 0;
		// while (true) {
		// AsyncGenerator.Data<NodeOutput> data = generator.next();
		// if (data.isDone()) {
		// System.out.println("Streaming completed. Total chunks: " + chunkCount);
		// break;
		// }
		// NodeOutput outBase = data.getData().join();
		// chunkCount++;
		// if (outBase instanceof StreamingOutput so) {
		// System.out.println("chunk[" + chunkCount + "]: " + so.chunk());
		// }
		// else {
		// System.out.println("chunk[" + chunkCount + "]: " + outBase.toString());
		// }
		// }
		// }
		// catch (java.util.concurrent.CompletionException e) {
		// e.printStackTrace();
		// }
		//
		// try {
		// Optional<OverAllState> result = currencyExchangeAgent.invoke(Map.of("input",
		// "你好，给我写一个100字的描写西湖的文章"));
		// System.out.println("Final Result: " + result.get());
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

}
