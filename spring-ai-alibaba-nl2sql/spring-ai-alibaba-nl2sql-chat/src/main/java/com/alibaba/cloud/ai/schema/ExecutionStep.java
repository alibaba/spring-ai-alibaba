package com.alibaba.cloud.ai.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class ExecutionStep {

	@JsonProperty("step")
	private int step;

	@JsonProperty("tool_to_use")
	private String toolToUse;

	@JsonProperty("tool_parameters")
	private ToolParameters toolParameters;

	@Data
	public static class ToolParameters{
		private String description;
		@JsonProperty("summary_and_recommendations")
		private String summaryAndRecommendations;
		@JsonProperty("sql_query")
		private String sqlQuery;
		@JsonProperty("instruction")
		private String instruction;
		@JsonProperty("input_data_description")
		private String inputDataDescription;
	}

	public ExecutionStep() {
	}

	public ExecutionStep(int step, String toolToUse, ToolParameters toolParameters) {
		this.step = step;
		this.toolToUse = toolToUse;
		this.toolParameters = toolParameters;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public String getToolToUse() {
		return toolToUse;
	}

	public void setToolToUse(String toolToUse) {
		this.toolToUse = toolToUse;
	}

	public ToolParameters getToolParameters() {
		return toolParameters;
	}

	public void setToolParameters(ToolParameters toolParameters) {
		this.toolParameters = toolParameters;
	}

	@Override
	public String toString() {
		return "ExecutionStep{" + "step=" + step + ", toolToUse='" + toolToUse + '\'' + ", toolParameters="
				+ toolParameters + '}';
	}

}
