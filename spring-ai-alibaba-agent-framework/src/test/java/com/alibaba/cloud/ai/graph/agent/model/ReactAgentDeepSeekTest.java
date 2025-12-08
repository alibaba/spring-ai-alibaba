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
import com.alibaba.cloud.ai.graph.agent.AgentTool;
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

		ReactAgent agent = ReactAgent.builder().name("single_agent").model(chatModel).saver(new MemorySaver())
				.build();
		AssistantMessage message = agent.call("帮我写一篇100字左右散文。");
		System.out.println(message.getText());
	}

	@Test
	public void testReactAgentWithOutputSchema() throws Exception {


		// Customized outputSchema
		String customSchema = """
				{
					"$schema": "https://json-schema.org/draft/2020-12/schema",
					"type": "object",
					"properties": {
						"title": {
							"type": "string"
						},
						"content": {
							"type": "string"
						},
						"style": {
							"type": "string"
						}
					},
					"additionalProperties": false
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


		String jsonSchema = """
				{
					"$schema": "https://json-schema.org/draft/2020-12/schema",
					"type": "object",
					"properties": {
						"summary": {
							"type": "string"
						},
						"keywords": {
							"type": "array",
							"items": {
								"type": "string"
							}
						},
						"sentiment": {
							"type": "string"
						}
					},
					"additionalProperties": false
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

	@Test
	public void testAgentToolBasic() throws Exception {


		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("可以写文章。")
				.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
				.saver(new MemorySaver())
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("可以对文章进行评论和修改。")
				.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
				.saver(new MemorySaver())
				.build();

		ReactAgent blogAgent = ReactAgent.builder()
				.name("blog_agent")
				.model(chatModel)
				.instruction("首先，根据用户给定的主题写一篇文章，然后将文章交给评论员进行审核，必要时做出修改。")
				.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent),
						AgentTool.getFunctionToolCallback(reviewerAgent)))
				.saver(new MemorySaver())
				.build();

		try {
			Optional<OverAllState> result = blogAgent
					.invoke(new UserMessage("帮我写一个100字左右的散文"));

			assertTrue(result.isPresent(), "Result should be present");

			OverAllState state = result.get();

			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			Object messages = state.value("messages").get();
			assertNotNull(messages, "Messages should not be null");

			System.out.println("=== Basic Agent Tool Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("Agent tool execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testAgentToolWithInputSchema() throws Exception {


		// 使用 inputSchema 定义工具的输入格式
		String writerInputSchema = """
				{
					"type": "object",
					"properties": {
						"topic": {
							"type": "string"
						},
						"wordCount": {
							"type": "integer"
						},
						"style": {
							"type": "string"
						}
					},
					"required": ["topic", "wordCount", "style"]
				}
				""";

		MemorySaver saver = new MemorySaver();
		ReactAgent writerAgent = ReactAgent.builder()
				.name("structured_writer_agent")
				.model(chatModel)
				.description("根据结构化输入写文章")
				.instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章。")
				.inputSchema(writerInputSchema)
				.saver(saver)
				.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
				.name("coordinator_agent")
				.model(chatModel)
				.instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
				.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
				.saver(saver)
				.build();

		try {
			Optional<OverAllState> result = coordinatorAgent
					.invoke("请写一篇关于春天的抒情散文，大约150字，直接写作不要再询问我了");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with InputSchema Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with inputSchema execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testAgentToolWithOutputSchema() throws Exception {


		// 使用 outputSchema 定义工具的输出格式
		String writerOutputSchema = """
				{
					"type": "object",
					"properties": {
						"title": {
							"type": "string"
						},
						"content": {
							"type": "string"
						},
						"characterCount": {
							"type": "integer"
						}
					}
				}
				""";

		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_with_output_schema")
				.model(chatModel)
				.description("写文章并返回结构化输出")
				.instruction("你是一个专业作家。请创作文章并严格按照指定的JSON格式返回结果。")
				.outputSchema(writerOutputSchema)
				.saver(new MemorySaver())
				.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
				.name("coordinator_output_schema")
				.model(chatModel)
				.instruction("调用写作工具完成用户请求，工具会返回结构化的文章数据。")
				.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
				.saver(new MemorySaver())
				.build();

		try {
			Optional<OverAllState> result = coordinatorAgent
					.invoke("写一篇关于冬天的短文");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with OutputSchema Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with outputSchema execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testAgentToolWithOutputType() throws Exception {


		// 使用 outputType，框架会自动生成输出 schema
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_with_output_type")
				.model(chatModel)
				.description("写文章并返回类型化输出")
				.instruction("你是一个专业作家。请创作文章并返回包含 title、content 和 style 的结构化结果。")
				.outputType(PoemOutput.class)
				.saver(new MemorySaver())
				.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
				.name("coordinator_output_type")
				.model(chatModel)
				.instruction("调用写作工具完成用户请求。")
				.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
				.saver(new MemorySaver())
				.build();

		try {
			Optional<OverAllState> result = coordinatorAgent
					.invoke("写一篇关于夏天的小诗");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with OutputType Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with outputType execution failed: " + e.getMessage());
		}
	}

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(new MemorySaver())
				.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
		return compileConfig;
	}

}
