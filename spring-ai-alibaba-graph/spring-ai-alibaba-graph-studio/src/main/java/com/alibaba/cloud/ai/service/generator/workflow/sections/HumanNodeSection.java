package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class HumanNodeSection implements NodeSection {
    @Override public boolean support(NodeType t) {
        return NodeType.HUMAN.equals(t);
    }
    // todo: add HumanNodeData
    @Override public String render(Node node) {
//        HumanNodeData d = (HumanNodeData) node.getData();
//        String id = node.getId();
//        return String.format(
//                "// —— HumanNode [%s] ——%n" +
//                        "HumanNode %1$sNode = HumanNode.builder()%n" +
//                        "    .prompt(\"%s\")%n" +
//                        "    .build();%n" +
//                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
//                id, d.getPrompt(), id
//        );
        return "";
    }
}
