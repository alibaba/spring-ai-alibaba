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
public class Plan{

	private String title;

	@JsonProperty("has_enough_context")
	private boolean hasEnoughContext;

	private String thought;

	private List<Step> steps;

	public Plan() {
	}

	public Plan(String title, boolean hasEnoughContext, String thought, List<Step> steps) {
		this.title = title;
		this.hasEnoughContext = hasEnoughContext;
		this.thought = thought;
		this.steps = steps;
	}

	public String getTitle() {
		return title;
	}

	public String getThought() {
		return thought;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setHasEnoughContext(boolean hasEnoughContext) {
		this.hasEnoughContext = hasEnoughContext;
	}

	public void setThought(String thought) {
		this.thought = thought;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public record Step(@JsonProperty("need_web_search")
					   boolean needWebSearch,
					   String title,
					   String description,
					   @JsonProperty("step_type")
					   StepType stepType,
					   String executionRes) {

		public Step {
		}

		public static Builder builder() {
			return new Builder();
		}

		public Builder mutate(){
			return new Builder()
					.needWebSearch(needWebSearch)
					.title(title)
					.description(description)
					.stepType(stepType)
					.executionRes(executionRes);
		}

		public static final class Builder {

			private boolean needWebSearch;
			private String title;
			private String description;
			private StepType stepType;
			private String executionRes;

			private Builder(){
			}

			public Builder needWebSearch(boolean needWebSearch) {
				this.needWebSearch = needWebSearch;
				return this;
			}

			public Builder title(String title) {
				this.title = title;
				return this;
			}

			public Builder description(String description) {
				this.description = description;
				return this;
			}

			public Builder stepType(StepType stepType) {
				this.stepType = stepType;
				return this;
			}

			public Builder executionRes(String executionRes) {
				this.executionRes = executionRes;
				return this;
			}

			public Step build() {
				return new Step(needWebSearch, title, description, stepType, executionRes);
			}

		}


	}

	public enum StepType {

		@JsonProperty("research")
		@JsonAlias("RESEARCH")
		RESEARCH, @JsonProperty("processing")
		@JsonAlias("PROCESSING")
		PROCESSING

	}

	public boolean isHasEnoughContext() {
		return hasEnoughContext;
	}

}