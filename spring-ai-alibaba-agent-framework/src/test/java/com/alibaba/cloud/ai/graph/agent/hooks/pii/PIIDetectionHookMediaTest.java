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

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionException;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PIIDetectionHookMediaTest {

	@ParameterizedTest
	@CsvSource({ "REDACT, Contact [REDACTED_EMAIL]", "MASK, Contact ************.com",
			"HASH, Contact <email_hash:ac34084f>" })
	void preservesMediaAndMetadataWhenRedactingUserText(RedactionStrategy strategy, String expectedText) {
		UserMessage original = userMessage("Contact test@example.com");

		UserMessage processed = beforeModel(strategy, original);

		assertEquals(expectedText, processed.getText());
		assertEquals(original.getMedia(), processed.getMedia());
		assertEquals(original.getMetadata(), processed.getMetadata());
		assertEquals("Contact test@example.com", original.getText());
	}

	@Test
	void retainsOriginalMultimodalMessageWhenNoPiiMatches() {
		UserMessage original = userMessage("Describe these attachments");

		assertSame(original, beforeModel(RedactionStrategy.REDACT, original));
	}

	@Test
	void stillBlocksMultimodalMessagesContainingPii() {
		UserMessage original = userMessage("Contact test@example.com");

		assertThrows(PIIDetectionException.class, () -> beforeModel(RedactionStrategy.BLOCK, original));
	}

	private UserMessage userMessage(String text) {
		Media image = Media.builder()
			.mimeType(MimeTypeUtils.IMAGE_PNG)
			.data(URI.create("https://example.com/image.png"))
			.id("image-1")
			.build();
		Media audio = Media.builder()
			.mimeType(MimeTypeUtils.parseMimeType("audio/wav"))
			.data(URI.create("https://example.com/audio.wav"))
			.id("audio-1")
			.build();
		return UserMessage.builder()
			.text(text)
			.media(List.of(image, audio))
			.metadata(Map.of("requestId", "request-1"))
			.build();
	}

	private UserMessage beforeModel(RedactionStrategy strategy, UserMessage message) {
		PIIDetectionHook hook = PIIDetectionHook.builder().piiType(PIIType.EMAIL).strategy(strategy).build();
		OverAllState state = new OverAllState(Map.of("messages", List.of(message)));
		Map<String, Object> updates = MessagesModelHook.beforeModelAction(hook)
			.apply(state, RunnableConfig.builder().build())
			.join();
		ReplaceAllWith<?> replacement = assertInstanceOf(ReplaceAllWith.class, updates.get("messages"));
		assertEquals(1, replacement.newValues().size());
		return assertInstanceOf(UserMessage.class, replacement.newValues().get(0));
	}

}
