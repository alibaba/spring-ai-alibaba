package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.utils.StringTemplateUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.commons.collections4.CollectionUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author HeYQ
 * @since 2024-12-12 23:54
 */

public class QuestionClassifyNodeDataConverter implements NodeDataConverter {
    @Override
    public Boolean supportType(String nodeType) {
        return  NodeType.QUESTION_CLASSIFIER.value().equals(nodeType);
    }

    @Override
    public NodeData parseDifyData(Map<String, Object> data) {
        List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("query_variable_selector"))
                .filter(CollectionUtils::isNotEmpty)
                .map(variables -> Collections.singletonList(new VariableSelector(variables.get(0), variables.get(1))))
                .orElse(Collections.emptyList());

        // convert model config
        Map<String, Object> modelData = (Map<String, Object>) data.get("model");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
        LLMNodeData.ModelConfig modelConfig = new LLMNodeData.ModelConfig().setMode((String) modelData.get("mode"))
                .setName((String)modelData.get("name"))
                .setProvider((String) modelData.get("provider"))
                .setCompletionParams(objectMapper.convertValue(modelData.get("completion_params"), LLMNodeData.CompletionParams.class));

        QuestionClassifierNodeData nodeData = new QuestionClassifierNodeData(inputs, List.of(QuestionClassifierNodeData.DEFAULT_OUTPUT_SCHEMA))
                .setModel(modelConfig);

        // covert instructions
        String instruction = (String) data.get("instructions");
        if (instruction != null && !instruction.isBlank()) {
            nodeData.setInstruction(instruction);
        }

        // covert classes
        if (data.containsKey("classes")) {
            List<Map<String, Object>> classes = (List<Map<String, Object>>) data.get("classes");
            nodeData.setClasses(classes.stream()
                    .map(item -> new QuestionClassifierNodeData
                            .ClassConfig((String) item.get("id"), (String) item.get("text"))).toList());
        }

        // convert memory config
        if (data.containsKey("memory")) {
            Map<String, Object> memoryData = (Map<String, Object>) data.get("memory");
            String lastMessageTemplate = (String) memoryData.get("query_prompt_template");
            Map<String, Object> window = (Map<String, Object>) memoryData.get("window");
            Boolean windowEnabled = (Boolean) window.get("enabled");
            Integer windowSize = (Integer) window.get("size");
            LLMNodeData.MemoryConfig memory = new LLMNodeData.MemoryConfig().setWindowEnabled(windowEnabled)
                    .setWindowSize(windowSize)
                    .setLastMessageTemplate(lastMessageTemplate)
                    .setIncludeLastMessage(false);
            nodeData.setMemoryConfig(memory);
        }

        return nodeData;
    }

    @Override
    public Map<String, Object> dumpDifyData(NodeData nodeData) {
        QuestionClassifierNodeData classifierNodeData = (QuestionClassifierNodeData) nodeData;
        Map<String, Object> data = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // put memory
        LLMNodeData.MemoryConfig memory = classifierNodeData.getMemoryConfig();
        if (memory != null) {
            data.put("memory",
                    Map.of("query_prompt_template", StringTemplateUtil.toDifyTmpl(memory.getLastMessageTemplate()),
                            "role_prefix", Map.of("assistant", "", "user", ""), "window",
                            Map.of("enabled", memory.getWindowEnabled(), "size", memory.getWindowSize())));
        }

        // put model
        LLMNodeData.ModelConfig model = classifierNodeData.getModel();
        data.put("model", Map.of("mode", model.getMode(), "name", model.getName(), "provider", model.getProvider(),
                "completion_params", objectMapper.convertValue(model.getCompletionParams(), Map.class)));

        // put query_variable_selector
        List<VariableSelector> inputs = classifierNodeData.getInputs();
        Optional.ofNullable(inputs)
                .filter(CollectionUtils::isNotEmpty)
                .map(inputList -> inputList.stream()
                        .findFirst()
                        .map(input -> List.of(input.getNamespace(), input.getName()))
                        .orElse(Collections.emptyList()))
                .ifPresent(variables -> data.put("query_variable_selector", variables));

        // put instructions
        data.put("instructions", classifierNodeData.getInstruction() != null ? classifierNodeData.getInstruction() : "");

        // put Classes
        if (!CollectionUtils.isEmpty(classifierNodeData.getClasses())) {
            data.put("classes", classifierNodeData.getClasses().stream()
                    .map(item -> Map.of("id", item.getId(), "text", item.getText())).toList());
        }

        return data;
    }
}
