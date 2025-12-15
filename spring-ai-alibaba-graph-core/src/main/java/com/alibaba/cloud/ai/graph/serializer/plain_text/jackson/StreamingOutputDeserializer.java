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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;

/**
 * Custom deserializer for StreamingOutput.
 * Supports deserialization of outputType and message fields.
 */
public class StreamingOutputDeserializer extends StdDeserializer<StreamingOutput> {

    public StreamingOutputDeserializer() {
        super(StreamingOutput.class);
    }

    @Override
    public StreamingOutput deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper objectMapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        String nodeName = node.has("node") && !node.get("node").isNull() ? node.get("node").asText() : null;
        String agentName = node.has("agent") && !node.get("agent").isNull() ? node.get("agent").asText() : null;
        // Use readValue instead of convertValue to ensure custom deserializers are triggered
        // This is critical for types like DeepSeekAssistantMessage that may be nested in OverAllState
        OverAllState state = null;
        if (node.has("state") && !node.get("state").isNull()) {
            state = objectMapper.readValue(objectMapper.treeAsTokens(node.get("state")), OverAllState.class);
        }
        String chunk = node.has("chunk") && !node.get("chunk").isNull() ? node.get("chunk").asText() : null;

        // Deserialize message if present
        Message message = null;
        if (node.has("message") && !node.get("message").isNull()) {
            message = objectMapper.readValue(objectMapper.treeAsTokens(node.get("message")), Message.class);
        }

        // Deserialize outputType if present
        OutputType outputType = null;
        if (node.has("outputType") && !node.get("outputType").isNull()) {
            String outputTypeStr = node.get("outputType").asText();
            try {
                outputType = OutputType.valueOf(outputTypeStr);
            } catch (IllegalArgumentException e) {
                // If enum value is not found, outputType remains null
            }
        }

        // Create StreamingOutput with all available fields
        // Prefer constructor with message if message is present
        if (message != null) {
            // Use constructor with message
            StreamingOutput<?> output = new StreamingOutput<>(message, nodeName, agentName, state, outputType);
            return output;
        } else {
            // Fallback to deprecated constructor with chunk (for backward compatibility)
            return new StreamingOutput<>(chunk, nodeName, agentName, state);
        }
    }
}

