package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CodeNodeDataConverter implements NodeDataConverter<CodeNodeData> {

	@Override
	public Boolean supportType(String nodeType) {
		return NodeType.CODE.value().equals(nodeType);
	}

	@Override
	public CodeNodeData parseDifyData(Map<String, Object> data) {
		List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("variables");
		List<VariableSelector> inputs = variables.stream().map(variable -> {
			List<String> selector = (List<String>) variable.get("value_selector");
			return new VariableSelector(selector.get(0), selector.get(1), (String) variable.get("variable"));
		}).toList();
		Map<String, Map<String, Object>> outputsMap = (Map<String, Map<String, Object>>) data.get("outputs");
		List<Variable> outputs = outputsMap.entrySet().stream().map(entry -> {
			String varName = entry.getKey();
			String difyType = (String) entry.getValue().get("type");
			VariableType varType = Optional.ofNullable(VariableType.difyValueOf(difyType))
				.orElseThrow(() -> new IllegalArgumentException("Unsupported variable type: " + difyType));
			return new Variable(varName, varType.value());
		}).toList();

		return new CodeNodeData(inputs, outputs).setCode((String) data.get("code"))
			.setCodeLanguage((String) data.get("code_language"));
	}

	@Override
	public Map<String, Object> dumpDifyData(CodeNodeData nodeData) {
		Map<String, Object> data = new HashMap<>();
		data.put("code", nodeData.getCode());
		data.put("code_language", nodeData.getCodeLanguage());
		List<Map<String, Object>> inputVars = new ArrayList<>();
		nodeData.getInputs().forEach(v -> {
			inputVars.add(Map.of("variable", v.getLabel(), "value_selector", List.of(v.getNamespace(), v.getName())));
		});
		data.put("variables", inputVars);
		Map<String, Object> outputVars = new HashMap<>();
		nodeData.getOutputs().forEach(variable -> {
			outputVars.put(variable.getName(),
					Map.of("type", VariableType.valueOf(variable.getValueType()).difyValue()));
		});
		data.put("outputs", outputVars);
		return data;
	}

}
