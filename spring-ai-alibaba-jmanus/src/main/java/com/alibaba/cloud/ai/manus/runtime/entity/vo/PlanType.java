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
package com.alibaba.cloud.ai.manus.runtime.entity.vo;

/**
 * Plan type enumeration
 */
public enum PlanType {

	/**
	 * Traditional simple execution plan
	 */
	SIMPLE("Simple Plan", "Suitable for basic task execution with sequential steps"),

	/**
	 * MapReduce mode execution plan
	 */
	MAPREDUCE("MapReduce Plan",
			"Suitable for complex distributed tasks with parallel processing and result aggregation");

	private final String displayName;

	private final String description;

	PlanType(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return displayName;
	}

}
