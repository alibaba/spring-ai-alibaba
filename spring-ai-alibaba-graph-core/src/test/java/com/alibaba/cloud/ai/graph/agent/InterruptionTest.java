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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.agent.PoemTool.createToolCallback;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class InterruptionTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testReactAgent() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("single_agent")
				.model(chatModel)
				.compileConfig(getCompileConfig())
				.tools(List.of(createToolCallback()))
				.hooks(List.of(HumanInTheLoopHook.builder().approvalOn("poem", ToolConfig.builder().description("Please confirm tool execution.").build()).build()))
				.outputKey("article")
				.build();

		GraphRepresentation representation = agent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());

		try {
			RunnableConfig runnableConfig = RunnableConfig.builder().threadId("123").build();
			Optional<OverAllState> result = agent.invoke("帮我写一篇100字左右散文。", runnableConfig);
			System.out.println(result.get());

			Object obj = result.map(state -> state.value("article")).orElseThrow();
			if (obj instanceof InterruptionMetadata interruptionMetadata) {
				System.out.println("interruption metadata: " + interruptionMetadata);
			}

			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) obj;
			InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder();
			interruptionMetadata.getToolFeedbacks().forEach(toolFeedback -> {
				newBuilder.addToolFeedback(InterruptionMetadata.ToolFeedback.builder(toolFeedback).result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED).build());
			});

			RunnableConfig resumeRunnableConfig = RunnableConfig.builder().threadId("123")
					.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, newBuilder.build())
					.build();
			result = agent.invoke("", resumeRunnableConfig);
			System.out.println(result.get());
		} catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}
}
