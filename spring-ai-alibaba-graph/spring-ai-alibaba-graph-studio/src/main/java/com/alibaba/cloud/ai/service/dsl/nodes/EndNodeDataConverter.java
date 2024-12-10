package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EndNodeDataConverter implements NodeDataConverter {

	@Override
	public Boolean supportType(String nodeType) {
		return NodeType.END.value().equals(nodeType);
	}

	@Override
	public NodeData parseDifyData(Map<String, Object> data) {
		List<Map<String, Object>> outputsMap = (List<Map<String, Object>>) data.get("outputs");
		List<VariableSelector> inputs = outputsMap.stream().map(output -> {
			List<String> valueSelector = (List<String>) output.get("value_selector");
			String variable = (String) output.get("variable");
			return new VariableSelector(valueSelector.get(0), valueSelector.get(1)).setLabel(variable);
		}).toList();
		return new EndNodeData(inputs, EndNodeData.DEFAULT_OUTPUTS);
	}

	@Override
	public Map<String, Object> dumpDifyData(NodeData nodeData) {
		EndNodeData endNodeData = (EndNodeData) nodeData;
		Map<String, Object> data = new HashMap<>();
		List<Map<String, Object>> outputsMap = endNodeData.getInputs()
			.stream()
			.map(input -> Map.of("value_selector", List.of(input.getNamespace(), input.getName()), "variable",
					input.getLabel()))
			.toList();
		data.put("outputs", outputsMap);
		return data;
	}

}
