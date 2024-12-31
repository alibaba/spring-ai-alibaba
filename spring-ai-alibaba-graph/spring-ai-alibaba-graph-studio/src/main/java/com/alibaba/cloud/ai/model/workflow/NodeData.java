package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * NodeData defines the behavior of a node. Each subclass represents the behavior of the
 * node.
 */
@Data
public class NodeData {

	/**
	 * The inputs of the node is the output reference of the previous node
	 */
	protected List<VariableSelector> inputs;

	/**
	 * The output variables of a node
	 */
	protected List<Variable> outputs;

	public NodeData() {

	}

	protected NodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
	}

}
