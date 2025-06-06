package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class VariableAggregatorNodeDataConverter extends AbstractNodeDataConverter<VariableAggregatorNodeData> {

    @Override
    public Boolean supportNodeType(NodeType nodeType) {
        return NodeType.AGGREGATOR.equals(nodeType);
    }

    @Override
    protected List<DialectConverter<VariableAggregatorNodeData>> getDialectConverters() {
        return Arrays.stream(AggregatorNodeDialectConverter.values())
                .map(AggregatorNodeDialectConverter::dialectConverter)
                .toList();
    }

    private enum AggregatorNodeDialectConverter {

        DIFY(new DialectConverter<>() {
            @Override
            public Boolean supportDialect(DSLDialectType dialectType) {
                return DSLDialectType.DIFY.equals(dialectType);
            }

            @SuppressWarnings("unchecked")
            @Override
            public VariableAggregatorNodeData parse(Map<String, Object> data) {
                VariableAggregatorNodeData.AdvancedSettings advancedSettings;
                Object advRaw = data.get("advanced_settings");
                if (advRaw instanceof Map<?, ?>) {
                    Map<String, Object> advanced_map = (Map<String, Object>) advRaw;
                    advancedSettings = new VariableAggregatorNodeData.AdvancedSettings();

                    Object ge = advanced_map.get("group_enabled");
                    advancedSettings.setGroupEnabled(ge instanceof Boolean ? (Boolean) ge : false);

                    Object groupsRaw = advanced_map.get("groups");
                    if (groupsRaw instanceof List<?>) {
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            String json = objectMapper.writeValueAsString(groupsRaw);
                            List<VariableAggregatorNodeData.Groups> groupsList =
                                    objectMapper.readValue(json, new TypeReference<List<VariableAggregatorNodeData.Groups>>() {});
                            advancedSettings.setGroups(groupsList);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to parse 'advanced_settings.groups' JSON", e);
                        }
                    } else {
                        advancedSettings.setGroups(Collections.emptyList());
                    }
                } else {
                    advancedSettings = new VariableAggregatorNodeData.AdvancedSettings();
                    advancedSettings.setGroupEnabled(false);
                    advancedSettings.setGroups(Collections.emptyList());
                }

                List<List<String>> variables = Collections.emptyList();
                Object varRaw = data.get("variables");
                if (varRaw instanceof List<?>) {
                    //noinspection unchecked
                    variables = (List<List<String>>) varRaw;
                }

                String outputType = data.containsKey("output_type")
                        ? (String) data.get("output_type")
                        : null;

                return new VariableAggregatorNodeData(
                        null,
                        null,
                        variables,
                        outputType,
                        advancedSettings
                );
            }

            @Override
            public Map<String, Object> dump(VariableAggregatorNodeData nodeData) {
                Map<String, Object> result = new HashMap<>();

                result.put("variables", nodeData.getVariables());

                if (nodeData.getOutputType() != null) {
                    result.put("output_type", nodeData.getOutputType());
                }

                VariableAggregatorNodeData.AdvancedSettings adv = nodeData.getAdvancedSettings();
                if (adv != null) {
                    Map<String, Object> advMap = new LinkedHashMap<>();
                    advMap.put("group_enabled", adv.isGroupEnabled());

                    List<VariableAggregatorNodeData.Groups> groupsList = adv.getGroups();
                    List<Map<String, Object>> groupsOut = new ArrayList<>();
                    if (groupsList != null) {
                        for (VariableAggregatorNodeData.Groups g : groupsList) {
                            Map<String, Object> gm = new LinkedHashMap<>();
                            gm.put("group_name", g.getGroupName());
                            gm.put("groupId", g.getGroupId());
                            gm.put("output_type", g.getOutputType());
                            gm.put("variables", g.getVariables());
                            groupsOut.add(gm);
                        }
                    }
                    advMap.put("groups", groupsOut);

                    result.put("advanced_settings", advMap);
                }

                return result;
            }
        }),

        CUSTOM(defaultCustomDialectConverter(VariableAggregatorNodeData.class));

        private final DialectConverter<VariableAggregatorNodeData> converter;
        AggregatorNodeDialectConverter(DialectConverter<VariableAggregatorNodeData> converter) {
            this.converter = converter;
        }
        public DialectConverter<VariableAggregatorNodeData> dialectConverter() {
            return this.converter;
        }
    }
}
