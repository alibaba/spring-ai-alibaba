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


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * auth: dahua
 */
public class JacksonNodeOutputDeserializer extends StdDeserializer<NodeOutput> {

    protected JacksonNodeOutputDeserializer() {
        super(NodeOutput.class);
    }

    @Override
    public NodeOutput deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper objectMapper = (ObjectMapper) deserializationContext.getParser().getCodec();
        TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String node = treeNode.get("node").toString();
        String agent = treeNode.get("agent").toString();
        // Use readValue instead of convertValue to ensure custom deserializers are triggered
        // This is critical for types like DeepSeekAssistantMessage that may be nested in OverAllState
        OverAllState overAllState = objectMapper.readValue(objectMapper.treeAsTokens(treeNode.get("state")), OverAllState.class);
        boolean subGraph = objectMapper.readValue(objectMapper.treeAsTokens(treeNode.get("subGraph")), boolean.class);
        NodeOutput nodeOutput = NodeOutput.of(node, agent, overAllState, null);
        nodeOutput.setSubGraph(subGraph);
        return nodeOutput;
    }
}
