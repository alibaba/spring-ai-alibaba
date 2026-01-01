/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.agent.tools.ReviewerTool;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ContextEditingTest {

	private ChatModel chatModel;

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

	}

	@Test
	public void testContextEditingCompaction() throws Exception {
		
		ContextEditingInterceptor contextEditingInterceptor = ContextEditingInterceptor.builder()
				.trigger(180)
				.keep(1)
				.clearAtLeast(100)
				.build();
		PoetTool poetTool = new PoetTool();
		ReviewerTool reviewerTool = new ReviewerTool();

		ReactAgent agent =
				ReactAgent.builder()
						.name("single_agent")
						.model(chatModel)
						.tools(PoetTool.createPoetToolCallback("poem", poetTool), ReviewerTool.createReviewerToolCallback("reviewer", reviewerTool))
						.interceptors(contextEditingInterceptor)
						.saver(new MemorySaver())
						.build();

		try {
			Optional<OverAllState> result = agent.invoke("帮我写一篇100字左右散文。"
					+ "工具调用要求：先调用 `poem` 工具写一首诗，然后调用 `reviewer` 工具对诗进行润色，再次调用 `poem` 工具重新改写诗，最后调用 `reviewer` 工具，输出最终诗词。");

			assertEquals(2, poetTool.count, "Poet tool should be called twice");
			assertEquals(2, reviewerTool.count, "Reviewer tool should be called twice");

			assertTrue(result.isPresent(), "Agent result should be present");

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

}
