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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.Message;


/**
 * Custom serializer for StreamingOutput that skips the originData field.
 * Supports serialization of outputType and message fields.
 */
public class StreamingOutputSerializer extends StdSerializer<StreamingOutput> {

    public StreamingOutputSerializer() {
        super(StreamingOutput.class);
    }

    @Override
    public void serialize(StreamingOutput value, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
        gen.writeStartObject();
        serializeFields(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(StreamingOutput value, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer) throws tools.jackson.core.JacksonException {
		WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, provider,
				typeSer.typeId(value, JsonToken.START_OBJECT));
		serializeFields(value, gen, provider);
		typeSer.writeTypeSuffix(gen, provider, typeIdDef);
    }

    private void serializeFields(StreamingOutput value, JsonGenerator gen, SerializationContext provider) throws tools.jackson.core.JacksonException {
		gen.writeStringProperty("node", value.node());
		gen.writeStringProperty("agent", value.agent());
		provider.defaultSerializeProperty("state", value.state(), gen);
		gen.writeBooleanProperty("subGraph", value.isSubGraph());

        // Serialize message if present
        Message message = value.message();
        if (message != null) {
			provider.defaultSerializeProperty("message", message, gen);
        }

        // Serialize outputType if present
        OutputType outputType = value.getOutputType();
        if (outputType != null) {
			gen.writeStringProperty("outputType", outputType.name());
        }

        // Only serialize chunk field, skip originData
        if (value.chunk() != null) {
			gen.writeStringProperty("chunk", value.chunk());
        }
    }
}
