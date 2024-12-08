package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeData;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeType;
import com.alibaba.cloud.ai.model.workflow.node.data.CodeNodeData;
import com.alibaba.cloud.ai.service.dsl.WorkflowNodeDataConverter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CodeNodeDataConverter implements WorkflowNodeDataConverter {

	@Override
	public Boolean supportType(String nodeType) {
		return WorkflowNodeType.CODE.value().equals(nodeType);
	}

	@Override
	public WorkflowNodeData parseDifyData(Map<String, Object> data) {
		List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("variables");
		List<VariableSelector> inputs = variables.stream().map(variable -> {
			String[] selector = (String[]) variable.get("value_selector");
			return new VariableSelector(selector[0], selector[1], (String) variable.get("variable"));
		}).toList();
		Map<String, Map<String, Object>> outputsMap = (Map<String, Map<String, Object>>) data.get("outputs");
		List<Variable> outputs = outputsMap.entrySet().stream().map(entry -> {
			String varName = entry.getKey();
			String difyType = (String) entry.getValue().get("type");
			VariableType varType = Optional.ofNullable(VariableType.difyValueOf(difyType))
				.orElseThrow(() -> new IllegalArgumentException("Unsupported variable type: " + difyType));
			return new Variable(varName, varType.value());
		}).toList();

		return new CodeNodeData(inputs, outputs).setCode((String) data.get("code")).setCodeLanguage("code_language");
	}

	@Override
	public Map<String, Object> dumpDifyData(WorkflowNodeData nodeData) {
		CodeNodeData codeNodeData = (CodeNodeData) nodeData;
		Map<String, Object> data = new HashMap<>();
		data.put("code", codeNodeData.getCode());
		data.put("code_language", codeNodeData.getCodeLanguage());
		List<Map<String, Object>> inputVars = new ArrayList<>();
		nodeData.getInputs().forEach(v -> {
			inputVars
				.add(Map.of("variable", v.getLabel(), "value_selector", List.of(v.getNamespace(), v.getNamespace())));
		});
		data.put("variables", inputVars);
		Map<String, Object> outputVars = new HashMap<>();
		nodeData.getOutputs().forEach(variable -> {
			outputVars.put(variable.getName(),
					Map.of("type", VariableType.valueOf(variable.getValueType()).difyValue(), "children", "null"));
		});
		data.put("outputs", outputVars);
		return data;
	}

}
