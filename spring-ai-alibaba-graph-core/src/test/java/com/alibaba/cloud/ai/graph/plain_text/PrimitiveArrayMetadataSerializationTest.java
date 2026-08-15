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
package com.alibaba.cloud.ai.graph.plain_text;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for primitive arrays nested inside message metadata.
 *
 * <p>
 * Jackson's WRAPPER_ARRAY default typing serializes some primitive arrays with a scalar
 * payload rather than a JSON array ({@code byte[]} as a base64 string, {@code char[]} as
 * a plain string). The custom {@code JacksonDeserializer} previously only recognized the
 * array-payload form, so {@code byte[]} values silently degraded to
 * {@code List["[B", "<base64>"]}. Gemini 3.x carries its {@code thoughtSignatures} as a
 * {@code List<byte[]>} in the assistant message metadata, so the corruption produced a
 * missing {@code thought_signature} 400 on the next tool-calling round-trip.
 * </p>
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4742">Issue #4742</a>
 */
class PrimitiveArrayMetadataSerializationTest {

	private SpringAIJacksonStateSerializer serializer;

	@BeforeEach
	void setUp() {
		AgentStateFactory<OverAllState> stateFactory = OverAllState::new;
		serializer = new SpringAIJacksonStateSerializer(stateFactory);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> roundTrip(Map<String, Object> data) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return serializer.readData(ois);
	}

	@Test
	@SuppressWarnings("unchecked")
	void thoughtSignaturesListOfByteArraysRoundTrips() throws Exception {
		byte[] sig1 = new byte[] { 1, 2, 3, 4, 5 };
		byte[] sig2 = new byte[] { -128, 0, 127, 42 };
		List<byte[]> signatures = new ArrayList<>();
		signatures.add(sig1);
		signatures.add(sig2);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("thoughtSignatures", signatures);

		AssistantMessage original = AssistantMessage.builder()
			.content("")
			.properties(metadata)
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "read_skill", "{}")))
			.build();

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> restored = roundTrip(data);

		AssistantMessage message = (AssistantMessage) restored.get("message");
		assertNotNull(message);
		Object restoredSignatures = message.getMetadata().get("thoughtSignatures");
		assertInstanceOf(List.class, restoredSignatures);

		List<Object> restoredList = (List<Object>) restoredSignatures;
		assertEquals(2, restoredList.size());
		assertInstanceOf(byte[].class, restoredList.get(0));
		assertInstanceOf(byte[].class, restoredList.get(1));
		assertArrayEquals(sig1, (byte[]) restoredList.get(0));
		assertArrayEquals(sig2, (byte[]) restoredList.get(1));

		// Tool call must survive too, so the next round is a valid assistant->tool pairing.
		assertEquals(1, message.getToolCalls().size());
		assertEquals("read_skill", message.getToolCalls().get(0).name());
	}

	@Test
	void standaloneByteArrayInMetadataRoundTrips() throws Exception {
		byte[] payload = new byte[] { 10, 20, 30, -1, 0 };

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("signature", payload);

		AssistantMessage original = AssistantMessage.builder().content("hi").properties(metadata).build();

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> restored = roundTrip(data);

		AssistantMessage message = (AssistantMessage) restored.get("message");
		Object restoredPayload = message.getMetadata().get("signature");
		assertInstanceOf(byte[].class, restoredPayload);
		assertArrayEquals(payload, (byte[]) restoredPayload);
	}

	@Test
	void charArrayInMetadataRoundTrips() throws Exception {
		char[] payload = new char[] { 'a', 'b', 'c' };

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("chars", payload);

		AssistantMessage original = AssistantMessage.builder().content("hi").properties(metadata).build();

		Map<String, Object> data = new HashMap<>();
		data.put("message", original);

		Map<String, Object> restored = roundTrip(data);

		AssistantMessage message = (AssistantMessage) restored.get("message");
		Object restoredPayload = message.getMetadata().get("chars");
		assertInstanceOf(char[].class, restoredPayload);
		assertArrayEquals(payload, (char[]) restoredPayload);
	}
}
