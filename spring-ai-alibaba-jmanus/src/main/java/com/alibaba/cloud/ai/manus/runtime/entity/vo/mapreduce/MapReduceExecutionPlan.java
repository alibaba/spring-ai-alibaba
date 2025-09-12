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

import com.alibaba.cloud.ai.manus.agent.AgentState;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.AbstractExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * MapReduce mode execution plan
 */
public class MapReduceExecutionPlan extends AbstractExecutionPlan {

	private List<ExecutionNode> steps; // Store SequentialNode or MapReduceNode

	@JsonIgnore
	private long createdTime;

	/**
	 * Plan type for Jackson polymorphic deserialization
	 */
	private String planType = "advanced";

	public MapReduceExecutionPlan() {
		super();
		this.steps = new ArrayList<>();
		this.createdTime = System.currentTimeMillis();
	}

	public MapReduceExecutionPlan(String currentPlanId, String rootPlanId, String title) {
		super(currentPlanId, rootPlanId, title);
		this.steps = new ArrayList<>();
		this.createdTime = System.currentTimeMillis();
	}

	public String getPlanType() {
		return planType;
	}

	public void setPlanType(String planType) {
		this.planType = planType;
	}

	/**
	 * Get step node list (more semantic method name)
	 * @return Step node list
	 */
	public List<ExecutionNode> getSteps() {
		return steps;
	}

	/**
	 * Set step node list (more semantic method name)
	 * @param steps Step node list
	 */
	public void setSteps(List<ExecutionNode> steps) {
		this.steps = steps;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	// Implementation of AbstractExecutionPlan abstract method

	@Override
	protected void clearSteps() {
		clearNodes();
	}

	/**
	 * Add sequential execution node
	 * @param node Sequential execution node
	 */
	public void addSequentialNode(SequentialNode node) {
		steps.add(node);
	}

	/**
	 * Add MapReduce execution node
	 * @param node MapReduce execution node
	 */
	public void addMapReduceNode(MapReduceNode node) {
		steps.add(node);
	}

	/**
	 * Get node count
	 * @return Total node count
	 */
	@JsonIgnore
	public int getNodeCount() {
		return steps.size();
	}

	/**
	 * Get node by index
	 * @param index Node index
	 * @return Node object, return null if index is invalid
	 */
	public ExecutionNode getNode(int index) {
		if (index >= 0 && index < steps.size()) {
			return steps.get(index);
		}
		return null;
	}

	/**
	 * Get all execution steps (flatten steps from all nodes). Return in execution order:
	 * Data Preparation → Map → Reduce
	 * @return List of all execution steps
	 */
	@Override
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		// Estimate total step count to optimize performance

		List<ExecutionStep> allSteps = new ArrayList<>();

		// Use interface method to directly get all steps from each node
		for (ExecutionNode node : steps) {
			List<ExecutionStep> nodeSteps = node.getAllSteps();
			if (nodeSteps != null && !nodeSteps.isEmpty()) {
				allSteps.addAll(nodeSteps);
			}
		}

		return allSteps;
	}

	/**
	 * Get total step count
	 * @return Total step count
	 */
	@Override
	@JsonIgnore
	public int getTotalStepCount() {
		return getAllSteps().size();
	}

	/**
	 * Get sequential node list
	 * @return Sequential node list
	 */
	@JsonIgnore
	public List<SequentialNode> getSequentialNodes() {
		List<SequentialNode> sequentialNodes = new ArrayList<>();
		for (ExecutionNode node : steps) {
			if (node instanceof SequentialNode) {
				sequentialNodes.add((SequentialNode) node);
			}
		}
		return sequentialNodes;
	}

	/**
	 * Get MapReduce node list
	 * @return MapReduce node list
	 */
	@JsonIgnore
	public List<MapReduceNode> getMapReduceNodes() {
		List<MapReduceNode> mapReduceNodes = new ArrayList<>();
		for (ExecutionNode node : steps) {
			if (node instanceof MapReduceNode) {
				mapReduceNodes.add((MapReduceNode) node);
			}
		}
		return mapReduceNodes;
	}

