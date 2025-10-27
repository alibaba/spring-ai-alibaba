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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback.ModelFallbackInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ModelfallbackTest {

	private ChatModelCallCounter chatModel;
	private ChatModelCallCounter fallbackModel;

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

	@BeforeEach
	void setUp() {
		OpenAiApi openAiApi = OpenAiApi.builder().apiKey(System.getenv("OPENAI_API_KEY")).build();
		ChatModel openAiChatModel = OpenAiChatModel.builder().openAiApi(openAiApi).build();
		this.chatModel = new ChatModelCallCounter(openAiChatModel, "OpenAI");

		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		ChatModel dashScopeChatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
		this.fallbackModel = new ChatModelCallCounter(dashScopeChatModel, "DashScope");

	}

	@Test
	public void testReactAgent() throws Exception {
		CompileConfig compileConfig = getCompileConfig();
		ModelFallbackInterceptor modelFallbackInterceptor = ModelFallbackInterceptor
				.builder()
				.addFallbackModel(fallbackModel)
				.build();

		ReactAgent agent =
				ReactAgent.builder()
						.name("single_agent")
						.model(chatModel)
						.interceptors(modelFallbackInterceptor)
						.compileConfig(compileConfig)
						.build();

		try {
			Optional<OverAllState> result = agent.invoke("帮我写一篇100字左右散文。");

			// Assert that the result is present
			assertTrue(result.isPresent(), "Agent result should be present");

			// Get call counts
			int primaryModelCalls = chatModel.getCallCount();
			int fallbackModelCalls = fallbackModel.getCallCount();

			// Log the call counts
			System.out.println("Primary model (OpenAI) call count: " + primaryModelCalls);
			System.out.println("Fallback model (DashScope) call count: " + fallbackModelCalls);

			// Verify the models were called as expected
			assertEquals(1, chatModel.getCallCount());
			assertEquals(1, fallbackModel.getCallCount());

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

}
