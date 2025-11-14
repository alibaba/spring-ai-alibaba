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
package com.alibaba.cloud.ai.graph.agent.model;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DEEPSEEK_API_KEY", matches = ".+")
class ReactAgentDeepSeekTest {

	private ChatModel chatModel;

	// Inner class for outputType example
	public static class PoemOutput {
		private String title;
		private String content;
		private String style;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getStyle() {
			return style;
		}

		public void setStyle(String style) {
			this.style = style;
		}

		@Override
		public String toString() {
			return "PoemOutput{" +
					"title='" + title + '\'' +
					", content='" + content + '\'' +
					", style='" + style + '\'' +
					'}';
		}
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DeepSeekApi deepSeekApi = DeepSeekApi.builder()
			.apiKey(System.getenv("AI_DEEPSEEK_API_KEY"))
//			.baseUrl(System.getenv("AI_DEEPSEEK_API_BASE_URL"))
			.build();

		DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
				.defaultOptions(DeepSeekChatOptions.builder()
						.model("deepseek-chat")
						.build())
				.deepSeekApi(deepSeekApi).build();

		// Create DashScope ChatModel instance
		this.chatModel = deepSeekChatModel;
	}

	@Test
	public void testReactAgent() throws Exception {
		CompileConfig compileConfig = getCompileConfig();
		ReactAgent agent = ReactAgent.builder().name("single_agent").model(chatModel).saver(new MemorySaver()).build();

		try {
			Optional<OverAllState> result = agent.invoke("帮我写一篇100字左右散文。");
			Optional<OverAllState> result2 = agent.invoke(new UserMessage("帮我写一首现代诗歌。"));
			Optional<OverAllState> result3 = agent.invoke("帮我写一首现代诗歌2。");

			assertTrue(result.isPresent(), "First result should be present");
			OverAllState state1 = result.get();
			assertTrue(state1.value("messages").isPresent(), "Messages should be present in first result");
			assertEquals(2, ((List)state1.value("messages").get()).size(), "There should be 2 messages in the first result");
			Object messages1 = state1.value("messages").get();
			assertNotNull(messages1, "Messages should not be null in first result");

			assertTrue(result2.isPresent(), "Second result should be present");
			OverAllState state2 = result2.get();
			assertTrue(state2.value("messages").isPresent(), "Messages should be present in second result");
			assertEquals(4, ((List<?>)state2.value("messages").get()).size(), "There should be 2 messages in the first result");
			Object messages2 = state2.value("messages").get();
			assertNotNull(messages2, "Messages should not be null in second result");

			assertNotEquals(messages1, messages2, "Results should be different for different inputs");

			System.out.println(result.get());

		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testReactAgentMessage() throws Exception {
		CompileConfig compileConfig = getCompileConfig();
		ReactAgent agent = ReactAgent.builder().name("single_agent").model(chatModel).saver(new MemorySaver())
				.build();
		AssistantMessage message = agent.call("帮我写一篇100字左右散文。");
		System.out.println(message.getText());
	}

	@Test
	public void testReactAgentWithOutputSchema() throws Exception {
		CompileConfig compileConfig = getCompileConfig();

		// Customized outputSchema
		String customSchema = """
				请按照以下JSON格式输出：
				{
					"title": "诗歌标题",
					"content": "诗歌正文内容",
					"style": "诗歌风格（如：现代诗、古体诗等）"
				}
				""";

		ReactAgent agent = ReactAgent.builder()
				.name("schema_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputSchema(customSchema)
				.build();

		AssistantMessage message = agent.call("帮我写一首关于春天的诗歌。");
		assertNotNull(message, "Message should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		System.out.println("=== Output with custom schema ===");
		System.out.println(message.getText());

		assertTrue(message.getText().contains("title") || message.getText().contains("标题"),
				"Output should contain title field");
	}

	@Test
	public void testReactAgentWithOutputType() throws Exception {
		CompileConfig compileConfig = getCompileConfig();

		// outputType will be automatically convert to schema
		ReactAgent agent = ReactAgent.builder()
				.name("type_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputType(PoemOutput.class)
				.build();

		AssistantMessage message = agent.call("帮我写一首关于秋天的现代诗。");
		assertNotNull(message, "Message should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		System.out.println("=== Output with outputType (auto-generated schema) ===");
		System.out.println(message.getText());

		assertTrue(message.getText().contains("title") || message.getText().contains("content") ||
				message.getText().contains("style"),
				"Output should contain structured fields");
	}

	@Test
	public void testReactAgentWithOutputSchemaAndInvoke() throws Exception {
		CompileConfig compileConfig = getCompileConfig();

		String jsonSchema = """
				请严格按照以下JSON格式返回结果：
				{
					"summary": "内容摘要",
					"keywords": ["关键词1", "关键词2", "关键词3"],
					"sentiment": "情感倾向（正面/负面/中性）"
				}
				""";

		ReactAgent agent = ReactAgent.builder()
				.name("analysis_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputSchema(jsonSchema)
				.build();

		Optional<OverAllState> result = agent.invoke("分析这句话：春天来了，万物复苏，生机勃勃。");

		assertTrue(result.isPresent(), "Result should be present");
		System.out.println("=== Full state output ===");
		System.out.println(result.get());
	}

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

}
