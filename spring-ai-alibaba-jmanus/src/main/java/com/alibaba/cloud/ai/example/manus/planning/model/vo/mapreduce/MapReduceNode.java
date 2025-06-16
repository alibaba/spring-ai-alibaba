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

import java.util.ArrayList;
import java.util.List;

/**
 * MapReduce执行节点
 */
public class MapReduceNode {

	private MapReduceStepType type = MapReduceStepType.MAPREDUCE;

	private List<ExecutionStep> mapSteps;

	private List<ExecutionStep> reduceSteps;

	public MapReduceNode() {
		this.mapSteps = new ArrayList<>();
		this.reduceSteps = new ArrayList<>();
	}

	public MapReduceNode(List<ExecutionStep> mapSteps, List<ExecutionStep> reduceSteps) {
		this.mapSteps = mapSteps != null ? mapSteps : new ArrayList<>();
		this.reduceSteps = reduceSteps != null ? reduceSteps : new ArrayList<>();
	}

	@JsonIgnore
	public MapReduceStepType getType() {
		return type;
	}

	public List<ExecutionStep> getMapSteps() {
		return mapSteps;
	}

	public void setMapSteps(List<ExecutionStep> mapSteps) {
		this.mapSteps = mapSteps != null ? mapSteps : new ArrayList<>();
	}

	public List<ExecutionStep> getReduceSteps() {
		return reduceSteps;
	}

	public void setReduceSteps(List<ExecutionStep> reduceSteps) {
		this.reduceSteps = reduceSteps != null ? reduceSteps : new ArrayList<>();
	}

	public void addMapStep(ExecutionStep step) {
		if (mapSteps == null) {
			mapSteps = new ArrayList<>();
		}
		mapSteps.add(step);
	}

	public void addReduceStep(ExecutionStep step) {
		if (reduceSteps == null) {
			reduceSteps = new ArrayList<>();
		}
		reduceSteps.add(step);
	}

	@JsonIgnore
	public int getMapStepCount() {
		return mapSteps != null ? mapSteps.size() : 0;
	}

	@JsonIgnore
	public int getReduceStepCount() {
		return reduceSteps != null ? reduceSteps.size() : 0;
	}

	@JsonIgnore
	public int getTotalStepCount() {
		return getMapStepCount() + getReduceStepCount();
	}

	@JsonIgnore
	public ExecutionStep getMapStep(int index) {
		if (mapSteps != null && index >= 0 && index < mapSteps.size()) {
			return mapSteps.get(index);
		}
		return null;
	}

	public ExecutionStep getReduceStep(int index) {
		if (reduceSteps != null && index >= 0 && index < reduceSteps.size()) {
			return reduceSteps.get(index);
		}
		return null;
	}

	/**
	 * 获取所有步骤（Map + Reduce）
	 * @return 所有步骤的列表
	 */
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		List<ExecutionStep> allSteps = new ArrayList<>();
		if (mapSteps != null) {
			allSteps.addAll(mapSteps);
		}
		if (reduceSteps != null) {
			allSteps.addAll(reduceSteps);
		}
		return allSteps;
	}

	/**
	 * 获取节点的字符串表示
	 * @return 节点字符串
	 */
	@JsonIgnore
	public String getNodeInStr() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== MapReduce执行节点 ===\n");
		sb.append("Map步骤数量: ").append(getMapStepCount()).append("\n");
		sb.append("Reduce步骤数量: ").append(getReduceStepCount()).append("\n");
		sb.append("总步骤数量: ").append(getTotalStepCount()).append("\n");

		if (mapSteps != null && !mapSteps.isEmpty()) {
			sb.append("\n--- Map阶段 ---\n");
			for (int i = 0; i < mapSteps.size(); i++) {
				ExecutionStep step = mapSteps.get(i);
				sb.append("  Map-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		if (reduceSteps != null && !reduceSteps.isEmpty()) {
			sb.append("\n--- Reduce阶段 ---\n");
			for (int i = 0; i < reduceSteps.size(); i++) {
				ExecutionStep step = reduceSteps.get(i);
				sb.append("  Reduce-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getNodeInStr();
	}

}
