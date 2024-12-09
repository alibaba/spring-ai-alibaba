package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StartNodeDataConverter implements NodeDataConverter {

	private static final List<String> VARIABLE_STRING_TYPES = List.of("text-input", "paragraph", "select");

	@Override
	public Boolean supportType(String nodeType) {
		return NodeType.START.value().equals(nodeType);
	}

	@Override
	public NodeData parseDifyData(Map<String, Object> data) {
		List<Map<String, Object>> inputMap = (List<Map<String, Object>>) data.get("variables");
		List<StartNodeData.StartInput> startInputs = new ArrayList<>();
		List<Variable> outputs = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (Map<String, Object> variable : inputMap) {
			StartNodeData.StartInput startInput = objectMapper.convertValue(variable, StartNodeData.StartInput.class);
			String inputType = startInput.getType();
			String varType;
			if (VARIABLE_STRING_TYPES.contains(inputType)) {
				varType = VariableType.STRING.value();
			}
			else if ("number".equals(inputType)) {
				varType = VariableType.NUMBER.value();
			}
			else if ("file".equals(inputType)) {
				varType = VariableType.FILE.value();
			}
			else {
				varType = VariableType.ARRAY_FILE.value();
			}
			outputs.add(new Variable(startInput.getVariable(), varType));
			startInputs.add(startInput);
		}
		return new StartNodeData(List.of(), outputs).setStartInputs(startInputs);
	}

	@Override
	public Map<String, Object> dumpDifyData(NodeData nodeData) {
		StartNodeData startNodeData = (StartNodeData) nodeData;
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Map<String, Object> data = new HashMap<>();
		data.put("variables", objectMapper.convertValue(startNodeData.getStartInputs(), List.class));
		return data;
	}

}
