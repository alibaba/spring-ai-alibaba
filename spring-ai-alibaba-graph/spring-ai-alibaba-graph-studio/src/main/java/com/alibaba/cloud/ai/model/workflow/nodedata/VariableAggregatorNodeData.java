package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/***
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class VariableAggregatorNodeData extends NodeData {

	private List<List<String>> variables;
	private String output_type;
	private AdvancedSettings advanced_settings;

	@Builder
	public VariableAggregatorNodeData(String type, String title, String desc, boolean selected,
									  List<VariableSelector> inputs, List<Variable> outputs,
									  List<List<String>> variables, String output_type,
									  AdvancedSettings advanced_settings) {
		super(type, title, desc, selected, inputs, outputs);
		this.variables = variables;
		this.output_type = output_type;
		this.advanced_settings = advanced_settings;
	}

	@Data
	public static class Groups{
		private String output_type;
		private List<List<String>> variables;
		private String group_name;
		private String groupId;
	}

	@Data
	public static class AdvancedSettings {
		private boolean group_enabled;
		private List<Groups> groups;
	}


}
