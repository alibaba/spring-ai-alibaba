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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the tool-call based structured output strategy of {@link ReactAgent}.
 */
class ReactAgentStructuredOutputTest {

	private static final String POEM_JSON = "{\"title\":\"Spring\",\"content\":\"A poem\",\"style\":\"modern\"}";

	@Test
	void outputTypeUsesStructuredOutputToolAndExposesParsedState() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(POEM_JSON));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();

		Optional<OverAllState> result = agent.invoke("Write a poem about spring");

		assertTrue(result.isPresent());
		OverAllState state = result.get();

		Object structured = state.value("structured_output").orElseThrow();
		PoemOutput poem = assertInstanceOf(PoemOutput.class, structured);
		assertEquals("Spring", poem.getTitle());
		assertEquals("A poem", poem.getContent());
		assertEquals("modern", poem.getStyle());

		AssistantMessage lastMessage = lastAssistantMessage(state);
		assertEquals(POEM_JSON, lastMessage.getText());
		assertFalse(lastMessage.hasToolCalls(), "The internal tool call must not be routed to the tool node");

		assertTrue(requestedToolNames(model.prompts.get(0)).contains("structured_output"));
	}

	@Test
	void structuredOutputToolArgumentsBecomeAssistantTextForCall() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(POEM_JSON));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();

		AssistantMessage message = agent.call("Write a poem about spring");

		assertEquals(POEM_JSON, message.getText());
	}

	@Test
	void outputSchemaFallsBackToJsonTextWhenModelDoesNotCallTool() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(
				new AssistantMessage("```json\n" + POEM_JSON + "\n```"));
		String schema = """
				{
					"type": "object",
					"properties": {
						"title": {"type": "string"},
						"content": {"type": "string"},
						"style": {"type": "string"}
					},
					"required": ["title", "content", "style"],
					"additionalProperties": false
				}
				""";
		ReactAgent agent = ReactAgent.builder()
			.name("schema_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputSchema(schema)
			.build();

		OverAllState state = agent.invoke("Write a poem about spring").orElseThrow();

		Object structured = state.value("structured_output").orElseThrow();
		Map<?, ?> parsed = assertInstanceOf(Map.class, structured);
		assertEquals("Spring", parsed.get("title"));
		assertTrue(requestedToolNames(model.prompts.get(0)).contains("structured_output"));
	}

	@Test
	void promptInstructionsKeepThePromptBasedBehavior() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(new AssistantMessage("0.5"));
		String instruction = "Only return one float between 0 and 1.";
		ReactAgent agent = ReactAgent.builder()
			.name("score_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputSchema(instruction)
			.build();

		OverAllState state = agent.invoke("Score this sentence").orElseThrow();

		assertFalse(state.value("structured_output").isPresent());
		assertFalse(requestedToolNames(model.prompts.get(0)).contains("structured_output"));
		assertTrue(userText(model.prompts.get(0)).contains(instruction));
	}

	@Test
	void nonObjectOutputTypeKeepsThePromptBasedBehavior() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(new AssistantMessage("0.5"));
		ReactAgent agent = ReactAgent.builder()
			.name("score_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(Double.class)
			.build();

		OverAllState state = agent.invoke("Score this sentence").orElseThrow();

		assertFalse(state.value("structured_output").isPresent());
		assertFalse(requestedToolNames(model.prompts.get(0)).contains("structured_output"));
		assertTrue(userText(model.prompts.get(0)).contains("JSON"));
	}

	@Test
	void malformedStructuredToolCallStillFinishesTheLoop() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage("not-json"));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();

		OverAllState state = agent.invoke("Write a poem about spring").orElseThrow();

		assertFalse(state.value("structured_output").isPresent());
		assertEquals("not-json", lastAssistantMessage(state).getText());
		assertEquals(1, model.prompts.size(), "A malformed structured tool call must not be routed to the tool node");
	}

	@Test
	void structuredOutputToolKeepsTheReActLoopWorking() throws Exception {
		AssistantMessage mixedResponse = AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(
					new AssistantMessage.ToolCall("call-1", "function", "echo", "{\"text\":\"hello\"}"),
					new AssistantMessage.ToolCall("call-structured", "function", "structured_output", POEM_JSON)))
			.build();
		ScriptedChatModel model = new ScriptedChatModel(mixedResponse, structuredOutputMessage(POEM_JSON));
		EchoTool echoTool = new EchoTool();

		ReactAgent agent = ReactAgent.builder()
			.name("react_agent")
			.model(model)
			.saver(new MemorySaver())
			.tools(ToolCallbacks.from(echoTool))
			.outputType(PoemOutput.class)
			.build();

		OverAllState state = agent.invoke("Echo hello and write a poem").orElseThrow();

		assertInstanceOf(PoemOutput.class, state.value("structured_output").orElseThrow());
		assertEquals(1, echoTool.getCallCount(), "The ordinary tool call must still be executed");
		assertEquals(2, model.prompts.size(), "The model should be called again after the user tool ran");
		assertEquals(POEM_JSON, lastAssistantMessage(state).getText());
	}

	@Test
	void structuredOutputIsStoredInTheCheckpointedState() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(POEM_JSON));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();
		RunnableConfig config = RunnableConfig.builder().threadId("structured-output-thread").build();

		agent.call("Write a poem about spring", config);

		StateSnapshot snapshot = agent.getCurrentState(config);
		assertInstanceOf(PoemOutput.class, snapshot.state().value("structured_output").orElseThrow());
	}

	@Test
	void structuredOutputToolIsVisibleToModelInterceptors() throws Exception {
		CapturingModelInterceptor interceptor = new CapturingModelInterceptor();
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(POEM_JSON));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.interceptors(interceptor)
			.outputType(PoemOutput.class)
			.build();

		agent.call("Write a poem about spring");

		assertTrue(interceptor.request.getTools().contains("structured_output"));
		assertTrue(interceptor.request.getOptions()
			.getToolCallbacks()
			.stream()
			.anyMatch(callback -> "structured_output".equals(callback.getToolDefinition().name())));
	}

	@Test
	void resumeKeepsTheSavedStructuredOutput() throws Exception {
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(POEM_JSON));
		ReactAgent agent = ReactAgent.builder()
			.name("poem_agent")
			.model(model)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();
		RunnableConfig config = RunnableConfig.builder().threadId("structured-output-resume-thread").build();

		agent.invoke("Write a poem about spring", config).orElseThrow();
		PoemOutput saved = assertInstanceOf(PoemOutput.class,
				agent.getCurrentState(config).state().value("structured_output").orElseThrow());

		OverAllState resumed = agent.invoke("", config.withResume()).orElseThrow();

		PoemOutput resumedPoem = assertInstanceOf(PoemOutput.class,
				resumed.value("structured_output").orElseThrow());
		assertEquals(saved.getTitle(), resumedPoem.getTitle());
	}

	@Test
	void modelFailureDoesNotProduceStructuredOutput() throws Exception {
		ChatModel failingModel = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new IllegalStateException("HTTP 429: {\"error\":\"rate limited\"}");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.error(new IllegalStateException("HTTP 429: {\"error\":\"rate limited\"}"));
			}
		};
		ReactAgent agent = ReactAgent.builder()
			.name("failing_agent")
			.model(failingModel)
			.saver(new MemorySaver())
			.outputType(PoemOutput.class)
			.build();

		OverAllState state = agent.invoke("Write a poem about spring").orElseThrow();

		assertFalse(state.value("structured_output").isPresent());
		assertTrue(lastAssistantMessage(state).getText().startsWith("Exception: "));
	}

	@Test
	void structuredOutputUsesTheRedactedAssistantMessage() throws Exception {
		String json = "{\"email\":\"alice@example.com\"}";
		String schema = """
				{
					"type": "object",
					"properties": {
						"email": {"type": "string"}
					},
					"required": ["email"],
					"additionalProperties": false
				}
				""";
		ScriptedChatModel model = new ScriptedChatModel(structuredOutputMessage(json));
		PIIDetectionHook piiHook = PIIDetectionHook.builder()
			.piiType(PIIType.EMAIL)
			.strategy(RedactionStrategy.REDACT)
			.applyToOutput(true)
			.build();
		ReactAgent agent = ReactAgent.builder()
			.name("pii_agent")
			.model(model)
			.saver(new MemorySaver())
			.hooks(piiHook)
			.outputSchema(schema)
			.build();

		OverAllState state = agent.invoke("Extract the email").orElseThrow();

		Map<?, ?> structured = assertInstanceOf(Map.class, state.value("structured_output").orElseThrow());
		assertEquals("[REDACTED_EMAIL]", structured.get("email"));
		assertEquals("{\"email\":\"[REDACTED_EMAIL]\"}", lastAssistantMessage(state).getText());
	}

	private static AssistantMessage structuredOutputMessage(String json) {
		return AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-structured", "function", "structured_output", json)))
			.build();
	}

	private static AssistantMessage lastAssistantMessage(OverAllState state) {
		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof AssistantMessage assistantMessage) {
				return assistantMessage;
			}
		}
		throw new AssertionError("No AssistantMessage in state");
	}

	private static List<String> requestedToolNames(Prompt prompt) {
		if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
			return options.getToolCallbacks().stream().map(callback -> callback.getToolDefinition().name()).toList();
		}
		return List.of();
	}

	private static String userText(Prompt prompt) {
		for (Message message : prompt.getInstructions()) {
			if (message instanceof org.springframework.ai.chat.messages.UserMessage userMessage) {
				return userMessage.getText();
			}
		}
		return "";
	}

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

	}

	public static class EchoTool {

		private int callCount;

		@Tool(description = "Echo the given text")
		public String echo(@ToolParam(description = "Text to echo") String text) {
			callCount++;
			return text;
		}

		public int getCallCount() {
			return callCount;
		}

	}

	static class CapturingModelInterceptor extends ModelInterceptor {

		private ModelRequest request;

		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			this.request = request;
			return handler.call(request);
		}

		@Override
		public String getName() {
			return getClass().getSimpleName();
		}

	}

	/**
	 * Chat model that replays a fixed list of assistant messages and records the prompts it
	 * receives.
	 */
	static class ScriptedChatModel implements ChatModel {

		private final List<AssistantMessage> responses;

		private final List<Prompt> prompts = new ArrayList<>();

		private int index;

		ScriptedChatModel(AssistantMessage... responses) {
			this.responses = List.of(responses);
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			AssistantMessage message = responses.get(Math.min(index++, responses.size() - 1));
			return new ChatResponse(List.of(new Generation(message)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

	}

}
