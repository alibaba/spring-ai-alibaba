package com.alibaba.cloud.ai.model.workflow.node;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodeData {

	// variable name -> variable ref
	protected List<VariableSelector> inputs;

	protected List<Variable> outputs;

}
