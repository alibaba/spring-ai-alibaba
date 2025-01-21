/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class NodeOutputSerializer extends StdSerializer<NodeOutput> {

    public NodeOutputSerializer() {
        super(NodeOutput.class);
    }

    @Override
    public void serialize(NodeOutput nodeOutput, JsonGenerator gen, SerializerProvider serializerProvider)
            throws IOException {
        log.trace("NodeOutputSerializer start! {}", nodeOutput.getClass());
        gen.writeStartObject();
        if (nodeOutput instanceof StateSnapshot snapshot) {
            var checkpoint = snapshot.config().checkPointId();
            log.trace("checkpoint: {}", checkpoint);
            if (checkpoint.isPresent()) {
                gen.writeStringField("checkpoint", checkpoint.get());
            }
        }
        gen.writeStringField("node", nodeOutput.node());
        gen.writeObjectField("state", nodeOutput.state().data());
        gen.writeEndObject();
    }
}
