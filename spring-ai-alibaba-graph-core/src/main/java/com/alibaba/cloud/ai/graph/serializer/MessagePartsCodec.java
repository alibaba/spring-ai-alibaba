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
package com.alibaba.cloud.ai.graph.serializer;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.util.JacksonUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Uses Spring AI's wire format for message parts in both Graph checkpoint formats.
 */
public final class MessagePartsCodec {

	private static final JsonMapper MAPPER = JacksonUtils.getDefaultJsonMapper();

	private static final TypeReference<List<MessagePart>> PARTS_TYPE = new TypeReference<>() { };

	private MessagePartsCodec() {
	}

	public static String write(List<MessagePart> parts) throws IOException {
		try {
			return MAPPER.writerFor(PARTS_TYPE).writeValueAsString(parts);
		}
		catch (JacksonException ex) {
			throw new IOException("Cannot write checkpoint message parts", ex);
		}
	}

	public static List<MessagePart> read(String json) throws IOException {
		try {
			return MAPPER.readValue(json, PARTS_TYPE);
		}
		catch (JacksonException ex) {
			throw new IOException("Cannot read checkpoint message parts", ex);
		}
	}

}
