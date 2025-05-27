package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class ListOperatorNodeSection implements NodeSection {
    @Override public boolean support(NodeType t) {
        return NodeType.LIST_OPERATOR.equals(t);
    }
    @Override public String render(Node node) {
//        ListOperatorNodeData d = (ListOperatorNodeData) node.getData();
//        String id = node.getId();
//        return String.format(
//                "// —— ListOperatorNode [%s] ——%n" +
//                        "ListOperatorNode %1$sNode = ListOperatorNode.builder()%n" +
//                        "    .operator(\"%s\")%n" +
//                        "    .items(%s)%n" +
//                        "    .build();%n" +
//                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
//                id, d.getOperator(), d.getItems(), id
//        );
        return "";
    }
}

