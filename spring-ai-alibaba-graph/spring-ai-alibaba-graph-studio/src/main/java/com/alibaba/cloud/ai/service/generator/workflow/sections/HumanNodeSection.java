package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.HumanNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HumanNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.HUMAN.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        HumanNodeData d = (HumanNodeData) node.getData();
        String id = node.getId();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("// —— HumanNode [%s] ——%n", id));

        String condKey = d.getInterruptConditionKey();
        // If the policy is conditioned, you need to read the boolean value from the state; Otherwise, it is always interrupted
        String condLambda = condKey != null
                ? String.format(
                "state -> state.value(\"%s\").map(v -> (Boolean)v).orElse(false)",
                condKey
        )
                : "state -> true";

        List<String> keys = d.getStateUpdateKeys();
        String updateLambda;
        if (keys != null && !keys.isEmpty()) {
            String keyListCode = keys.stream()
                    .map(k -> "\""+ escape(k) +"\"")
                    .collect(Collectors.joining(", ", "List.of(", ")"));
            updateLambda = String.format(
                    "state -> { java.util.Map<String, Object> raw = state.humanFeedback().data(); "
                            + "return raw.entrySet().stream()"
                            + ".filter(e -> %s.contains(e.getKey()))"
                            + ".collect(java.util.stream.Collectors.toMap("
                            + "java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)); }",
                    keyListCode
            );
        } else {
            // If you don't specify a key to filter, the raw feedback is simply returned
            updateLambda = "state -> state.humanFeedback().data()";
        }

        sb.append("HumanNode ").append(id).append("Node = new HumanNode(")
                .append("\"").append(d.getInterruptStrategy()).append("\", ")
                .append(condLambda).append(", ")
                .append(updateLambda).append(");\n");

        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id
        ));

        return sb.toString();
    }

}