	/**
	 * Get plan execution status in string format
	 * @param onlyCompletedAndFirstInProgress When true, only output all completed steps
	 * and first in-progress step
	 * @return Plan status string
	 */
	@Override
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder sb = new StringBuilder();
		sb.append("=== MapReduce Execution Plan ===\n");
		sb.append("Plan ID: ").append(currentPlanId).append("\n");
		sb.append("Root Plan ID: ").append(rootPlanId).append("\n");
		sb.append("Title: ").append(title).append("\n");
		sb.append("Created Time: ").append(new java.util.Date(createdTime)).append("\n");
		sb.append("Node Count: ").append(steps.size()).append("\n");
		sb.append("Total Step Count: ").append(getTotalStepCount()).append("\n");
		sb.append("\n");

		for (int i = 0; i < steps.size(); i++) {
			ExecutionNode node = steps.get(i);
			sb.append("--- Node ").append(i + 1).append(" ---\n");

			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				sb.append("Type: Sequential Execution\n");
				sb.append("Step Count: ").append(seqNode.getStepCount()).append("\n");

				if (seqNode.getSteps() != null) {
					for (ExecutionStep step : seqNode.getSteps()) {
						sb.append("  ").append(step.getStepInStr()).append("\n");
						if (!onlyCompletedAndFirstInProgress && step.getResult() != null) {
							sb.append("    Result: ").append(step.getResult()).append("\n");
						}
					}
				}
			}
			else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				// sb.append("Type: MapReduce\n");
				// sb.append("Data Preparation Step Count:
				// ").append(mrNode.getDataPreparedStepCount()).append("\n");
				// sb.append("Map Step Count:
				// ").append(mrNode.getMapStepCount()).append("\n");
				// sb.append("Reduce Step Count:
				// ").append(mrNode.getReduceStepCount()).append("\n");
				// sb.append("Post Process Step Count:
				// ").append(mrNode.getPostProcessStepCount()).append("\n");

				// When onlyCompletedAndFirstInProgress = true, need to determine current
				// execution phase
				boolean showReduceAndPostProcess = true;
				if (onlyCompletedAndFirstInProgress) {
					// Check if currently executing first step of Map phase
					boolean isMapPhaseFirstStep = isExecutingMapPhaseFirstStep(mrNode);
					if (isMapPhaseFirstStep) {
						showReduceAndPostProcess = false;
					}
				}

				// Data preparation phase
				if (mrNode.getDataPreparedSteps() != null && !mrNode.getDataPreparedSteps().isEmpty()) {
					sb.append("  Data Preparation Phase:\n");
					appendFilteredSteps(sb, mrNode.getDataPreparedSteps(), onlyCompletedAndFirstInProgress);
				}

				// Map phase
				if (mrNode.getMapSteps() != null && !mrNode.getMapSteps().isEmpty()) {
					sb.append("  Map Phase:\n");
					appendFilteredSteps(sb, mrNode.getMapSteps(), onlyCompletedAndFirstInProgress);
				}

				// Reduce phase - decide whether to show based on conditions
				if (showReduceAndPostProcess && mrNode.getReduceSteps() != null && !mrNode.getReduceSteps().isEmpty()) {
					sb.append("  Reduce Phase:\n");
					appendFilteredSteps(sb, mrNode.getReduceSteps(), onlyCompletedAndFirstInProgress);
				}

				// Post process phase - decide whether to show based on conditions
				if (showReduceAndPostProcess && mrNode.getPostProcessSteps() != null
						&& !mrNode.getPostProcessSteps().isEmpty()) {
					sb.append("  Post Process Phase:\n");
					appendFilteredSteps(sb, mrNode.getPostProcessSteps(), onlyCompletedAndFirstInProgress);
				}
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Clear all nodes
	 */
	public void clearNodes() {
		steps.clear();
	}

