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
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class LLMNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
    public String render(Node node) {
        NodeData model = node.getData();
        LLMNodeData data = (LLMNodeData) model;
        String id = node.getId();
        // render promptTemplate
        StringBuilder prompts = new StringBuilder();
        for (LLMNodeData.PromptTemplate item : data.getPromptTemplate()) {
            prompts.append(String.format(
                    "    new PromptItem(\"%s\", \"%s\"),%n",
                    item.getRole(), item.getText()
            ));
        }
        // render memoryConfig
        String mem = data.getMemoryConfig() != null
                ? String.format(
                "        .memoryConfig(new MemoryConfig(%b, %d))%n",
                data.getMemoryConfig().getWindowEnabled(),
                data.getMemoryConfig().getWindowSize()
        )
                : "";

        return String.format(
                "// —— LLM 节点 [%s] ——%n" +
                        "stateGraph.addNode(\"%s\",%n" +
                        "    AsyncNodeAction.node_async(%n" +
                        "        new LlmNodeBuilder()%n" +
                        "            .model(\"%s\",\"%s\",\"%s\")%n" +
                        "            .promptTemplate(List.of(%n%s" +
                        "            ))%n%s" +
                        "            .build()%n" +
                        "    )%n" +
                        ");%n%n",
                id, id,
                data.getModel().getMode(),
                data.getModel().getName(),
                data.getModel().getProvider(),
                prompts.toString(),
                mem
        );
    }

}
