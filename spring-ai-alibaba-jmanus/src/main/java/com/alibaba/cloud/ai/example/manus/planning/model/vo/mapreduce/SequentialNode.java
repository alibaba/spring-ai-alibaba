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

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 顺序执行节点
 */
public class SequentialNode extends AbstractExecutionNode {

	private List<ExecutionStep> steps;

	public SequentialNode() {
		super(MapReduceStepType.SEQUENTIAL);
		this.steps = new ArrayList<>();
	}

	public SequentialNode(List<ExecutionStep> steps) {
		super(MapReduceStepType.SEQUENTIAL);
		this.steps = steps != null ? steps : new ArrayList<>();
	}

	/**
	 * 获取节点类型的字符串表示，用于 Jackson 序列化/反序列化
	 * @return 类型字符串
	 */
	@JsonProperty("type")
	public String getTypeString() {
		return "sequential";
	}

	/**
	 * 设置节点类型，用于 Jackson 反序列化，实际不执行任何操作
	 * @param typeString 类型字符串
	 */
	@JsonProperty("type")
	public void setTypeString(String typeString) {
		// 反序列化时忽略此字段，类型已在构造函数中设置
	}

	public List<ExecutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ExecutionStep> steps) {
		this.steps = steps != null ? steps : new ArrayList<>();
	}

	public void addStep(ExecutionStep step) {
		if (steps == null) {
			steps = new ArrayList<>();
		}
		steps.add(step);
	}

	public void removeStep(ExecutionStep step) {
		if (steps != null) {
			steps.remove(step);
		}
	}

	public ExecutionStep removeStep(int index) {
		if (steps != null && index >= 0 && index < steps.size()) {
			return steps.remove(index);
		}
		return null;
	}

	@JsonIgnore
	public int getStepCount() {
		return steps != null ? steps.size() : 0;
	}

	public ExecutionStep getStep(int index) {
		if (steps != null && index >= 0 && index < steps.size()) {
			return steps.get(index);
		}
		return null;
	}

	@Override
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		return steps != null ? new ArrayList<>(steps) : new ArrayList<>();
	}

	/**
	 * 获取节点的字符串表示
	 * @return 节点字符串
	 */
	@JsonIgnore
	public String getNodeInStr() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== 顺序执行节点 ===\n");
		sb.append("步骤数量: ").append(getStepCount()).append("\n");

		if (steps != null) {
			for (int i = 0; i < steps.size(); i++) {
				ExecutionStep step = steps.get(i);
				sb.append("  ").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getNodeInStr();
	}

}
