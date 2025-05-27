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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class LLMNodeSection implements NodeSection {

    @Override
    public boolean support(NodeType nodeType) {
        return NodeType.LLM.equals(nodeType);
    }

    @Override
    public String render(Node node) {
        LLMNodeData d = (LLMNodeData) node.getData();
        String id = node.getId();

        // 把 prompt_template 原封不动地当字符串
        String msgs = d.getPromptTemplate().stream()
                .map(pt -> {
                    // 角色对应不同的 Message 类
                    String cls = pt.getRole().equals("system")
                            ? "SystemMessage" : "UserMessage";
                    // 直接把 text 包在双引号里，不做 {{…}} 的变量替换
                    String txt = pt.getText().replace("\"", "\\\"");
                    return String.format("        new %s(\"%s\")", cls, txt);
                })
                .collect(Collectors.joining(",\n"));

        // 如果有 memoryConfig，则一并渲染
        String memConfig = "";
        if (d.getMemoryConfig() != null && d.getMemoryConfig().getEnabled()) {
            memConfig = String.format(
                    "    .memoryConfig(new MemoryConfig(%b, %d))%n",
                    d.getMemoryConfig().getWindowEnabled(),
                    d.getMemoryConfig().getWindowSize()
            );
        }

        // 最终拼装
        return String.format(
                "// —— LlmNode [%s] ——%n" +
                        "LlmNode %1$sNode = LlmNode.builder()%n" +
                        "    .chatClient(chatClient)%n" +
                        "    .messages(List.of(%n%s%n    ))%n" +
                        "%s" +  // memoryConfig 段，可为空
                        "    .build();%n" +
                        "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
                id, msgs, memConfig, id
        );
    }

}

