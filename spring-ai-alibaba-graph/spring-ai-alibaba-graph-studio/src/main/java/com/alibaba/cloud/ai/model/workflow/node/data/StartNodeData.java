package com.alibaba.cloud.ai.model.workflow.node.data;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class StartNodeData extends WorkflowNodeData {

	private List<StartInput> startInputs;

	public StartNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	@Data
	public static class StartInput {

		private String label;

		private String type;

		private String variable;

		private Integer maxLength;

		private List<String> options;

		private Boolean required;

	}

}
