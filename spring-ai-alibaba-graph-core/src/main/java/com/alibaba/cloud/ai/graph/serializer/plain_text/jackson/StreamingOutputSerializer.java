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

import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;

/**
 * Custom serializer for StreamingOutput that skips the originData field.
 * Supports serialization of outputType and message fields.
 */
public class StreamingOutputSerializer extends StdSerializer<StreamingOutput> {

    public StreamingOutputSerializer() {
        super(StreamingOutput.class);
    }

    @Override
    public void serialize(StreamingOutput value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("@class", value.getClass().getName());
        gen.writeStringField("node", value.node());
        gen.writeStringField("agent", value.agent());
        gen.writeObjectField("state", value.state());
        gen.writeBooleanField("subGraph", value.isSubGraph());

        // Serialize message if present
        Message message = value.message();
        if (message != null) {
            gen.writeObjectField("message", message);
        }

        // Serialize outputType if present
        OutputType outputType = value.getOutputType();
        if (outputType != null) {
            gen.writeStringField("outputType", outputType.name());
        }

        // Only serialize chunk field, skip originData
        if (value.chunk() != null) {
            gen.writeStringField("chunk", value.chunk());
        }

        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(StreamingOutput value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        serialize(value, gen, serializers);
    }
}

