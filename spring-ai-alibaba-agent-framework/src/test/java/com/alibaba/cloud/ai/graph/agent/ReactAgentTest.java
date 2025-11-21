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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ReactAgentTest {

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

	// Inner class for BeanOutputConverter example
	public static class ActorsFilms {
		private String actor;
		private List<String> films;

		public String getActor() {
			return actor;
		}

		public void setActor(String actor) {
			this.actor = actor;
		}

		public List<String> getFilms() {
			return films;
		}

		public void setFilms(List<String> films) {
			this.films = films;
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
				.enableLogging(true)
				.build();

		Optional<OverAllState> result = agent.invoke("分析这句话：春天来了，万物复苏，生机勃勃。");

		assertTrue(result.isPresent(), "Result should be present");
		System.out.println("=== Full state output ===");
		System.out.println(result.get());
	}

	@Test
	public void testAgentNameAndTokenUsage() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("test_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		Optional<NodeOutput> nodeOutputOptional = agent.invokeAndGetOutput("帮我写一篇100字左右散文。");

		assertTrue(nodeOutputOptional.isPresent(), "Result should be present");

		NodeOutput nodeOutput = nodeOutputOptional.get();
		assertNotNull(nodeOutput, "NodeOutput should not be null");
		assertNotNull(nodeOutput.tokenUsage(), "TokenUsage should not be null");
		assertNotNull(nodeOutput.agent(), "Agent should not be null");
		assertEquals("test_agent", nodeOutput.agent(), "Agent name should match");

		System.out.println("=== NodeOutput ===");
		System.out.println("Agent: " + nodeOutput.agent());
		System.out.println("TokenUsage: " + nodeOutput.tokenUsage());
	}

	@Test
	public void testAgentNameAndTokenUsage2() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("test_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		Flux<NodeOutput> flux = agent.stream(new UserMessage("帮我写一篇100字左右散文。"));

		flux.doOnNext(output -> {
			if (output instanceof StreamingOutput<?> streamingOutput){
				assertNotNull(streamingOutput, "NodeOutput should not be null");
				assertNotNull(streamingOutput.tokenUsage(), "TokenUsage should not be null");
				assertNotNull(streamingOutput.agent(), "Agent should not be null");
				assertEquals("test_agent", streamingOutput.agent(), "Agent name should match");

				System.out.println("=== NodeOutput ===");
				System.out.println("Agent: " + streamingOutput.agent());
				System.out.println("TokenUsage: " + streamingOutput.tokenUsage());
			}
		}).blockLast();
	}

	@Test
	public void testAgentSystemPrompt() throws Exception {
		ReactAgent agent = ReactAgent.builder()
				.name("test_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.systemPrompt("你是一个诗歌写作助理，你能帮我写一首关于春天的现代诗。")
				.enableLogging(true)
				.build();

		AssistantMessage assistantMessage = agent.call("帮我写一首关于春天的现代诗。");
		System.out.println(assistantMessage.getText());
	}

	@Test
	public void testReactAgentWithBeanOutputConverter() throws Exception {
		// Use BeanOutputConverter to generate outputSchema
		BeanOutputConverter<List<ActorsFilms>> outputConverter = new BeanOutputConverter<>(
				new ParameterizedTypeReference<List<ActorsFilms>>() { });

		String format = outputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("actors_films_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputSchema(format)
				.enableLogging(true)
				.build();

		AssistantMessage message = agent.call("列出3位知名演员及其代表作品，每位演员列出2-3部电影。");
		assertNotNull(message, "Message should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		System.out.println("=== Output with BeanOutputConverter generated schema ===");
		System.out.println(message.getText());

		assertTrue(message.getText().contains("actor") || message.getText().contains("films"),
				"Output should contain actor or films field");
	}

	@Test
	public void testReactAgentWithMapOutputConverter() throws Exception {
		// Use MapOutputConverter to generate outputSchema
		MapOutputConverter mapOutputConverter = new MapOutputConverter();
		String format = mapOutputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("map_output_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputSchema(format)
				.enableLogging(true)
				.build();

		AssistantMessage message = agent.call("请提供一个包含姓名、年龄和职业的JSON对象。");
		assertNotNull(message, "Message should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		System.out.println("=== Output with MapOutputConverter generated schema ===");
		System.out.println(message.getText());

		assertTrue(message.getText().length() > 0, "Output should not be empty");
	}

	@Test
	public void testReactAgentWithListOutputConverter() throws Exception {
		// Use ListOutputConverter to generate outputSchema
		ListOutputConverter listOutputConverter = new ListOutputConverter(new DefaultConversionService());
		String format = listOutputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("list_output_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.outputSchema(format)
				.enableLogging(true)
				.build();

		AssistantMessage message = agent.call("请列出5个你最喜欢的编程语言。");
		assertNotNull(message, "Message should not be null");
		assertNotNull(message.getText(), "Message text should not be null");
		System.out.println("=== Output with ListOutputConverter generated schema ===");
		System.out.println(message.getText());

		assertTrue(message.getText().length() > 0, "Output should not be empty");
	}

}
