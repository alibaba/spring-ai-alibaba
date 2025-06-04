package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class AnswerNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.ANSWER.equals(nodeType);
	}

    @Override
    public String render(Node node) {
        AnswerNodeData d = (AnswerNodeData) node.getData();
        String id = node.getId();
        // The template may contain double quotation marks and should be escaped
        String escapedTemplate = escape(d.getAnswer());
        return String.format(
                "// —— AnswerNode [%s] ——%n" +
                        "AnswerNode %1$sNode = AnswerNode.builder()%n" +
                        "    .answer(\"%s\")%n" +
                        "    .build();%n" +
                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
                id, escapedTemplate, id
        );
    }

}
