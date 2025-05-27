package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class ToolNodeSection implements NodeSection {
    @Override public boolean support(NodeType t) {
        return NodeType.TOOL.equals(t);
    }
    // todo: add ToolNodeData
    @Override public String render(Node node) {
//        ToolNodeData d = (ToolNodeData) node.getData();
//        String id = node.getId();
//        return String.format(
//                "// —— ToolNode [%s] ——%n" +
//                        "ToolNode %1$sNode = ToolNode.builder()%n" +
//                        "    .toolName(\"%s\")%n" +
//                        "    .build();%n" +
//                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
//                id, d.getToolName(), id
//        );
        return "";
    }
}
