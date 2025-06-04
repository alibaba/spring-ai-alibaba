package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class ParameterParsingNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType t) {
		return NodeType.PARAMETER_PARSING.equals(t);
	}

	// todo: add ParameterParsingNodeData
	@Override
	public String render(Node node) {
		// ParameterParsingNodeData d = (ParameterParsingNodeData) node.getData();
		// String id = node.getId();
		// return String.format(
		// "// —— ParameterParsingNode [%s] ——%n" +
		// "ParameterParsingNode %1$sNode = ParameterParsingNode.builder()%n" +
		// " .schema(\"%s\")%n" +
		// " .build();%n" +
		// "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
		// id, d.getSchema(), id
		// );
		return "";
	}

}
