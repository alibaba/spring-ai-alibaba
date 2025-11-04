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
        OverAllState overAllState = objectMapper.convertValue(treeNode.get("state"), OverAllState.class);
        boolean subGraph = objectMapper.convertValue(treeNode.get("subGraph"), boolean.class);
        NodeOutput nodeOutput = NodeOutput.of(node, overAllState);
        nodeOutput.setSubGraph(subGraph);
        return nodeOutput;
    }
}
