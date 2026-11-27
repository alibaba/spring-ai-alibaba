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
package com.alibaba.cloud.ai.agent.studio.dto.messages;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.MediaPart;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.OpaquePayload;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagePartsDTOTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void roundTripsAPIToolRequestsAndRichResultsAlongsideLegacyFields() throws Exception {
		var reason = ReasoningPart.of("reason");
		var call = new ToolCallPart(new AssistantMessage.ToolCall("call", "function", "image", "{}"),
				new OpaquePayload("provider", "signature", "fixture"), Map.of());
		var assistant = AssistantMessage.builder().parts(List.of(reason, call, TextPart.of("calling"))).build();
		var wire = mapper.writeValueAsString(MessageDTO.MessageDTOFactory.fromMessage(assistant));
		assertTrue(wire.contains("\"toolCalls\""));
		assertEquals(assistant, MessageDTO.MessageDTOFactory.toMessage(mapper.readValue(wire, MessageDTO.class)));

		var media = MediaPart.of(Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data("fixture").build());
		var result = new ToolResultPart("call", "image", List.of(TextPart.of("failed"), media), true, null, Map.of());
		var tool = ToolResponseMessage.builder().result(result).build();
		wire = mapper.writeValueAsString(MessageDTO.MessageDTOFactory.fromMessage(tool));
		assertTrue(wire.contains("\"responses\""));
		var restored = assertInstanceOf(ToolResponseMessage.class,
				MessageDTO.MessageDTOFactory.toMessage(mapper.readValue(wire, MessageDTO.class))).getResults().get(0);
		assertTrue(restored.isError());
		assertEquals("fixture", assertInstanceOf(MediaPart.class, restored.content().get(1)).media().getData());
	}

	@Test
	void readsLegacyMessagesWithoutAPartsField() throws Exception {
		String wire = "{\"messageType\":\"user\",\"content\":\"hello\",\"metadata\":{\"key\":\"value\"}}";
		var user = assertInstanceOf(UserMessage.class,
				MessageDTO.MessageDTOFactory.toMessage(mapper.readValue(wire, MessageDTO.class)));
		assertEquals(List.<MessagePart>of(TextPart.of("hello")), user.getParts());
		assertEquals("value", user.getMetadata().get("key"));
	}

}
