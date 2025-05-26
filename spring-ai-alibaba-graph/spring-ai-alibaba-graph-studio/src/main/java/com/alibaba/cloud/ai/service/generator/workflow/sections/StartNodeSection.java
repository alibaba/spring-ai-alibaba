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

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class StartNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.START.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        StartNodeData data = (StartNodeData) node.getData();
        String id = node.getId();
        StringBuilder vars = new StringBuilder();
        for (StartNodeData.StartInput in : data.getStartInputs()) {
            vars.append(String.format(
                "    .addVariable(\"%s\", \"%s\", \"%s\")%n",
                in.getVariable(), in.getType(), in.getLabel()
            ));
        }
        return String.format(
            "// —— Start 节点 [%s] ——%n" +
            "stateGraph.addNode(\"%s\",%n" +
            "    StartNodeBuilder.create()%n%s" +
            "        .build()%n" +
            ");%n%n",
            id, id, vars.toString()
        );
    }
}
