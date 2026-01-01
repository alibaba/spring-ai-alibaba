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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;

import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class StreamAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// 先创�?DashScopeApi 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// 创建 DashScope ChatModel 实例
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testStreamLlmRoutingAgent() throws Exception {
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("可以写散文文章�?)
			.instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答�?)
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗�?)
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问进行回答�?)
			.outputKey("poem_article")
			.build();

		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("可以根据用户给定的主题写文章或作诗�?)
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.build();

		try {
			List<NodeOutput> outputs = new ArrayList<>();

			Flux<NodeOutput> result = blogAgent.stream("帮我写一�?00字左右的散文");
			result.doOnNext(nodeOutput -> {
				System.out.println(nodeOutput);
				outputs.add(nodeOutput);
			}).then().block();

			assertFalse(outputs.isEmpty());
			var last = outputs.get(outputs.size() - 1);
			var finalState = last.state();
			assertTrue(finalState.value("prose_article").isPresent());
			assertFalse(finalState.value("poem_article").isPresent());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
