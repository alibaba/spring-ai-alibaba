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
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.GuardrailRefusalException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.messages.part.StreamingParts;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.GuardrailRefusal;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionResponse;
import org.springframework.ai.decision.answer.ChoiceAnswer;
import org.springframework.ai.model.tool.ToolCallConfirmationRequiredException;
import org.springframework.ai.model.tool.ToolCallDecision;
import org.springframework.ai.model.tool.ToolCallDecisions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.function.FunctionToolCallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoundationTutorialExamplesTest {

	@Test
	void requiresDecisionsBeforeExecutingAnApprovalBatch() {
		var manager = ToolsExample.toolCallingManager();
		var tools = ToolCallbacks.from(new ToolsExample.ReviewTools());
		var options = ToolCallingChatOptions.builder().toolCallbacks(tools).build();
		var prompt = new Prompt("Prepare two review notes", options);
		var assistant = AssistantMessage.builder().toolCalls(List.of(
				new AssistantMessage.ToolCall("review-1", "function", "prepareReviewNote", "{\"title\":\"alpha\"}"),
				new AssistantMessage.ToolCall("review-2", "function", "prepareReviewNote", "{\"title\":\"beta\"}")))
				.build();
		var reply = new ChatResponse(List.of(new Generation(assistant)));

		assertThrows(ToolCallConfirmationRequiredException.class, () -> manager.executeToolCalls(prompt, reply));

		var result = ToolsExample.resumeApprovedTools(manager, prompt, reply, ToolCallDecisions.of(
				ToolCallDecision.allow("review-1"), ToolCallDecision.deny("review-2", "Skipped by reviewer")));
		var messages = result.conversationHistory();
		var toolResults = assertInstanceOf(ToolResponseMessage.class, messages.get(messages.size() - 1));
		assertEquals(List.of("review-1", "review-2"),
				toolResults.getResponses().stream().map(ToolResponseMessage.ToolResponse::id).toList());
		assertTrue(toolResults.getResponses().get(0).responseData().contains("Prepared review note: alpha"));
		assertTrue(toolResults.getResponses().get(1).responseData().contains("Skipped by reviewer"));
	}

	@Test
	void runsTheChatClientToolLoopWithRequestScopedCallbacks() {
		var requests = new ArrayList<Prompt>();
		var lookup = FunctionToolCallback.builder("lookupOrder", (OrderRequest request) -> "ready-" + request.id())
				.description("Look up an order").inputType(OrderRequest.class).build();
		ChatModel model = new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				requests.add(prompt);
				if (requests.size() == 1) {
					var message = AssistantMessage.builder().toolCalls(List.of(
							new AssistantMessage.ToolCall("order-1", "function", "lookupOrder", "{\"id\":\"42\"}")))
							.build();
					return new ChatResponse(List.of(new Generation(message)));
				}
				return new ChatResponse(List.of(new Generation(new AssistantMessage("Order is ready"))));
			}

			@Override
			public ToolCallingChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}

		};
		var client = ToolsExample.toolClient(model, ToolsExample.toolCallingManager());

		var response = ToolsExample.callWithTools(client, "Check order 42", lookup);

		assertEquals("Order is ready", response.getResult().getOutput().getText());
		var sentTools = assertInstanceOf(ToolCallingChatOptions.class, requests.get(0).getOptions());
		assertEquals(List.of(lookup), sentTools.getToolCallbacks());
		var history = requests.get(1).getInstructions();
		var tools = assertInstanceOf(ToolResponseMessage.class, history.get(history.size() - 1));
		assertTrue(tools.getResponses().get(0).responseData().contains("ready-42"));
	}

	@Test
	void editsTextWithoutDroppingPartsAndKeepsPartialArgumentsOutOfToolCalls() {
		var original = MessagesExample.orderedAssistantMessage();
		var edited = MessagesExample.updateAssistantText(original, "Checking order 42.");
		assertEquals(List.of(ReasoningPart.class, TextPart.class, ToolCallPart.class),
				edited.getParts().stream().map(Object::getClass).toList());
		assertEquals(original.getReasoning(), edited.getReasoning());
		assertEquals(original.getToolCalls(), edited.getToolCalls());
		assertEquals("Checking order 42.", edited.getText());

		var partial = MessagesExample.streamedArguments();
		assertTrue(StreamingParts.isPartial(partial.getParts().get(0)));
		assertTrue(partial.getToolCalls().isEmpty());

		var failure = MessagesExample.failedToolResult("order-1", "lookupOrder");
		assertTrue(failure.getResults().get(0).isError());
		assertEquals("order-1", failure.getResults().get(0).id());
	}

	@Test
	void recallsPreviousMessagesOnlyWithinTheSameSession() {
		var prompts = new ArrayList<Prompt>();
		ChatModel model = prompt -> {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Acknowledged"))));
		};
		var sessions = MemoryExample.inMemorySessions();
		var client = MemoryExample.sessionClient(model, sessions);

		MemoryExample.sessionTurn(client, "one", "My project is Atlas");
		MemoryExample.sessionTurn(client, "one", "What is my project?");
		MemoryExample.sessionTurn(client, "two", "What is my project?");

		assertEquals(List.of("My project is Atlas", "Acknowledged", "What is my project?"),
				prompts.get(1).getInstructions().stream().map(Message::getText).toList());
		var otherSession = prompts.get(2).getInstructions();
		assertEquals(1, otherSession.size());
		assertInstanceOf(UserMessage.class, otherSession.get(0));
		assertEquals(4, sessions.getMessages("one").size());
		assertEquals(2, sessions.getMessages("two").size());
	}


	@Test
	void mergesRequestOptionsWithTheModelDefaults() {
		ChatModel model = new ChatModel() {

			@Override
			public DashScopeChatOptions getOptions() {
				return DashScopeChatOptions.builder().model("qwen-plus").temperature(0.8).build();
			}

			@Override
			public ChatResponse call(Prompt prompt) {
				var options = assertInstanceOf(DashScopeChatOptions.class, prompt.getOptions());
				assertEquals("qwen-plus", options.getModel());
				assertEquals(0.2, options.getTemperature());
				return new ChatResponse(List.of(new Generation(new AssistantMessage("Answer"))));
			}

		};

		var reply = ModelsExample.requestOptions(ChatClient.create(model), "Explain Spring AI");
		assertEquals("Answer", reply.getResult().getOutput().getText());
	}

	@Test
	void classifiesUsingAChoiceQuestionWithStableOptionNames() {
		DecisionModel model = prompt -> {
			assertTrue(prompt.getQuestions().containsKey("route"));
			return DecisionResponse.builder(prompt)
					.answer("route", ChoiceAnswer.builder().value("billing")
							.probabilities(Map.of("billing", 0.9, "support", 0.1)).build())
					.build();
		};

		var response = ModelsExample.classifyRequest(model, "Where is my invoice?");
		assertEquals("billing", response.getChoice("route").getValue());
		assertEquals(0.9, response.getChoice("route").getProbability());
	}

	@Test
	void convertsAgentTextToAContactAndChecksReviewFields() throws Exception {
		var contact = StructuredOutputExample.extractContact(
				prompt -> reply("""
						```json
						{"name":"Ada","email":"ada@example.com","phone":"1234"}
						```
						"""), "Ada, ada@example.com");
		assertEquals("ada@example.com", contact.email());
		assertThrows(StructuredOutputException.class,
				() -> StructuredOutputExample.extractContact(prompt -> reply("null"), "Ada"));

		var review = StructuredOutputExample.analyzeReview(
				prompt -> reply("{\"rating\":5,\"sentiment\":\"POSITIVE\",\"keyPoints\":[\"配送快\"]}"), "A positive review");
		assertEquals(List.of("配送快"), review.keyPoints());
		var invalidRating = assertThrows(StructuredOutputException.class,
				() -> StructuredOutputExample.analyzeReview(
						prompt -> reply("{\"rating\":8,\"sentiment\":\"POSITIVE\",\"keyPoints\":[]}"), "An invalid rating"));
		assertTrue(invalidRating.getMessage().contains("maximum"));
	}

	@Test
	void appendsSchemaAfterRenderingTheAgentInstruction() throws Exception {
		String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
		ChatModel model = prompt -> {
			assertTrue(prompt.getInstructions().stream().map(Message::getText)
					.anyMatch(text -> text.startsWith("Extract Ada" + System.lineSeparator()) && text.endsWith(schema)));
			return reply("{\"name\":\"Ada\"}");
		};
		var agent = ReactAgent.builder().name("structured_template").model(model)
				.instruction("Extract {contact}")
				.templateRenderer((template, variables) -> {
					assertEquals("Extract {contact}", template);
					return template.replace("{contact}", variables.get("contact").toString());
				})
				.outputSchema(schema)
				.build();

		assertEquals("{\"name\":\"Ada\"}", agent.call(Map.of("input", "Find contact", "contact", "Ada")).getText());
	}

	@Test
	void usesTheOutputTypeWhenTheExplicitSchemaIsBlank() throws Exception {
		ChatModel model = prompt -> {
			assertTrue(prompt.getInstructions().stream().map(Message::getText)
					.anyMatch(text -> text.contains("\"email\"") && text.contains("\"phone\"")));
			return reply("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"phone\":\"1234\"}");
		};
		var agent = ReactAgent.builder().name("schema_fallback").model(model)
				.outputSchema("  ").outputType(StructuredOutputExample.ContactInfo.class).build();
		agent.call("Extract Ada's contact details");
	}

	@Test
	void retriesMalformedOutputInAStandaloneExtraction() {
		var requests = new ArrayList<Prompt>();
		ChatModel model = prompt -> {
			requests.add(prompt);
			return requests.size() == 1 ? reply("Not JSON")
					: reply("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"phone\":\"1234\"}");
		};
		var contact = StructuredOutputExample.validatedContact(ChatClient.create(model), "Find a contact");
		assertEquals("Ada", contact.name());
		assertEquals(2, requests.size());
	}

	private static ChatResponse reply(String content) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
	}

	@Test
	void reportsGuardrailRefusalInsteadOfRetryingStructuredOutput() {
		var requests = new ArrayList<Prompt>();
		ChatModel model = prompt -> {
			requests.add(prompt);
			return new ChatResponse(List.of(GuardrailRefusal.generation("Refused by the policy")));
		};
		assertThrows(GuardrailRefusalException.class,
				() -> StructuredOutputExample.providerStructuredContact(ChatClient.create(model), "Find a contact"));
		assertEquals(1, requests.size());
	}

	record OrderRequest(String id) {
	}

}
