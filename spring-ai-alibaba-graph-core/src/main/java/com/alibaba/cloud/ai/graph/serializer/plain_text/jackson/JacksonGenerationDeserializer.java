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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;

import java.io.IOException;

/**
 * auth: dahua
 */
public class JacksonGenerationDeserializer extends StdDeserializer<Generation> {

    protected JacksonGenerationDeserializer() {
        super(Generation.class);
    }

    @Override
    public Generation deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectMapper objectMapper = (ObjectMapper) ctxt.getParser().getCodec();
        TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        TreeNode assistantMessageNode = treeNode.get("assistantMessage");
        TreeNode chatGenerationMetadataNode = treeNode.get("chatGenerationMetadata");
        AssistantMessage assistantMessage = objectMapper.treeToValue(assistantMessageNode, AssistantMessage.class);
        CustomChatGenerationMetadata customChatGenerationMetadata = objectMapper.treeToValue(chatGenerationMetadataNode, CustomChatGenerationMetadata.class);
        return new Generation(assistantMessage, customChatGenerationMetadata);
    }
}
