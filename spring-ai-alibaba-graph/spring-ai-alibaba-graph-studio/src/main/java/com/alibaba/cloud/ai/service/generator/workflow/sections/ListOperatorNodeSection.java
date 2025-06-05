package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.ListOperatorNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListOperatorNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.LIST_OPERATOR.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        ListOperatorNodeData d = (ListOperatorNodeData) node.getData();
        String id = node.getId();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("// —— ListOperatorNode [%s] ——%n", id));
        sb.append(String.format("ListOperatorNode %sNode = ListOperatorNode.builder()%n", id));

        if (d.getInputTextKey() != null) {
            sb.append(String.format("    .inputTextKey(\"%s\")%n", escape(d.getInputTextKey())));
        }

        if (d.getOutputTextKey() != null) {
            sb.append(String.format("    .outputTextKey(\"%s\")%n", escape(d.getOutputTextKey())));
        }

        List<String> filters = d.getFilters();
        if (filters != null && !filters.isEmpty()) {
            String joined = filters.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .filters(List.of(%s))%n", joined));
        }

        List<String> comps = d.getComparators();
        if (comps != null && !comps.isEmpty()) {
            String joined = comps.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .comparators(List.of(%s))%n", joined));
        }

        if (d.getLimitNumber() != null) {
            sb.append(String.format("    .limitNumber(%d)%n", d.getLimitNumber()));
        }

        if (d.getElementClassType() != null) {
            sb.append(String.format("    .elementClassType(\"%s\")%n", escape(d.getElementClassType())));
        }

        sb.append("    .build();\n");
        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id));

        return sb.toString();
    }

}
