package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.DocumentExtractorNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class DocumentExtractorNodeSection implements NodeSection {
    @Override public boolean support(NodeType t) {
        return NodeType.DOC_EXTRACTOR.equals(t);
    }
    @Override public String render(Node node) {
        DocumentExtractorNodeData d = (DocumentExtractorNodeData) node.getData();
        String id = node.getId();
        return String.format(
                "// —— DocumentExtractor [%s] ——%n" +
                        "DocumentExtractorNode %1$sNode = DocumentExtractorNode.builder()%n" +
                        "    .outputSchema(DocumentExtractorNodeData.DEFAULT_OUTPUT_SCHEMA)%n" +
                        "    .build();%n" +
                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
                id, id
        );
    }
}
