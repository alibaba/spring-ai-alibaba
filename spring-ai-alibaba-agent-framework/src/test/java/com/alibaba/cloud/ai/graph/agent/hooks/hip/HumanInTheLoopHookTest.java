/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hooks.hip;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanInTheLoopHookTest {

	private static final String ORIGINAL_ARGUMENTS = "{\"city\":\"Nanchang\",\"hotelName\":\"Ji Hotel\"}";

	private static final String EDITED_ARGUMENTS = "{\"city\":\"Shanghai\",\"hotelName\":\"Atour Hotel\"}";

	private final HumanInTheLoopHook hook = HumanInTheLoopHook.builder()
		.approvalOn("bookHotelTool", "Approve hotel booking")
		.build();

	@Test
	void editedFeedbackReplacesPendingMessageAndPreservesItsContent() {
		UserMessage user = new UserMessage("Book a hotel");
		AssistantMessage pending = AssistantMessage.builder()
			.content("Booking a hotel")
			.properties(Map.of("custom", "metadata"))
			.media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create("https://example.com/hotel.png"))))
			.toolCalls(List.of(toolCall("booking", ORIGINAL_ARGUMENTS)))
			.build();
		List<Message> originalMessages = List.of(user, pending);
		OverAllState state = state(originalMessages);

		state.updateState(hook
			.afterModel(state, feedbackConfig(state, feedback("booking", FeedbackResult.EDITED, EDITED_ARGUMENTS)))
			.join());

		List<Message> messages = messages(state);
		assertEquals(2, messages.size());
		assertSame(user, messages.get(0));
		AssistantMessage edited = assertInstanceOf(AssistantMessage.class, messages.get(1));
		assertEquals(pending.getText(), edited.getText());
		assertEquals(pending.getMetadata(), edited.getMetadata());
		assertEquals(pending.getMedia(), edited.getMedia());
		assertEquals(List.of(toolCall("booking", EDITED_ARGUMENTS)), edited.getToolCalls());
		assertEquals(List.of(user, pending), originalMessages);
		assertEquals(ORIGINAL_ARGUMENTS, pending.getToolCalls().get(0).arguments());
	}

	@Test
	void editedFeedbackReplacesOnlyPendingMessageWhenEarlierMessageHasSameHash() {
		// Distinct tool call IDs with the same String hash reproduce a collision in
		// RemoveByHash.
		AssistantMessage earlier = assistant(toolCall("Aa", ORIGINAL_ARGUMENTS));
		AssistantMessage pending = assistant(toolCall("BB", ORIGINAL_ARGUMENTS));
		assertNotEquals(earlier, pending);
		assertEquals(earlier.hashCode(), pending.hashCode());
		ToolResponseMessage previousResponse = ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("Aa", "bookHotelTool", "Previous booking")))
			.build();
		UserMessage nextRequest = new UserMessage("Book another hotel");
		OverAllState state = state(List.of(earlier, previousResponse, nextRequest, pending));

		state.updateState(
				hook.afterModel(state, feedbackConfig(state, feedback("BB", FeedbackResult.EDITED, EDITED_ARGUMENTS)))
					.join());

		List<Message> messages = messages(state);
		assertEquals(4, messages.size());
		assertEquals(List.of(earlier, previousResponse, nextRequest), messages.subList(0, 3));
		AssistantMessage edited = assertInstanceOf(AssistantMessage.class, messages.get(3));
		assertEquals(List.of(toolCall("BB", EDITED_ARGUMENTS)), edited.getToolCalls());
		assertFalse(messages.contains(pending));
	}

	@Test
	void mixedFeedbackKeepsToolCallOrderAndAppendsRejectionsAfterEditedMessage() {
		AssistantMessage.ToolCall edited = toolCall("edited", ORIGINAL_ARGUMENTS);
		AssistantMessage.ToolCall approved = toolCall("approved", ORIGINAL_ARGUMENTS);
		AssistantMessage.ToolCall rejected = toolCall("rejected", ORIGINAL_ARGUMENTS);
		AssistantMessage.ToolCall automatic = new AssistantMessage.ToolCall("automatic", "function", "search", "{}");
		UserMessage user = new UserMessage("Plan a trip");
		OverAllState state = state(List.of(user, assistant(edited, approved, rejected, automatic)));

		state.updateState(hook.afterModel(state,
				feedbackConfig(state, feedback("edited", FeedbackResult.EDITED, EDITED_ARGUMENTS),
						feedback("approved", FeedbackResult.APPROVED, ORIGINAL_ARGUMENTS),
						feedback("rejected", FeedbackResult.REJECTED, ORIGINAL_ARGUMENTS)))
			.join());

		List<Message> messages = messages(state);
		assertEquals(3, messages.size());
		assertSame(user, messages.get(0));
		AssistantMessage updated = assertInstanceOf(AssistantMessage.class, messages.get(1));
		assertEquals(List.of(toolCall("edited", EDITED_ARGUMENTS), approved, rejected, automatic),
				updated.getToolCalls());
		ToolResponseMessage rejection = assertInstanceOf(ToolResponseMessage.class, messages.get(2));
		assertEquals(1, rejection.getResponses().size());
		assertEquals("rejected", rejection.getResponses().get(0).id());
		assertTrue(rejection.getResponses().get(0).responseData().contains("rejected by human"));
	}

	@Test
	void noFeedbackLeavesMessagesUnchanged() {
		OverAllState state = state(List.of(assistant(toolCall("booking", ORIGINAL_ARGUMENTS))));
		assertTrue(hook.afterModel(state, RunnableConfig.builder().build()).join().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("resumeModes")
	void resumeExecutesToolOnceAndPassesConsistentHistoryToModel(FeedbackResult decision, boolean streaming)
			throws Exception {
		List<List<Message>> prompts = new ArrayList<>();
		AssistantMessage pending = assistant(toolCall("booking", ORIGINAL_ARGUMENTS));
		ChatModel model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				prompts.add(new ArrayList<>(prompt.getInstructions()));
				AssistantMessage response = prompts.size() == 1 ? pending : new AssistantMessage("Booking submitted");
				return new ChatResponse(List.of(new Generation(response)));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.defer(() -> Flux.just(call(prompt)));
			}
		};
		List<Booking> bookings = new ArrayList<>();
		FunctionToolCallback<Booking, String> tool = FunctionToolCallback
			.builder("bookHotelTool", (Booking booking) -> {
				bookings.add(booking);
				return booking.city() + ": " + booking.hotelName();
			})
			.description("Book a hotel")
			.inputType(Booking.class)
			.build();
		ReactAgent agent = ReactAgent.builder()
			.name("booking_agent")
			.model(model)
			.saver(new MemorySaver())
			.tools(tool)
			.hooks(hook)
			.build();
		RunnableConfig config = RunnableConfig.builder().threadId("booking-thread").build();

		NodeOutput first = streaming ? agent.stream("Book Ji Hotel in Nanchang", config).blockLast()
				: agent.invokeAndGetOutput("Book Ji Hotel in Nanchang", config).orElseThrow();
		InterruptionMetadata interruption = assertInstanceOf(InterruptionMetadata.class, first);
		assertEquals(1, interruption.toolFeedbacks().size());
		assertTrue(bookings.isEmpty());
		String arguments = decision == FeedbackResult.EDITED ? EDITED_ARGUMENTS : ORIGINAL_ARGUMENTS;
		InterruptionMetadata feedback = InterruptionMetadata.builder(interruption)
			.addToolFeedback(feedback("booking", decision, arguments))
			.build();
		RunnableConfig resumeConfig = RunnableConfig.builder(config)
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
			.build();

		List<NodeOutput> outputs = streaming ? agent.stream((String) null, resumeConfig).collectList().block()
				: List.of(agent.invokeAndGetOutput((String) null, resumeConfig).orElseThrow());
		NodeOutput result = outputs.get(outputs.size() - 1);

		assertFalse(outputs.stream().anyMatch(InterruptionMetadata.class::isInstance));
		assertEquals(2, prompts.size());
		Booking expectedBooking = decision == FeedbackResult.EDITED ? new Booking("Shanghai", "Atour Hotel")
				: new Booking("Nanchang", "Ji Hotel");
		assertEquals(List.of(expectedBooking), bookings);
		List<Message> resumedPrompt = prompts.get(1);
		assertEquals(3, resumedPrompt.size());
		AssistantMessage toolRequest = assertInstanceOf(AssistantMessage.class, resumedPrompt.get(1));
		assertEquals(List.of(toolCall("booking", arguments)), toolRequest.getToolCalls());
		if (streaming) {
			NodeOutput hookOutput = outputs.stream()
				.filter(output -> (Hook.getFullHookName(hook) + ".afterModel").equals(output.node()))
				.findFirst()
				.orElseThrow();
			assertEquals(toolRequest, assertInstanceOf(StreamingOutput.class, hookOutput).message());
		}
		ToolResponseMessage toolResult = assertInstanceOf(ToolResponseMessage.class, resumedPrompt.get(2));
		assertEquals("booking", toolResult.getResponses().get(0).id());
		assertEquals("\"" + expectedBooking.city() + ": " + expectedBooking.hotelName() + "\"",
				toolResult.getResponses().get(0).responseData());
		List<Message> finalMessages = messages(result.state());
		assertEquals(4, finalMessages.size());
		assertEquals("Booking submitted", finalMessages.get(3).getText());
	}

	private static Stream<Arguments> resumeModes() {
		return Stream.of(Arguments.of(FeedbackResult.EDITED, false), Arguments.of(FeedbackResult.EDITED, true),
				Arguments.of(FeedbackResult.APPROVED, false), Arguments.of(FeedbackResult.APPROVED, true));
	}

	private static AssistantMessage.ToolCall toolCall(String id, String arguments) {
		return new AssistantMessage.ToolCall(id, "function", "bookHotelTool", arguments);
	}

	private static AssistantMessage assistant(AssistantMessage.ToolCall... toolCalls) {
		return AssistantMessage.builder().content("").toolCalls(List.of(toolCalls)).build();
	}

	private static ToolFeedback feedback(String id, FeedbackResult result, String arguments) {
		return ToolFeedback.builder().id(id).name("bookHotelTool").arguments(arguments).result(result).build();
	}

	private static RunnableConfig feedbackConfig(OverAllState state, ToolFeedback... feedbacks) {
		return RunnableConfig.builder()
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY,
					InterruptionMetadata.builder("HITL", state).toolFeedbacks(List.of(feedbacks)).build())
			.build();
	}

	private static OverAllState state(List<Message> messages) {
		return new OverAllState(Map.of("messages", messages)).registerKeyAndStrategy("messages", new AppendStrategy());
	}

	private static List<Message> messages(OverAllState state) {
		return state.<List<Message>>value("messages").orElseThrow();
	}

	public record Booking(String city, String hotelName) {
	}

}
