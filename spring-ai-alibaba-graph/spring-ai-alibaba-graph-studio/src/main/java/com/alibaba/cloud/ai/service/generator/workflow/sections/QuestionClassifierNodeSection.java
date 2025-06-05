package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuestionClassifierNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        QuestionClassifierNodeData data = (QuestionClassifierNodeData) node.getData();
        String id = node.getId();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("// —— QuestionClassifierNode [%s] ——%n", id));
        sb.append(String.format("QuestionClassifierNode %sNode = QuestionClassifierNode.builder()%n", id));

        sb.append("    .chatClient(chatClient)\n");

        List<VariableSelector> inputs = data.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            String key = inputs.get(0).getName();
            sb.append(String.format("    .inputTextKey(\"%s\")%n", escape(key)));
        }

        List<String> categoryIds = data.getClasses().stream()
                .map(QuestionClassifierNodeData.ClassConfig::getId)
                .collect(Collectors.toList());
        if (!categoryIds.isEmpty()) {
            String joined = categoryIds.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .categories(List.of(%s))%n", joined));
        }

        String instr = data.getInstruction();
        if (instr != null && !instr.isBlank()) {
            sb.append(String.format("    .classificationInstructions(List.of(\"%s\"))%n", escape(instr)));
        }

        sb.append("    .build();\n");
        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id));

        return sb.toString();
    }

}
