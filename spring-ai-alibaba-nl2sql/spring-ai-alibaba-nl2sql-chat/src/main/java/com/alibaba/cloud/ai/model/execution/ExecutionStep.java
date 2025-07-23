/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.model.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

public class ExecutionStep {

	@JsonProperty("step")
	private int step;

	@JsonProperty("tool_to_use")
	private String toolToUse;

	@JsonProperty("tool_parameters")
	private ToolParameters toolParameters;

	@Data
	public static class ToolParameters {

		private String description;

		@JsonProperty("summary_and_recommendations")
		private String summaryAndRecommendations;

		@JsonProperty("sql_query")
		private String sqlQuery;

		@JsonProperty("instruction")
		private String instruction;

		@JsonProperty("input_data_description")
		private String inputDataDescription;

		// Manual getters and setters to ensure they are available
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getSummaryAndRecommendations() {
			return summaryAndRecommendations;
		}

		public void setSummaryAndRecommendations(String summaryAndRecommendations) {
			this.summaryAndRecommendations = summaryAndRecommendations;
		}

		public String getSqlQuery() {
			return sqlQuery;
		}

		public void setSqlQuery(String sqlQuery) {
			this.sqlQuery = sqlQuery;
		}

		public String getInstruction() {
			return instruction;
		}

		public void setInstruction(String instruction) {
			this.instruction = instruction;
		}

		public String getInputDataDescription() {
			return inputDataDescription;
		}

		public void setInputDataDescription(String inputDataDescription) {
			this.inputDataDescription = inputDataDescription;
		}

		public String toJsonStr() {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.writeValueAsString(this);
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to convert object to JSON string", e);
			}
		}

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
