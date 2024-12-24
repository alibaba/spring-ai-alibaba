package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/***
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
@Data
public class VariableAggregatorNodeData extends NodeData {

	private List<List<String>> variables;

	private String outputType;

	private AdvancedSettings advancedSettings;

	@Builder
	public VariableAggregatorNodeData(List<VariableSelector> inputs, List<Variable> outputs,
			List<List<String>> variables, String outputType, AdvancedSettings advancedSettings) {
		super(inputs, outputs);
		this.variables = variables;
		this.outputType = outputType;
		this.advancedSettings = advancedSettings;
	}

	@Data
	public static class Groups {

		private String outputType;

		private List<List<String>> variables;

		private String groupName;

		private String groupId;

	}

	@Data
	public static class AdvancedSettings {

		private boolean groupEnabled;

		private List<Groups> groups;

	}

}
