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

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * MapReduce execution node
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
	 * Get string representation of node type for Jackson serialization/deserialization
	 * @return Type string
	 */
	@JsonProperty("type")
	public String getTypeString() {
		return "mapreduce";
	}

	/**
	 * Set node type for Jackson deserialization, actually performs no operation
	 * @param typeString Type string
	 */
	@JsonProperty("type")
	public void setTypeString(String typeString) {
		// Ignore this field during deserialization, type is already set in constructor
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
	 * Get all steps (Data Prepared + Map + Reduce + Post Process)
	 * @return List of all steps
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
	 * Get string representation of the node
	 * @return Node string
	 */
	@JsonIgnore
	public String getNodeInStr() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== MapReduce Execution Node ===\n");
		sb.append("Data Prepared Step Count: ").append(getDataPreparedStepCount()).append("\n");
		sb.append("Map Step Count: ").append(getMapStepCount()).append("\n");
		sb.append("Reduce Step Count: ").append(getReduceStepCount()).append("\n");
		sb.append("Post Process Step Count: ").append(getPostProcessStepCount()).append("\n");
		sb.append("Total Step Count: ").append(getTotalStepCount()).append("\n");

		if (dataPreparedSteps != null && !dataPreparedSteps.isEmpty()) {
			sb.append("\n--- Data Prepared Phase ---\n");
			for (int i = 0; i < dataPreparedSteps.size(); i++) {
				ExecutionStep step = dataPreparedSteps.get(i);
				sb.append("  DataPrepared-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		if (mapSteps != null && !mapSteps.isEmpty()) {
			sb.append("\n--- Map Phase ---\n");
			for (int i = 0; i < mapSteps.size(); i++) {
				ExecutionStep step = mapSteps.get(i);
				sb.append("  Map-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		if (reduceSteps != null && !reduceSteps.isEmpty()) {
			sb.append("\n--- Reduce Phase ---\n");
			for (int i = 0; i < reduceSteps.size(); i++) {
				ExecutionStep step = reduceSteps.get(i);
				sb.append("  Reduce-").append(i + 1).append(". ").append(step.getStepRequirement()).append("\n");
			}
		}

		if (postProcessSteps != null && !postProcessSteps.isEmpty()) {
			sb.append("\n--- Post Process Phase ---\n");
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

	@Override
	public String getResult() {
		// Use the result from the last postProcessSteps
		if (postProcessSteps != null && !postProcessSteps.isEmpty()) {
			ExecutionStep lastPostProcessStep = postProcessSteps.get(postProcessSteps.size() - 1);
			if (lastPostProcessStep != null && lastPostProcessStep.getResult() != null) {
				return lastPostProcessStep.getResult();
			}
		}
		// Return null if no postProcessSteps or no result available
		return null;
	}

}
