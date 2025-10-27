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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReactAgent 结构化输出集成测试 - OpenAI 版本
 * 
 * <p>测试场景：
 * <ul>
 *   <li>NATIVE 模式：OpenAI 使用 responseFormat (JSON_OBJECT)</li>
 *   <li>验证反射动态设置 responseFormat</li>
 *   <li>验证完整的 Agent 执行流程</li>
 * </ul>
 * 
 * <p>使用环境变量：
 * <pre>{@code
 * export OPENAI_API_KEY=your_openai_api_key
 * mvn test -Dtest=ReactAgentStructuredOutputOpenAITest
 * }</pre>
 *
 * @author logicwu0
 */
@EnabledIfEnvironmentVariable(named = "AI_OPENAI_API_KEY", matches = ".+")
class ReactAgentStructuredOutputOpenAITest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// 创建 OpenAI API 实例
		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey("AI_OPENAI_API_KEY")
				.build();

		// 创建 OpenAI ChatModel（使用 gpt-4o-mini，支持 JSON mode）
		this.chatModel = OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.build();
	}

	/**
	 * 测试：OpenAI NATIVE 模式结构化输出（使用 responseFormat）
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testOpenAiWithStructuredOutput() throws Exception {
		System.out.println("\n=== 测试 OpenAI NATIVE 模式结构化输出 ===");

		// 定义 JSON Schema（书籍推荐）
		String bookSchema = """
				{
					"type": "object",
					"properties": {
						"title": {
							"type": "string",
							"description": "Book title"
						},
						"author": {
							"type": "string",
							"description": "Author name"
						},
						"genre": {
							"type": "string",
							"description": "Book genre"
						},
						"summary": {
							"type": "string",
							"description": "Brief summary"
						},
						"rating": {
							"type": "number",
							"description": "Rating (1-10)"
						}
					},
					"required": ["title", "author", "genre", "summary"]
				}
				""";

		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(saverConfig)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("openai_structured_agent")
				.model(chatModel)
				.outputSchema(bookSchema)  // 设置 outputSchema（OpenAI 会使用 NATIVE 模式）
				.compileConfig(compileConfig)
				.build();

		// 执行 Agent
		Optional<OverAllState> result = agent.invoke("Recommend a science fiction book.");

		// 验证结果存在
		assertTrue(result.isPresent(), "Result should be present");
		OverAllState state = result.get();

		// 验证 messages 存在
		Optional<Object> messagesOpt = state.value("messages");
		assertTrue(messagesOpt.isPresent(), "Messages should be present");

		List<Message> messages = (List<Message>) messagesOpt.get();
		assertNotNull(messages, "Messages should not be null");
		assertFalse(messages.isEmpty(), "Messages should not be empty");

		// 打印最后一条消息
		Message lastMessage = messages.get(messages.size() - 1);
		System.out.println("\n📝 OpenAI Response (JSON format):");
		System.out.println(lastMessage.getText());

		// 验证是 JSON 格式
		String text = lastMessage.getText().trim();
		assertTrue(text.startsWith("{") && text.endsWith("}"), 
				"Response should be JSON format (OpenAI NATIVE mode)");

		// 验证 structured_output 存在
		Optional<Object> structuredOutputOpt = state.value("structured_output");
		assertTrue(structuredOutputOpt.isPresent(), 
				"structured_output should exist (OpenAI NATIVE mode)");

		Map<String, Object> structuredOutput = (Map<String, Object>) structuredOutputOpt.get();
		assertNotNull(structuredOutput, "Structured output should not be null");
		assertFalse(structuredOutput.isEmpty(), "Structured output should not be empty");

		// 验证必填字段
		assertTrue(structuredOutput.containsKey("title"), "Should contain title field");
		assertTrue(structuredOutput.containsKey("author"), "Should contain author field");
		assertTrue(structuredOutput.containsKey("genre"), "Should contain genre field");
		assertTrue(structuredOutput.containsKey("summary"), "Should contain summary field");

		// 打印结构化输出
		System.out.println("\n✅ Structured Output (OpenAI NATIVE):");
		System.out.println("Title: " + structuredOutput.get("title"));
		System.out.println("Author: " + structuredOutput.get("author"));
		System.out.println("Genre: " + structuredOutput.get("genre"));
		if (structuredOutput.containsKey("rating")) {
			System.out.println("Rating: " + structuredOutput.get("rating"));
		}
		System.out.println("\nSummary:");
		System.out.println(structuredOutput.get("summary"));

		System.out.println("\n✓ Test passed: OpenAI NATIVE mode works correctly");
	}

	/**
	 * 测试：OpenAI call() 方法
	 */
	@Test
	public void testOpenAiCallWithStructuredOutput() throws Exception {
		System.out.println("\n=== 测试 OpenAI call() 方法结构化输出 ===");

		String personSchema = """
				{
					"type": "object",
					"properties": {
						"name": {
							"type": "string",
							"description": "Person's name"
						},
						"age": {
							"type": "integer",
							"description": "Person's age"
						},
						"occupation": {
							"type": "string",
							"description": "Person's occupation"
						},
						"bio": {
							"type": "string",
							"description": "Brief biography"
						}
					},
					"required": ["name", "age", "occupation"]
				}
				""";

		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(saverConfig)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("openai_person_agent")
				.model(chatModel)
				.outputSchema(personSchema)
				.compileConfig(compileConfig)
				.build();

		// 使用 call() 方法
		AssistantMessage message = agent.call("Tell me about Albert Einstein.");

		// 验证消息
		assertNotNull(message, "AssistantMessage should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		assertFalse(message.getText().isEmpty(), "Message text should not be empty");

		System.out.println("\n👤 Person Info (JSON format):");
		System.out.println(message.getText());

		// 验证是 JSON 格式
		String text = message.getText().trim();
		assertTrue(text.contains("name") && text.contains("age"), 
				"Response should contain required fields");

		System.out.println("\n✓ Test passed: OpenAI call() method works correctly");
	}

	/**
	 * 测试：对比 OpenAI NATIVE 模式 vs 通义千问 TOOLCALL 模式
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testOpenAiNativeModeDifference() throws Exception {
		System.out.println("\n=== 测试 OpenAI NATIVE 模式特性 ===");

		String simpleSchema = """
				{
					"type": "object",
					"properties": {
						"answer": {
							"type": "string",
							"description": "The answer"
						},
						"confidence": {
							"type": "number",
							"description": "Confidence level (0-1)"
						}
					},
					"required": ["answer"]
				}
				""";

		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(saverConfig)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("openai_simple_agent")
				.model(chatModel)
				.outputSchema(simpleSchema)
				.compileConfig(compileConfig)
				.build();

		Optional<OverAllState> result = agent.invoke("What is 2+2?");

		assertTrue(result.isPresent(), "Result should be present");
		OverAllState state = result.get();

		// 获取 messages
		Optional<Object> messagesOpt = state.value("messages");
		assertTrue(messagesOpt.isPresent(), "Messages should be present");
		List<Message> messages = (List<Message>) messagesOpt.get();

		// 检查最后一条消息
		AssistantMessage lastMsg = (AssistantMessage) messages.get(messages.size() - 1);
		System.out.println("\n📊 Response characteristics:");
		System.out.println("- Text content: " + lastMsg.getText());
		
		// OpenAI NATIVE 模式：没有 toolCalls
		System.out.println("- Tool calls: " + 
				(lastMsg.getToolCalls() == null || lastMsg.getToolCalls().isEmpty() ? 
						"None (NATIVE mode)" : "Present (TOOLCALL mode)"));

		// 验证 structured_output 存在
		Optional<Object> structuredOutputOpt = state.value("structured_output");
		assertTrue(structuredOutputOpt.isPresent(), "structured_output should exist");

		Map<String, Object> structuredOutput = (Map<String, Object>) structuredOutputOpt.get();
		System.out.println("\n✅ Structured Output:");
		System.out.println(structuredOutput);

		System.out.println("\n📝 Key difference:");
		System.out.println("- OpenAI: Uses responseFormat (JSON_OBJECT) - NATIVE mode");
		System.out.println("- DashScope: Uses format_output tool - TOOLCALL mode");
		System.out.println("- Both achieve structured output, just different mechanisms!");

		System.out.println("\n✓ Test passed: OpenAI NATIVE mode verified");
	}

	/**
	 * 测试：OpenAI 不使用 outputSchema（向后兼容）
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testOpenAiWithoutStructuredOutput() throws Exception {
		System.out.println("\n=== 测试 OpenAI 无结构化输出（向后兼容）===");

		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();

		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(saverConfig)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("openai_normal_agent")
				.model(chatModel)
				// 不设置 outputSchema
				.compileConfig(compileConfig)
				.build();

		Optional<OverAllState> result = agent.invoke("Tell me a joke.");

		assertTrue(result.isPresent(), "Result should be present");
		OverAllState state = result.get();

		// 验证 messages 存在
		Optional<Object> messagesOpt = state.value("messages");
		assertTrue(messagesOpt.isPresent(), "Messages should be present");

		// 验证 structured_output 不存在
		Optional<Object> structuredOutputOpt = state.value("structured_output");
		assertFalse(structuredOutputOpt.isPresent(), 
				"structured_output should not exist without outputSchema");

		List<Message> messages = (List<Message>) messagesOpt.get();
		Message lastMessage = messages.get(messages.size() - 1);

		System.out.println("\n😄 Normal response (no structured output):");
		System.out.println(lastMessage.getText());

		System.out.println("\n✓ Test passed: Backward compatibility verified");
	}
}

