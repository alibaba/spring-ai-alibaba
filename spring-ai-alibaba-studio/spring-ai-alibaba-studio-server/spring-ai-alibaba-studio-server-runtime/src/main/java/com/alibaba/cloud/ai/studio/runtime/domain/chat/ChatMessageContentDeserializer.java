/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

/**
 * Custom deserializer for chat message content. Handles different types of content:
 * arrays, text, and other JSON nodes.
 */
public class ChatMessageContentDeserializer extends JsonDeserializer<Object> {

	/**
	 * Deserializes JSON content into appropriate object type.
	 * @param p JSON parser
	 * @param ctxt Deserialization context
	 * @return Deserialized object (List MultimodalContent, String, or JsonNode)
	 * @throws IOException if deserialization fails
	 */
	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.isArray()) {
			return JsonUtils.getObjectMapper().convertValue(node, new TypeReference<List<MultimodalContent>>() {
			});
		}
		else if (node.isTextual()) {
			return node.asText();
		}
		else {
			return node;
		}
	}

}
