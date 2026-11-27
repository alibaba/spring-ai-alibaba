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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.alibaba.cloud.ai.graph.serializer.MessagePartsCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.part.MessagePart;

final class MessagePartsJson {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private MessagePartsJson() {
	}

	static JsonNode write(List<MessagePart> parts) {
		try {
			return MAPPER.readTree(MessagePartsCodec.write(parts));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot encode message parts", ex);
		}
	}

	static List<MessagePart> read(JsonNode parts) {
		try {
			return MessagePartsCodec.read(parts.toString());
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Invalid message parts", ex);
		}
	}

}
