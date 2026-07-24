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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.agent.extension.interceptor.AssistantMessageSanitizerInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AssistantMessageSanitizerInterceptor}, covering issue #4561:
 * tool-calling AssistantMessages with {@code null} text must be rewritten with an empty
 * content so strict DeepSeek-compatible backends accept them.
 */
class AssistantMessageSanitizerInterceptorTest {

	private final AssistantMessageSanitizerInterceptor interceptor = AssistantMessageSanitizerInterceptor.builder()
			.build();

	private static AssistantMessage.ToolCall sampleToolCall() {
		return new AssistantMessage.ToolCall("call_1", "function", "search", "{\"query\": \"what is sy\"}");
	}

	/**
	 * Capture the request that actually reaches the model and echo back a dummy response.
	 */
	private static ModelCallHandler capturingHandler(AtomicReference<ModelRequest> captured) {
		return request -> {
			captured.set(request);
			return ModelResponse.of(new AssistantMessage("ok"));
		};
	}

	@Test
	void rewritesNullTextToolCallMessageToEmptyContent() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
				.content(null)
				.toolCalls(List.of(sampleToolCall()))
				.build();

		ModelRequest request = ModelRequest.builder()
				.messages(List.of(new UserMessage("search what is sy?"), toolCallMessage))
				.build();

		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		interceptor.interceptModel(request, capturingHandler(captured));

		ModelRequest forwarded = captured.get();
		assertNotNull(forwarded);
		Message rewritten = forwarded.getMessages().get(1);
		assertInstanceOf(AssistantMessage.class, rewritten);
		AssistantMessage rewrittenAssistant = (AssistantMessage) rewritten;
		// null text -> empty string content, so the serialized request keeps the content field.
		assertEquals("", rewrittenAssistant.getText());
		assertTrue(rewrittenAssistant.hasToolCalls());
		assertEquals(1, rewrittenAssistant.getToolCalls().size());
		assertEquals("search", rewrittenAssistant.getToolCalls().get(0).name());
	}

	@Test
	void preservesToolCallsMetadataAndMedia() {
		Media media = Media.builder()
				.mimeType(MimeTypeUtils.IMAGE_PNG)
				.data(URI.create("http://example.com/image.png"))
				.id("media-1")
				.build();

		AssistantMessage toolCallMessage = AssistantMessage.builder()
				.content(null)
				.toolCalls(List.of(sampleToolCall()))
				.properties(Map.of("customKey", "customValue"))
				.media(List.of(media))
				.build();

		ModelRequest request = ModelRequest.builder()
				.messages(List.of(toolCallMessage))
				.build();

		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		interceptor.interceptModel(request, capturingHandler(captured));

		AssistantMessage rewritten = (AssistantMessage) captured.get().getMessages().get(0);
		assertEquals("", rewritten.getText());
		assertEquals(List.of(sampleToolCall()), rewritten.getToolCalls());
		assertEquals("customValue", rewritten.getMetadata().get("customKey"));
		assertEquals(1, rewritten.getMedia().size());
		assertEquals("media-1", rewritten.getMedia().get(0).getId());
	}

	@Test
	void leavesMessagesUntouchedWhenNoRewriteNeeded() {
		AssistantMessage textWithToolCalls = AssistantMessage.builder()
				.content("thinking...")
				.toolCalls(List.of(sampleToolCall()))
				.build();

		List<Message> original = List.of(
				new UserMessage("hi"),
				new AssistantMessage("plain answer"),
				textWithToolCalls,
				ToolResponseMessage.builder()
						.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "result")))
						.build());

		ModelRequest request = ModelRequest.builder().messages(original).build();

		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		interceptor.interceptModel(request, capturingHandler(captured));

		// No message needed fixing: the exact same request instance is forwarded.
		assertSame(request, captured.get());
		assertSame(original, captured.get().getMessages());
	}

	@Test
	void handlesEmptyMessageListGracefully() {
		ModelRequest request = ModelRequest.builder().messages(List.of()).build();

		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		interceptor.interceptModel(request, capturingHandler(captured));

		assertSame(request, captured.get());
	}
}
