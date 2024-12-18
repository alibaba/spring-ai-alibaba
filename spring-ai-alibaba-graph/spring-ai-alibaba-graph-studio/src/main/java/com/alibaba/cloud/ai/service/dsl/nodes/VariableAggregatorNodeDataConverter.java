package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VariableAggregatorNodeDataConverter implements NodeDataConverter<VariableAggregatorNodeData> {

    @Override
    public Boolean supportType(String nodeType) {
        return NodeType.AGGREGATOR.value().equals(nodeType);
    }


    @Override
    public VariableAggregatorNodeData parseDifyData(Map<String, Object> data) {
        Map<String, Object> advanced_settings = (Map<String, Object>) data.get("advanced_settings");
        VariableAggregatorNodeData.AdvancedSettings advancedSettings = new VariableAggregatorNodeData.AdvancedSettings();
        advancedSettings.setGroupEnabled((Boolean) advanced_settings.get("group_enabled"));
        advancedSettings.setGroups(JSON.parseArray(JSON.toJSONString(advanced_settings.get("groups")), VariableAggregatorNodeData.Groups.class));
        return VariableAggregatorNodeData.builder()
                .variables((List<List<String>>) data.get("variables"))
                .outputType((String) data.get("output_type"))
                .advancedSettings(advancedSettings)
                .build();
    }


    @Override
    @SneakyThrows
    public Map<String, Object> dumpDifyData(VariableAggregatorNodeData nodeData) {
        Map<String,Object> result = new HashMap<>();
        HashMap<Object, Object> advancedSettings = new HashMap<>();
        VariableAggregatorNodeData.AdvancedSettings advancedSettings1 = nodeData.getAdvancedSettings();
        advancedSettings.put("group_enabled", advancedSettings1.isGroupEnabled());
        List<VariableAggregatorNodeData.Groups> groups1 = advancedSettings1.getGroups();
        List<Map<String, Object>> groups = new ArrayList<>();
        for (VariableAggregatorNodeData.Groups group : groups1) {
            Map<String, Object> groupMap = new HashMap<>();
            groupMap.put("output_type", group.getOutputType());
            groupMap.put("variables", group.getVariables());
            groupMap.put("group_name", group.getGroupName());
            groupMap.put("groupId", group.getGroupId());
            groups.add(groupMap);
        }
        advancedSettings.put("groups", groups);
        result.put("variables",nodeData.getVariables());
        result.put("output_type",nodeData.getOutputType());
        result.put("advanced_settings",advancedSettings);
        return result;
    }


}
