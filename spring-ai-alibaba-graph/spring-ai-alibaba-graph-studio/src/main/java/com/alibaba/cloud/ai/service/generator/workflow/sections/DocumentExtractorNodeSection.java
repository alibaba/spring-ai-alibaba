package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.DocumentExtractorNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentExtractorNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.DOC_EXTRACTOR.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        DocumentExtractorNodeData data = (DocumentExtractorNodeData) node.getData();
        String id = node.getId();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("// —— DocumentExtractorNode [%s] ——%n", id));
        sb.append(String.format("DocumentExtractorNode %sNode = DocumentExtractorNode.builder()%n", id));

        List<String> fileList = data.getFileList();
        if (fileList != null && !fileList.isEmpty()) {
            String joined = fileList.stream()
                    .map(f -> "\"" + escape(f) + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .fileList(List.of(%s))%n", joined));
        }

        List<com.alibaba.cloud.ai.model.VariableSelector> inputs = data.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            // 取第一个 VariableSelector 的 name 作为 paramsKey
            String key = inputs.get(0).getName();
            sb.append(String.format("    .paramsKey(\"%s\")%n", escape(key)));
        }

        String outputKey = data.getOutputKey();
        sb.append(String.format("    .outputKey(\"%s\")%n", escape(outputKey)));

        sb.append("    .build();\n");
        sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n", id, id));

        return sb.toString();
    }

}
