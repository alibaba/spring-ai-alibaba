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
 * MapReduce执行节点
 */
public class MapReduceNode extends AbstractExecutionNode {

	private List<ExecutionStep> dataPreparedSteps;

	private List<ExecutionStep> mapSteps;

	private List<ExecutionStep> reduceSteps;

	private List<ExecutionStep> postProcessSteps;

	public MapReduceNode() {
		super(MapReduceStepType.MAPREDUCE);
		this.dataPreparedSteps = new ArrayList<>();
		this.mapSteps = new ArrayList<>();
		this.reduceSteps = new ArrayList<>();
		this.postProcessSteps = new ArrayList<>();
	}

	public MapReduceNode(List<ExecutionStep> dataPreparedSteps, List<ExecutionStep> mapSteps,
			List<ExecutionStep> reduceSteps) {
		super(MapReduceStepType.MAPREDUCE);
		this.dataPreparedSteps = dataPreparedSteps != null ? dataPreparedSteps : new ArrayList<>();
		this.mapSteps = mapSteps != null ? mapSteps : new ArrayList<>();
		this.reduceSteps = reduceSteps != null ? reduceSteps : new ArrayList<>();
		this.postProcessSteps = new ArrayList<>();
	}

	public MapReduceNode(List<ExecutionStep> dataPreparedSteps, List<ExecutionStep> mapSteps,
			List<ExecutionStep> reduceSteps, List<ExecutionStep> postProcessSteps) {
		super(MapReduceStepType.MAPREDUCE);
		this.dataPreparedSteps = dataPreparedSteps != null ? dataPreparedSteps : new ArrayList<>();
		this.mapSteps = mapSteps != null ? mapSteps : new ArrayList<>();
		this.reduceSteps = reduceSteps != null ? reduceSteps : new ArrayList<>();
		this.postProcessSteps = postProcessSteps != null ? postProcessSteps : new ArrayList<>();
	}

	/**
	 * 获取节点类型的字符串表示，用于 Jackson 序列化/反序列化
	 * @return 类型字符串
	 */
	@JsonProperty("type")
	public String getTypeString() {
		return "mapreduce";
	}

	/**
	 * 设置节点类型，用于 Jackson 反序列化，实际不执行任何操作
	 * @param typeString 类型字符串
	 */
	@JsonProperty("type")
	public void setTypeString(String typeString) {
		// 反序列化时忽略此字段，类型已在构造函数中设置
	}

	public List<ExecutionStep> getDataPreparedSteps() {
		return dataPreparedSteps;
	}

	public void setDataPreparedSteps(List<ExecutionStep> dataPreparedSteps) {
		this.dataPreparedSteps = dataPreparedSteps != null ? dataPreparedSteps : new ArrayList<>();
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

	public List<ExecutionStep> getPostProcessSteps() {
		return postProcessSteps;
	}

	public void setPostProcessSteps(List<ExecutionStep> postProcessSteps) {
		this.postProcessSteps = postProcessSteps != null ? postProcessSteps : new ArrayList<>();
	}

	public void addDataPreparedStep(ExecutionStep step) {
		if (dataPreparedSteps == null) {
			dataPreparedSteps = new ArrayList<>();
		}
		dataPreparedSteps.add(step);
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

	public void addPostProcessStep(ExecutionStep step) {
		if (postProcessSteps == null) {
			postProcessSteps = new ArrayList<>();
		}
		postProcessSteps.add(step);
	}

	@JsonIgnore
	public int getDataPreparedStepCount() {
		return dataPreparedSteps != null ? dataPreparedSteps.size() : 0;
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
	public int getPostProcessStepCount() {
		return postProcessSteps != null ? postProcessSteps.size() : 0;
	}

	@JsonIgnore
	public int getTotalStepCount() {
		return getDataPreparedStepCount() + getMapStepCount() + getReduceStepCount() + getPostProcessStepCount();
	}

	@JsonIgnore
	public ExecutionStep getDataPreparedStep(int index) {
		if (dataPreparedSteps != null && index >= 0 && index < dataPreparedSteps.size()) {
			return dataPreparedSteps.get(index);
		}
		return null;
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

	@JsonIgnore
	public ExecutionStep getPostProcessStep(int index) {
		if (postProcessSteps != null && index >= 0 && index < postProcessSteps.size()) {
			return postProcessSteps.get(index);
		}
		return null;
	}

	/**
	 * 获取所有步骤（Data Prepared + Map + Reduce + Post Process）
	 * @return 所有步骤的列表
	 */
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		List<ExecutionStep> allSteps = new ArrayList<>();
		if (dataPreparedSteps != null) {
			allSteps.addAll(dataPreparedSteps);
		}
		if (mapSteps != null) {
			allSteps.addAll(mapSteps);
		}
		if (reduceSteps != null) {
			allSteps.addAll(reduceSteps);
		}
		if (postProcessSteps != null) {
			allSteps.addAll(postProcessSteps);
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
		sb.append("Data Prepared步骤数量: ").append(getDataPreparedStepCount()).append("\n");
		sb.append("Map步骤数量: ").append(getMapStepCount()).append("\n");
		sb.append("Reduce步骤数量: ").append(getReduceStepCount()).append("\n");
		sb.append("Post Process步骤数量: ").append(getPostProcessStepCount()).append("\n");
		sb.append("总步骤数量: ").append(getTotalStepCount()).append("\n");

		if (dataPreparedSteps != null && !dataPreparedSteps.isEmpty()) {
			sb.append("\n--- Data Prepared阶段 ---\n");
			for (int i = 0; i < dataPreparedSteps.size(); i++) {
				ExecutionStep step = dataPreparedSteps.get(i);
				sb.append("  DataPrepared-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

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

		if (postProcessSteps != null && !postProcessSteps.isEmpty()) {
			sb.append("\n--- Post Process阶段 ---\n");
			for (int i = 0; i < postProcessSteps.size(); i++) {
				ExecutionStep step = postProcessSteps.get(i);
				sb.append("  PostProcess-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getNodeInStr();
	}

}
