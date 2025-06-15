/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yingzi
 * @author ViliamSun
 * @since 2025/5/18 17:48
 */
public class Plan {

	private String title;

	@JsonProperty("has_enough_context")
	private boolean hasEnoughContext;

	private String thought;

	private List<Step> steps;

	public static class Step {

		@JsonProperty("need_web_search")
		private boolean needWebSearch;

		private String title;

		private String description;

		@JsonProperty("step_type")
		private StepType stepType;

		private String executionRes;

		private String executionStatus;

		public boolean isNeedWebSearch() {
			return needWebSearch;
		}

		public void setNeedWebSearch(boolean needWebSearch) {
			this.needWebSearch = needWebSearch;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public StepType getStepType() {
			return stepType;
		}

		public void setStepType(StepType stepType) {
			this.stepType = stepType;
		}

		public String getExecutionRes() {
			return executionRes;
		}

		public void setExecutionRes(String executionRes) {
			this.executionRes = executionRes;
		}

		public String getExecutionStatus() {
			return executionStatus;
		}

		public void setExecutionStatus(String executionStatus) {
			this.executionStatus = executionStatus;
		}

	}

	public enum StepType {

		@JsonProperty("research")
		@JsonAlias("RESEARCH")
		RESEARCH,

		@JsonProperty("processing")
		@JsonAlias("PROCESSING")
		PROCESSING

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getThought() {
		return thought;
	}

	public void setThought(String thought) {
		this.thought = thought;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public void setHasEnoughContext(boolean hasEnoughContext) {
		this.hasEnoughContext = hasEnoughContext;
	}

	public boolean isHasEnoughContext() {
		return hasEnoughContext;
	}

}
