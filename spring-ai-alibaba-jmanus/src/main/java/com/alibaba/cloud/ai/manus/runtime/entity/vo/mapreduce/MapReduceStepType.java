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
package com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce;

/**
 * MapReduce step type enumeration
 */
public enum MapReduceStepType {

	/**
	 * Sequential execution step
	 */
	SEQUENTIAL("Sequential Execution", "sequential"),

	/**
	 * MapReduce mode execution step
	 */
	MAPREDUCE("MapReduce Mode", "mapreduce");

	private final String description;

	private final String jsonTypeName;

	MapReduceStepType(String description, String jsonTypeName) {
		this.description = description;
		this.jsonTypeName = jsonTypeName;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Get type name used for JSON serialization
	 * @return JSON type name
	 */
	public String getJsonTypeName() {
		return jsonTypeName;
	}

	@Override
	public String toString() {
		return description;
	}

}
