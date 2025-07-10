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
package com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce;

/**
 * MapReduce步骤类型枚举
 */
public enum MapReduceStepType {

	/**
	 * 顺序执行步骤
	 */
	SEQUENTIAL("顺序执行", "sequential"),

	/**
	 * MapReduce模式执行步骤
	 */
	MAPREDUCE("MapReduce模式", "mapreduce");

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
	 * 获取 JSON 序列化时使用的类型名称
	 * @return JSON 类型名称
	 */
	public String getJsonTypeName() {
		return jsonTypeName;
	}

	@Override
	public String toString() {
		return description;
	}

}
