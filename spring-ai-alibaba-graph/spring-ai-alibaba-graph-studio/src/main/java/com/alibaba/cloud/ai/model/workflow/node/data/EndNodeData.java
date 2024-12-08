package com.alibaba.cloud.ai.model.workflow.node.data;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EndNodeData extends WorkflowNodeData {

	public static final List<Variable> DEFAULT_OUTPUTS = List.of(new Variable("output", VariableType.STRING.value()));

	public EndNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

}
