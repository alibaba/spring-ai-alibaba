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
import com.alibaba.cloud.ai.graph.OverAllState;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class AgentToolTest {

	private ChatModel chatModel;

	// Input type for testing inputType parameter
	public static class ArticleRequest {
		private String topic;
		private int wordCount;
		private String style;

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		public int getWordCount() {
			return wordCount;
		}

		public void setWordCount(int wordCount) {
			this.wordCount = wordCount;
		}

		public String getStyle() {
			return style;
		}

		public void setStyle(String style) {
			this.style = style;
		}
	}

	// Output type for testing outputType parameter
	public static class ArticleOutput {
		private String title;
		private String content;
		private int characterCount;

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

		public int getCharacterCount() {
			return characterCount;
		}

		public void setCharacterCount(int characterCount) {
			this.characterCount = characterCount;
		}
	}

	// Review output type
	public static class ReviewOutput {
		private String comment;
		private boolean approved;
		private List<String> suggestions;

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public boolean isApproved() {
			return approved;
		}

		public void setApproved(boolean approved) {
			this.approved = approved;
		}

		public List<String> getSuggestions() {
			return suggestions;
		}

		public void setSuggestions(List<String> suggestions) {
			this.suggestions = suggestions;
		}
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testAgentToolBasic() throws Exception {
		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章�?)
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答�?)
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("可以对文章进行评论和修改�?)
			.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述�?)
			.build();

		ReactAgent blogAgent = ReactAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.instruction("首先，根据用户给定的主题写一篇文章，然后将文章交给评论员进行审核，必要时做出修改�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent),
					AgentTool.getFunctionToolCallback(reviewerAgent)))
			.build();

		try {
			Optional<OverAllState> result = blogAgent
				.invoke(new UserMessage("帮我写一�?00字左右的散文"));

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
		// 使用 inputSchema 定义工具的输入格�?
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

		ReactAgent writerAgent = ReactAgent.builder()
			.name("structured_writer_agent")
			.model(chatModel)
			.description("根据结构化输入写文章")
			.instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章�?)
			.inputSchema(writerInputSchema)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_agent")
			.model(chatModel)
			.instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
			.build();

		try {
			Optional<OverAllState> result = coordinatorAgent
				.invoke("请写一篇关于春天的散文，大�?50�?);

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
	public void testAgentToolWithInputType() throws Exception {
		// 使用 inputType，框架会自动生成 JSON Schema
		ReactAgent writerAgent = ReactAgent.builder()
			.name("typed_writer_agent")
			.model(chatModel)
			.description("根据类型化输入写文章")
			.instruction("你是一个专业作家。请严格按照输入�?topic（主题）、wordCount（字数）�?style（风格）要求创作文章�?)
			.inputType(ArticleRequest.class)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_with_type_agent")
			.model(chatModel)
			.instruction("你需要调用写作工具来完成用户的写作请求。工具接�?JSON 格式的参数�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
			.build();

		try {
			Optional<OverAllState> result = coordinatorAgent
				.invoke("请写一篇关于秋天的现代诗，大约100�?);

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with InputType Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with inputType execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testAgentToolWithOutputSchema() throws Exception {
		// 使用 outputSchema 定义工具的输出格�?
		String writerOutputSchema = """
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
						"characterCount": {
							"type": "integer"
						}
					},
					"additionalProperties": false
				}
				""";

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_with_output_schema")
			.model(chatModel)
			.description("写文章并返回结构化输�?)
			.instruction("你是一个专业作家。请创作文章并严格按照指定的JSON格式返回结果�?)
			.outputSchema(writerOutputSchema)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_output_schema")
			.model(chatModel)
			.instruction("调用写作工具完成用户请求，工具会返回结构化的文章数据�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
			.outputType(ArticleOutput.class)
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
			.description("写文章并返回类型化输�?)
			.instruction("你是一个专业作家。请创作文章并返回包�?title、content �?characterCount 的结构化结果�?)
			.outputType(ArticleOutput.class)
			.build();

		ReactAgent coordinatorAgent = ReactAgent.builder()
			.name("coordinator_output_type")
			.model(chatModel)
			.instruction("调用写作工具完成用户请求�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(writerAgent)))
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

	@Test
	public void testAgentToolWithAllSchemaTypes() throws Exception {
		// 综合测试：同时使�?inputType �?outputType
		ReactAgent writerAgent = ReactAgent.builder()
			.name("full_typed_writer")
			.model(chatModel)
			.description("完整类型化的写作工具")
			.instruction("根据结构化输入（topic、wordCount、style）创作文章，并返回结构化输出（title、content、characterCount）�?)
			.inputType(ArticleRequest.class)
			.outputType(ArticleOutput.class)
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("typed_reviewer")
			.model(chatModel)
			.description("完整类型化的评审工具")
			.instruction("对文章进行评审，返回评审意见（comment、approved、suggestions）�?)
			.outputType(ReviewOutput.class)
			.build();

		ReactAgent orchestratorAgent = ReactAgent.builder()
			.name("orchestrator")
			.model(chatModel)
			.instruction("协调写作和评审流程。先调用写作工具创作文章，然后调用评审工具进行评审�?)
			.tools(List.of(
					AgentTool.getFunctionToolCallback(writerAgent),
					AgentTool.getFunctionToolCallback(reviewerAgent)))
			.build();

		try {
			Optional<OverAllState> result = orchestratorAgent
				.invoke("请写一篇关于友谊的散文，约200字，需要评�?);

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with All Schema Types Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with all schema types execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testAgentToolWithMixedSchemas() throws Exception {
		// 混合使用：inputSchema + outputType
		String customInputSchema = """
				{
					"type": "object",
					"properties": {
						"articleText": {
							"type": "string"
						},
						"criteria": {
							"type": "string"
						}
					},
					"required": ["articleText", "criteria"]
				}
				""";

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("mixed_schema_reviewer")
			.model(chatModel)
			.description("使用混合 schema 的评审工�?)
			.instruction("根据给定的文章内容和评审标准进行评审，返回结构化的评审结果�?)
			.inputSchema(customInputSchema)
			.outputType(ReviewOutput.class)
			.build();

		ReactAgent mainAgent = ReactAgent.builder()
			.name("main_agent")
			.model(chatModel)
			.instruction("使用评审工具对用户提供的内容进行评审�?)
			.tools(List.of(AgentTool.getFunctionToolCallback(reviewerAgent)))
			.build();

		try {
			Optional<OverAllState> result = mainAgent
				.invoke("请评审这段话：春天来了，花儿开了。评审标准：文采和表达力");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("=== Agent Tool with Mixed Schemas Test ===");
			System.out.println(result.get());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Agent tool with mixed schemas execution failed: " + e.getMessage());
		}
	}
}
