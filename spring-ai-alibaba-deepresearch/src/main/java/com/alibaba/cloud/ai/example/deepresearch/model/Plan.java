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

package com.alibaba.cloud.ai.example.deepresearch.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author yingzi
 * @author ViliamSun
 * @since 2025/5/18 17:48
 */
@Data
public class Plan {

	private String title;

	@JsonProperty("has_enough_context")
	private boolean hasEnoughContext;

	private String thought;

	private List<Step> steps;

	@Data
	public static class Step {

		@JsonProperty("need_web_search")
		private boolean needWebSearch;

		private String title;

		private String description;

		@JsonProperty("step_type")
		private StepType stepType;

		private String executionRes;

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