	/**
	 * Check if plan is empty
	 * @return Returns true if no nodes exist
	 */
	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return steps.isEmpty();
	}

	// PlanInterface implementation methods

	@Override
	public void addStep(ExecutionStep step) {
		// For MapReduce plan, add to new sequential node by default
		SequentialNode newNode = new SequentialNode();
		newNode.addStep(step);
		addSequentialNode(newNode);
	}

	@Override
	public void removeStep(ExecutionStep step) {
		// Find and remove specified step from all nodes
		for (ExecutionNode node : steps) {
			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				seqNode.removeStep(step);
			}
			else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				mrNode.getDataPreparedSteps().remove(step);
				mrNode.getMapSteps().remove(step);
				mrNode.getReduceSteps().remove(step);
				mrNode.getPostProcessSteps().remove(step);
			}
		}
	}

	@Override
	public void clear() {
		clearNodes();
		planningThinking = null;
		executionParams = "";
	}

	/**
	 * Update indices of all steps, incrementing from 0 Set consecutive indices for all
	 * steps in MapReduce plan (including data preparation, Map and Reduce phases)
	 */
	@Override
	public void updateStepIndices() {
		int index = 0;
		for (ExecutionNode node : steps) {
			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				if (seqNode.getSteps() != null) {
					for (ExecutionStep step : seqNode.getSteps()) {
						step.setStepIndex(index++);
					}
				}
			}
			else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				// First set data preparation step indices
				if (mrNode.getDataPreparedSteps() != null) {
					for (ExecutionStep step : mrNode.getDataPreparedSteps()) {
						step.setStepIndex(index++);
					}
				}
				// Then set Map step indices
				if (mrNode.getMapSteps() != null) {
					for (ExecutionStep step : mrNode.getMapSteps()) {
						step.setStepIndex(index++);
					}
				}
				// Then set Reduce step indices
				if (mrNode.getReduceSteps() != null) {
					for (ExecutionStep step : mrNode.getReduceSteps()) {
						step.setStepIndex(index++);
					}
				}
				// Finally set post process step indices
				if (mrNode.getPostProcessSteps() != null) {
					for (ExecutionStep step : mrNode.getPostProcessSteps()) {
						step.setStepIndex(index++);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return getPlanExecutionStateStringFormat(false);
	}

	/**
	 * Add steps to string builder based on filter conditions
	 * @param sb String builder
	 * @param steps Step list
	 * @param onlyCompletedAndFirstNotStarted Whether to only show completed steps and
	 * first not started step
	 */
	private void appendFilteredSteps(StringBuilder sb, List<ExecutionStep> steps,
			boolean onlyCompletedAndFirstNotStarted) {
		boolean foundNotStarted = false;

		for (ExecutionStep step : steps) {
			if (onlyCompletedAndFirstNotStarted) {
				// If COMPLETED status, always show
				if (step.getStatus() == AgentState.COMPLETED) {
					sb.append("    ").append(step.getStepInStr()).append("\n");
					if (step.getResult() != null) {
						sb.append("      Result: ").append(step.getResult()).append("\n");
					}
				}
				// If NOT_STARTED status and haven't found other NOT_STARTED steps yet
				else if (step.getStatus() == AgentState.NOT_STARTED && !foundNotStarted) {
					foundNotStarted = true; // Mark that NOT_STARTED step has been found
					sb.append("    ").append(step.getStepInStr()).append("\n");
					// NOT_STARTED status steps usually have no results, so don't show
					// results
				}
				// All other cases (not COMPLETED and not first NOT_STARTED)
				// Skip steps that don't meet conditions
			}
			else {
				// Show all steps
				sb.append("    ").append(step.getStepInStr()).append("\n");
				if (step.getResult() != null) {
					sb.append("      Result: ").append(step.getResult()).append("\n");
				}
			}
		}
	}

	/**
	 * Determine if currently executing first step of Map phase
	 * @param mrNode MapReduce node
	 * @return Returns true if currently executing first step of Map phase
	 */
	private boolean isExecutingMapPhaseFirstStep(MapReduceNode mrNode) {
		// Check if data preparation phase is fully completed
		boolean dataPreparedCompleted = true;
		if (mrNode.getDataPreparedSteps() != null && !mrNode.getDataPreparedSteps().isEmpty()) {
			for (ExecutionStep step : mrNode.getDataPreparedSteps()) {
				if (step.getStatus() != AgentState.COMPLETED) {
					dataPreparedCompleted = false;
					break;
				}
			}
		}

		// Check if Map phase has first step in progress or about to execute
		boolean mapFirstStepInProgress = false;
		if (mrNode.getMapSteps() != null && !mrNode.getMapSteps().isEmpty()) {
			ExecutionStep firstMapStep = mrNode.getMapSteps().get(0);
			// If first Map step is NOT_STARTED status, it means about to execute
			if (firstMapStep.getStatus() == AgentState.NOT_STARTED) {
				mapFirstStepInProgress = true;
			}
		}

		// Only return true when data preparation phase is fully completed and first step
		// of Map phase is about to execute
		return dataPreparedCompleted && mapFirstStepInProgress;
	}

	@Override
	public String getResult() {
		// Get the result from the last node
		if (steps != null && !steps.isEmpty()) {
			ExecutionNode lastNode = steps.get(steps.size() - 1);
			if (lastNode != null) {
				return lastNode.getResult();
			}
		}
		// Return null if no nodes or no result available
		return null;
	}

}
