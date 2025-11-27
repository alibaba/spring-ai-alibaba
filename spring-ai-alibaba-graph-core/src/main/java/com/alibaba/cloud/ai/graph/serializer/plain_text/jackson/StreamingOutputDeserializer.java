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
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for StreamingOutput.
 */
public class StreamingOutputDeserializer extends StdDeserializer<StreamingOutput> {

    public StreamingOutputDeserializer() {
        super(StreamingOutput.class);
    }

    @Override
    public StreamingOutput deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper objectMapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        String nodeName = node.has("node") ? node.get("node").asText() : null;
        String agentName = node.has("agent") ? node.get("agent").asText() : null;
        // Use readValue instead of convertValue to ensure custom deserializers are triggered
        // This is critical for types like DeepSeekAssistantMessage that may be nested in OverAllState
        OverAllState state = node.has("state") ? 
            objectMapper.readValue(objectMapper.treeAsTokens(node.get("state")), OverAllState.class) : null;
        String chunk = node.has("chunk") ? node.get("chunk").asText() : null;

        // Create StreamingOutput without originData (it was not serialized)
        return new StreamingOutput<>(chunk, nodeName, agentName, state);
    }
}

