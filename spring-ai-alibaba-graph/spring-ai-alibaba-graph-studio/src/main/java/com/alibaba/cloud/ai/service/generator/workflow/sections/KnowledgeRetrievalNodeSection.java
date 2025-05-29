package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeRetrievalNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType t) {
		return NodeType.KNOWLEDGE_RETRIEVAL.equals(t);
	}

	// todo: add KnowledgeRetrievalNodeData
	@Override
	public String render(Node node) {
		// KnowledgeRetrievalNodeData d = (KnowledgeRetrievalNodeData) node.getData();
		// String id = node.getId();
		// return String.format(
		// "// —— 知识检索节点 [%s] ——%n" +
		// "KnowledgeRetrievalNode %1$sNode = KnowledgeRetrievalNode.builder()%n" +
		// " .vectorName(\"%s\")%n" +
		// " .topK(%d)%n" +
		// " .build();%n" +
		// "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
		// id, d.getVectorName(), d.getTopK(), id
		// );
		return "";
	}

}
