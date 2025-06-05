package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.ToolNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class ToolNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.TOOL.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        ToolNodeData d = (ToolNodeData) node.getData();
        String id = node.getId();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("// —— ToolNode [%s] ——%n", id));
        sb.append(String.format("ToolNode %sNode = ToolNode.builder()%n", id));

        if (d.getLlmResponseKey() != null) {
            sb.append(String.format("    .llmResponseKey(\"%s\")%n", escape(d.getLlmResponseKey())));
        }

        if (d.getOutputKey() != null) {
            sb.append(String.format("    .outputKey(\"%s\")%n", escape(d.getOutputKey())));
        }

        List<String> names = d.getToolNames();
        if (names != null && !names.isEmpty()) {
            String joined = names.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .toolNames(List.of(%s))%n", joined));
        }

        List<String> callbacks = d.getToolCallbacks();
        if (callbacks != null && !callbacks.isEmpty()) {
            String joined = callbacks.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .toolCallbacks(List.of(%s))%n", joined));
        }

        sb.append("    .toolCallbackResolver(toolCallbackResolver)\n");

        sb.append("    .build();\n");
        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id));

        return sb.toString();
    }

}
