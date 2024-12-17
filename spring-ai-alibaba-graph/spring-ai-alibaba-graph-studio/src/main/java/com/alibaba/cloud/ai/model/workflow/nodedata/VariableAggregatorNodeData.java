package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableAggregatorNodeData extends NodeData {

	private String type;
	private String title;
	private String desc;
	private List<List<String>> variables;
	@JsonProperty("output_type")
	private String output_type;
	private boolean selected;
	@JsonProperty("advanced_settings")
	private AdvancedSettings advanced_settings;

	public VariableAggregatorNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}



	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Groups{
		@JsonProperty("output_type")
		private String output_type;
		private List<List<String>> variables;
		@JsonProperty("group_name")
		private String group_name;
		private String groupId;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class AdvancedSettings {
		@JsonProperty("group_enabled")
		private boolean group_enabled;
		private List<Groups> groups;
	}


}
