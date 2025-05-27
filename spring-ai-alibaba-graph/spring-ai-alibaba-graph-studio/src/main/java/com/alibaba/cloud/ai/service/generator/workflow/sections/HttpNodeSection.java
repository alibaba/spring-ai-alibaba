package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class HttpNodeSection implements NodeSection {
    @Override public boolean support(NodeType t) {
        return NodeType.HTTP.equals(t);
    }
    // todo: add HttpNodeData
    @Override public String render(Node node) {
//        HttpNodeData d = (HttpNodeData) node.getData();
//        String id = node.getId();
//        return String.format(
//                "// —— HttpNode [%s] ——%n" +
//                        "HttpNode %1$sNode = HttpNode.builder()%n" +
//                        "    .method(\"%s\")%n" +
//                        "    .url(\"%s\")%n" +
//                        "    .build();%n" +
//                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
//                id, d.getMethod(), d.getUrl(), id
//        );
        return "";
    }
}
