package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class EndNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.END.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        String id = node.getId();
        return String.format(
            "// —— End 节点 [%s] ——%n" +
            "stateGraph.addNode(\"%s\", EndNode.create());%n%n",
            id, id
        );
    }
}
