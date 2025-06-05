package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("// —— LlmNode [%s] ——%n", id));
        sb.append(String.format("LlmNode %sNode = LlmNode.builder()%n", id));

        if (d.getSystemPromptTemplate() != null) {
            sb.append(String.format("    .systemPromptTemplate(\"%s\")%n", escape(d.getSystemPromptTemplate())));
        }

        if (d.getUserPromptTemplate() != null) {
            sb.append(String.format("    .userPromptTemplate(\"%s\")%n", escape(d.getUserPromptTemplate())));
        }

        if (d.getSystemPromptTemplateKey() != null) {
            sb.append(String.format("    .systemPromptTemplateKey(\"%s\")%n", escape(d.getSystemPromptTemplateKey())));
        }

        if (d.getUserPromptTemplateKey() != null) {
            sb.append(String.format("    .userPromptTemplateKey(\"%s\")%n", escape(d.getUserPromptTemplateKey())));
        }

        Map<String, Object> params = d.getParams();
        if (params != null && !params.isEmpty()) {
            String joined = params.entrySet().stream()
                    .map(e -> String.format("\"%s\", \"%s\"", escape(e.getKey()), escape(String.valueOf(e.getValue()))))
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .params(Map.of(%s))%n", joined));
        }

        if (d.getParamsKey() != null) {
            sb.append(String.format("    .paramsKey(\"%s\")%n", escape(d.getParamsKey())));
        }

        List<?> messages = d.getMessages();
        if (messages != null && !messages.isEmpty()) {
            String joined = messages.stream()
                    .map(Object::toString)
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .messages(List.of(%s))%n", joined));
        }

        if (d.getMessagesKey() != null) {
            sb.append(String.format("    .messagesKey(\"%s\")%n", escape(d.getMessagesKey())));
        }

        List<?> advisors = d.getAdvisors();
        if (advisors != null && !advisors.isEmpty()) {
            String joined = advisors.stream()
                    .map(Object::toString)
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .advisors(List.of(%s))%n", joined));
        }

        List<?> toolCallbacks = d.getToolCallbacks();
        if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
            String joined = toolCallbacks.stream()
                    .map(Object::toString)
                    .map(this::escape)
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(String.format("    .toolCallbacks(List.of(%s))%n", joined));
        }

        sb.append("    .chatClient(chatClient)\n");

        if (d.getOutputKey() != null) {
            sb.append(String.format("    .outputKey(\"%s\")%n", escape(d.getOutputKey())));
        }

        sb.append("    .build();\n");
        sb.append(String.format(
                "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n",
                id, id));

        return sb.toString();
    }

}
