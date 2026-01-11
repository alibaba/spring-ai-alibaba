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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Serializer for NodeOutput.
 */
public class JacksonNodeOutputSerializer extends StdSerializer<NodeOutput> {

    public JacksonNodeOutputSerializer() {
        super(NodeOutput.class);
    }

    @Override
    public void serialize(NodeOutput value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        serializeFields(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(NodeOutput value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeFields(value, gen, provider);
        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    private void serializeFields(NodeOutput value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStringField("node", value.node());
        gen.writeStringField("agent", value.agent());
        gen.writeObjectField("state", value.state());
        gen.writeBooleanField("subGraph", value.isSubGraph());
    }
}
