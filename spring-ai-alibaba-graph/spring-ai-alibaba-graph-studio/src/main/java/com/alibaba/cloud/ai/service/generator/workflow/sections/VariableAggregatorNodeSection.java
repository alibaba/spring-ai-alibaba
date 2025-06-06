/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VariableAggregatorNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.VARIABLE_AGGREGATOR.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        VariableAggregatorNodeData d = (VariableAggregatorNodeData) node.getData();
        String id = node.getId();

        List<String> keys = d.getInputs().stream()
            .map(VariableSelector::getName)
            .collect(Collectors.toList());

        String inputKeysCode;
        if (keys.isEmpty()) {
            inputKeysCode = "List.of()";
        } else {
            String joined = keys.stream()
                                .map(k -> "\"" + escapeJavaString(k) + "\"")
                                .collect(Collectors.joining(", "));
            inputKeysCode = "List.of(" + joined + ")";
        }

        String outputKeyEscaped = escapeJavaString(d.getOutputKey());

        return String.format(
            "// —— VariableAggregatorNode [%s] ——%n" +
            "VariableAggregatorNode %1$sNode = VariableAggregatorNode.builder()%n" +
            "    .inputKeys(%s)%n" +
            "    .outputKey(\"%s\")%n" +
            "    .build();%n" +
            "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
            id,
            inputKeysCode,
            outputKeyEscaped,
            id
        );
    }

    private String escapeJavaString(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
