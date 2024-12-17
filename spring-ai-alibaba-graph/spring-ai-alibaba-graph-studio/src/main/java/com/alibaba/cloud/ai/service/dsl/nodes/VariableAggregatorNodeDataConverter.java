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
        VariableAggregatorNodeData variableAggregatorNodeData = new VariableAggregatorNodeData(null, null);
        variableAggregatorNodeData.setTitle((String) data.get("title"));
        variableAggregatorNodeData.setType((String) data.get("type"));
        variableAggregatorNodeData.setSelected((Boolean) data.get("selected"));
        variableAggregatorNodeData.setOutput_type((String) data.get("output_type"));
        variableAggregatorNodeData.setDesc((String) data.get("desc"));
        variableAggregatorNodeData.setVariables((List<List<String>>) data.get("variables"));
        Map<String,Object> advanced_settings = (Map<String, Object>) data.get("advanced_settings");
        VariableAggregatorNodeData.AdvancedSettings advancedSettings  = new VariableAggregatorNodeData.AdvancedSettings();
        advancedSettings.setGroup_enabled((Boolean) advanced_settings.get("group_enabled"));
        advancedSettings.setGroups(JSON.parseArray(JSON.toJSONString(advanced_settings.get("groups")), VariableAggregatorNodeData.Groups.class));
        variableAggregatorNodeData.setAdvanced_settings(advancedSettings);
        return variableAggregatorNodeData;
    }


    @Override
    @SneakyThrows
    public Map<String, Object> dumpDifyData(VariableAggregatorNodeData nodeData) {
        HashMap<String, Object> result = new HashMap<>();
        ReflectionUtils.doWithFields(nodeData.getClass(), field -> {
            field.setAccessible(true);
            Object value = field.get(nodeData);
            if (field.getName().equalsIgnoreCase("advanced_settings")){
                HashMap<Object, Object> advancedSettings = new HashMap<>();
                VariableAggregatorNodeData.AdvancedSettings advancedSettings1 = (VariableAggregatorNodeData.AdvancedSettings) value;
                advancedSettings.put("group_enabled",advancedSettings1.isGroup_enabled());
                List<VariableAggregatorNodeData.Groups> groups1 = advancedSettings1.getGroups();
                List<Map<String,Object>> groups = new ArrayList<>();
                for (VariableAggregatorNodeData.Groups group : groups1) {
                    Map<String, Object> groupMap = new HashMap<>();
                    groupMap.put("output_type",group.getOutput_type());
                    groupMap.put("variables",group.getVariables());
                    groupMap.put("group_name",group.getGroup_name());
                    groupMap.put("groupId",group.getGroupId());
                    groups.add(groupMap);
                }
                advancedSettings.put("groups",groups);
                result.put("advanced_settings",advancedSettings);
            }else {
                result.put(field.getName(), value);
            }
        });
        return result;
    }



}
