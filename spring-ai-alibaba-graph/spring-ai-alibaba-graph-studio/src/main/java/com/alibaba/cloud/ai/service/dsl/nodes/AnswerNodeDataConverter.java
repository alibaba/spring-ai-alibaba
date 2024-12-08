package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeData;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeType;
import com.alibaba.cloud.ai.model.workflow.node.data.AnswerNodeData;
import com.alibaba.cloud.ai.service.dsl.WorkflowNodeDataConverter;
import com.alibaba.cloud.ai.utils.StringTemplateUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnswerNodeDataConverter implements WorkflowNodeDataConverter {

	@Override
	public Boolean supportType(String nodeType) {
		return WorkflowNodeType.ANSWER.value().equals(nodeType);
	}

	@Override
	public WorkflowNodeData parseDifyData(Map<String, Object> data) {
		String difyTmpl = (String) data.get("answer");
		List<String> variables = new ArrayList<>();
		String tmpl = StringTemplateUtil.fromDifyTmpl(difyTmpl, variables);
		List<VariableSelector> inputs = variables.stream().map(variable -> {
			String[] splits = variable.split("\\.", 2);
			return new VariableSelector(splits[0], splits[1]);
		}).toList();
		return new AnswerNodeData(inputs, AnswerNodeData.DEFAULT_OUTPUTS).setAnswer(tmpl);
	}

	@Override
	public Map<String, Object> dumpDifyData(WorkflowNodeData nodeData) {
		AnswerNodeData answerNodeData = (AnswerNodeData) nodeData;
		Map<String, Object> data = new HashMap<>();
		String difyTmpl = StringTemplateUtil.toDifyTmpl(answerNodeData.getAnswer());
		data.put("answer", difyTmpl);
		return data;
	}

}
