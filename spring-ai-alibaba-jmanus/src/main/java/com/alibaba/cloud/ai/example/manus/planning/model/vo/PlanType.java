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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

/**
 * 计划类型枚举
 */
public enum PlanType {

	/**
	 * 传统的简单执行计划
	 */
	SIMPLE("简单计划", "适用于基本的任务执行，步骤按顺序进行"),

	/**
	 * MapReduce模式的执行计划
	 */
	MAPREDUCE("MapReduce计划", "适用于复杂的分布式任务，支持并行处理和结果聚合");

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
