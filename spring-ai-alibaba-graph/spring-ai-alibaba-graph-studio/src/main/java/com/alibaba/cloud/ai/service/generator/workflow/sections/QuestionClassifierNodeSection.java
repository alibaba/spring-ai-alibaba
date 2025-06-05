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
        sb.append(String.format("        // —— QuestionClassifierNode [%s] ——%n", id));
        sb.append(String.format("        QuestionClassifierNode %sNode = QuestionClassifierNode.builder()%n", id));

        sb.append("            .chatClient(chatClient)\n");

        List<VariableSelector> inputs = data.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            String key = inputs.get(0).getName();
            sb.append(String.format("            .inputTextKey(\"%s\")%n", escape(key)));
        }

        List<String> categoryIds = data.getClasses().stream()
                .map(QuestionClassifierNodeData.ClassConfig::getId)
                .collect(Collectors.toList());
        if (!categoryIds.isEmpty()) {
            String joined = categoryIds.stream()
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("            .categories(List.of(%s))%n", joined));
        }

        String instr = data.getInstruction();
        if (instr != null && !instr.isBlank()) {
            sb.append(String.format("            .classificationInstructions(List.of(\"%s\"))%n", escape(instr)));
        }

        sb.append("            .build();\n");
        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id));

        return sb.toString();
    }

}
