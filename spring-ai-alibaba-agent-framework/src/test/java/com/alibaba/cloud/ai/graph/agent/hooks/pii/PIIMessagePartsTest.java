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
package com.alibaba.cloud.ai.graph.agent.hooks.pii;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionException;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.MediaPart;
import org.springframework.ai.chat.messages.part.OpaquePayload;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import reactor.core.publisher.Flux;
import org.springframework.ai.chat.messages.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PIIMessagePartsTest {

	private static final MediaPart IMAGE = MediaPart.of(Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG)
			.data("image fixture").build());

	private static final RunnableConfig CONFIG = RunnableConfig.builder().build();

	@Test
	void redactsAcrossTextPartsAndPreservesNestedResultParts() {
		var user = UserMessage.builder().parts(List.of(TextPart.of("Send to a@"), IMAGE,
				TextPart.of("example.test please"))).build();
		var result = new ToolResultPart("call", "lookup", List.of(TextPart.of("b@example.test"), IMAGE), true,
				null, Map.of("position", "#1"));
		var mediaOnly = new ToolResultPart(null, "image", List.of(IMAGE), false, null, Map.of());
		var tool = ToolResponseMessage.builder().results(List.of(result, mediaOnly)).build();
		var hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).applyToToolResults(true).build();

		var state = new OverAllState(Map.of("messages", List.of(user, tool)));
		var updated = MessagesModelHook.beforeModelAction(hook).apply(state, CONFIG).join();
		var messages = assertInstanceOf(ReplaceAllWith.class, updated.get("messages")).newValues();

		var editedUser = assertInstanceOf(UserMessage.class, messages.get(0));
		assertEquals(List.of(TextPart.of("Send to [REDACTED_EMAIL]"), IMAGE, TextPart.of(" please")),
				editedUser.getParts());
		var results = assertInstanceOf(ToolResponseMessage.class, messages.get(1)).getResults();
		assertEquals(List.of(TextPart.of("[REDACTED_EMAIL]"), IMAGE), results.get(0).content());
		assertTrue(results.get(0).isError());
		assertEquals(result.attributes(), results.get(0).attributes());
		assertSame(mediaOnly, results.get(1));
	}

	@Test
	void keepsAssistantReasoningAndTheSignedToolCallDuringOutputRedaction() {
		var reasoning = ReasoningPart.of("thinking");
		var signed = new ToolCallPart(new AssistantMessage.ToolCall("c", "function", "lookup", "{}"),
				new OpaquePayload("fixture", "signature", "value"), Map.of());
		var message = AssistantMessage.builder().parts(List.of(reasoning, TextPart.of("a@example.test"), signed)).build();
		var hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).applyToOutput(true).build();

		var updated = MessagesModelHook.afterModelAction(hook)
				.apply(new OverAllState(Map.of("messages", List.of(message))), CONFIG).join();
		var messages = assertInstanceOf(ReplaceAllWith.class, updated.get("messages")).newValues();
		assertEquals(List.of(reasoning, TextPart.of("[REDACTED_EMAIL]"), signed),
				assertInstanceOf(AssistantMessage.class, messages.get(0)).getParts());
	}

	@Test
	void redactsTheModelValueExposedUnderAnOutputKey() throws Exception {
		var hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).applyToOutput(true).build();
		var agent = ReactAgent.builder().name("pii-output").hooks(List.of(hook)).outputKey("answer")
				.model(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("a@example.test")))))
				.build();

		assertEquals("[REDACTED_EMAIL]", agent.call("reply").getText());
	}

	@Test
	void releasesOnlyRedactedOutputWhenTheMatchSpansStreamChunks() throws Exception {
		var agent = streamingAgent(RedactionStrategy.REDACT);
		var messages = agent.streamMessages("reply").map(Message::getText).collectList().block(Duration.ofSeconds(5));
		assertEquals(List.of("Send to [REDACTED_EMAIL] please"), messages);
	}

	@Test
	void blocksBeforeAnyAssistantChunkIsPublished() throws Exception {
		var agent = streamingAgent(RedactionStrategy.BLOCK);
		var signals = agent.streamMessages("reply").materialize().collectList().block(Duration.ofSeconds(5));
		assertEquals(1, signals.size());
		assertTrue(signals.get(0).isOnError());
		assertInstanceOf(PIIDetectionException.class, signals.get(0).getThrowable());
	}

	private static ReactAgent streamingAgent(RedactionStrategy strategy) {
		var hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).strategy(strategy).applyToOutput(true).build();
		return ReactAgent.builder().name("pii-stream").hooks(List.of(hook)).model(new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new AssertionError("The agent should still use the streaming model");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.just(
						new ChatResponse(List.of(new Generation(new AssistantMessage("Send to a@")))),
						new ChatResponse(List.of(new Generation(new AssistantMessage("example.test please")))));
			}
		}).build();
	}

	@Test
	void blocksSplitSensitiveTextBeforeModel() {
		var message = UserMessage.builder().parts(List.of(TextPart.of("user@"), IMAGE, TextPart.of("example.test"))).build();
		var hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).strategy(RedactionStrategy.BLOCK).build();
		assertThrows(PIIDetectionException.class, () -> hook.beforeModel(List.of(message), CONFIG));
	}

}
