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

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.AbstractExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * MapReduce模式的执行计划
 */
public class MapReduceExecutionPlan extends AbstractExecutionPlan {

	private List<ExecutionNode> steps; // 存储 SequentialNode 或 MapReduceNode

	@JsonIgnore
	private long createdTime;

	/**
	 * 计划类型，用于 Jackson 多态反序列化
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
	 * 获取步骤节点列表（更语义化的方法名）
	 * @return 步骤节点列表
	 */
	public List<ExecutionNode> getSteps() {
		return steps;
	}

	/**
	 * 设置步骤节点列表（更语义化的方法名）
	 * @param steps 步骤节点列表
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

	// AbstractExecutionPlan 抽象方法的实现

	@Override
	protected void clearSteps() {
		clearNodes();
	}

	/**
	 * 添加顺序执行节点
	 * @param node 顺序执行节点
	 */
	public void addSequentialNode(SequentialNode node) {
		steps.add(node);
	}

	/**
	 * 添加MapReduce执行节点
	 * @param node MapReduce执行节点
	 */
	public void addMapReduceNode(MapReduceNode node) {
		steps.add(node);
	}

	/**
	 * 获取节点数量
	 * @return 节点总数
	 */
	@JsonIgnore
	public int getNodeCount() {
		return steps.size();
	}

	/**
	 * 根据索引获取节点
	 * @param index 节点索引
	 * @return 节点对象，如果索引无效则返回null
	 */
	public ExecutionNode getNode(int index) {
		if (index >= 0 && index < steps.size()) {
			return steps.get(index);
		}
		return null;
	}

	/**
	 * 获取所有执行步骤（展平所有节点中的步骤） 按照执行顺序返回：数据准备 → Map → Reduce
	 * @return 所有执行步骤的列表
	 */
	@Override
	@JsonIgnore
	public List<ExecutionStep> getAllSteps() {
		// 预估总步骤数以优化性能

		List<ExecutionStep> allSteps = new ArrayList<>();

		// 使用接口方法直接获取每个节点的所有步骤
		for (ExecutionNode node : steps) {
			List<ExecutionStep> nodeSteps = node.getAllSteps();
			if (nodeSteps != null && !nodeSteps.isEmpty()) {
				allSteps.addAll(nodeSteps);
			}
		}

		return allSteps;
	}

	/**
	 * 获取总步骤数量
	 * @return 总步骤数
	 */
	@Override
	@JsonIgnore
	public int getTotalStepCount() {
		return getAllSteps().size();
	}

	/**
	 * 获取顺序节点列表
	 * @return 顺序节点列表
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
	 * 获取MapReduce节点列表
	 * @return MapReduce节点列表
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
	 * 获取计划执行状态的字符串格式
	 * @param onlyCompletedAndFirstInProgress 当为true时，只输出所有已完成的步骤和第一个进行中的步骤
	 * @return 计划状态字符串
	 */
	@Override
	public String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress) {
		StringBuilder sb = new StringBuilder();
		sb.append("=== MapReduce执行计划 ===\n");
		sb.append("计划ID: ").append(currentPlanId).append("\n");
		sb.append("root计划ID: ").append(rootPlanId).append("\n");
		sb.append("标题: ").append(title).append("\n");
		sb.append("创建时间: ").append(new java.util.Date(createdTime)).append("\n");
		sb.append("节点数量: ").append(steps.size()).append("\n");
		sb.append("总步骤数: ").append(getTotalStepCount()).append("\n");
		sb.append("\n");

		for (int i = 0; i < steps.size(); i++) {
			ExecutionNode node = steps.get(i);
			sb.append("--- 节点 ").append(i + 1).append(" ---\n");

			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				sb.append("类型: 顺序执行\n");
				sb.append("步骤数: ").append(seqNode.getStepCount()).append("\n");

				if (seqNode.getSteps() != null) {
					for (ExecutionStep step : seqNode.getSteps()) {
						sb.append("  ").append(step.getStepInStr()).append("\n");
						if (!onlyCompletedAndFirstInProgress && step.getResult() != null) {
							sb.append("    结果: ").append(step.getResult()).append("\n");
						}
					}
				}
			}
			else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				sb.append("类型: MapReduce\n");
				sb.append("数据准备步骤数: ").append(mrNode.getDataPreparedStepCount()).append("\n");
				sb.append("Map步骤数: ").append(mrNode.getMapStepCount()).append("\n");
				sb.append("Reduce步骤数: ").append(mrNode.getReduceStepCount()).append("\n");
				sb.append("后处理步骤数: ").append(mrNode.getPostProcessStepCount()).append("\n");

				// 当 onlyCompletedAndFirstInProgress = true 时，需要判断当前执行阶段
				boolean showReduceAndPostProcess = true;
				if (onlyCompletedAndFirstInProgress) {
					// 检查是否正在执行 Map 阶段的第一个步骤
					boolean isMapPhaseFirstStep = isExecutingMapPhaseFirstStep(mrNode);
					if (isMapPhaseFirstStep) {
						showReduceAndPostProcess = false;
					}
				}

				// 数据准备阶段
				if (mrNode.getDataPreparedSteps() != null && !mrNode.getDataPreparedSteps().isEmpty()) {
					sb.append("  数据准备阶段:\n");
					appendFilteredSteps(sb, mrNode.getDataPreparedSteps(), onlyCompletedAndFirstInProgress);
				}

				// Map阶段
				if (mrNode.getMapSteps() != null && !mrNode.getMapSteps().isEmpty()) {
					sb.append("  Map阶段:\n");
					appendFilteredSteps(sb, mrNode.getMapSteps(), onlyCompletedAndFirstInProgress);
				}

				// Reduce阶段 - 根据条件决定是否显示
				if (showReduceAndPostProcess && mrNode.getReduceSteps() != null && !mrNode.getReduceSteps().isEmpty()) {
					sb.append("  Reduce阶段:\n");
					appendFilteredSteps(sb, mrNode.getReduceSteps(), onlyCompletedAndFirstInProgress);
				}

				// 后处理阶段 - 根据条件决定是否显示
				if (showReduceAndPostProcess && mrNode.getPostProcessSteps() != null
						&& !mrNode.getPostProcessSteps().isEmpty()) {
					sb.append("  后处理阶段:\n");
					appendFilteredSteps(sb, mrNode.getPostProcessSteps(), onlyCompletedAndFirstInProgress);
				}
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 清空所有节点
	 */
	public void clearNodes() {
		steps.clear();
	}

	/**
	 * 检查计划是否为空
	 * @return 如果没有节点则返回true
	 */
	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return steps.isEmpty();
	}

	// PlanInterface 实现方法

	@Override
	public void addStep(ExecutionStep step) {
		// 对于MapReduce计划，默认添加到新的顺序节点中
		SequentialNode newNode = new SequentialNode();
		newNode.addStep(step);
		addSequentialNode(newNode);
	}

	@Override
	public void removeStep(ExecutionStep step) {
		// 从所有节点中查找并移除指定步骤
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
	 * 更新所有步骤的索引，从0开始递增 为MapReduce计划中的所有步骤（包括数据准备、Map和Reduce阶段）设置连续的索引
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
				// 先设置数据准备步骤的索引
				if (mrNode.getDataPreparedSteps() != null) {
					for (ExecutionStep step : mrNode.getDataPreparedSteps()) {
						step.setStepIndex(index++);
					}
				}
				// 再设置Map步骤的索引
				if (mrNode.getMapSteps() != null) {
					for (ExecutionStep step : mrNode.getMapSteps()) {
						step.setStepIndex(index++);
					}
				}
				// 然后设置Reduce步骤的索引
				if (mrNode.getReduceSteps() != null) {
					for (ExecutionStep step : mrNode.getReduceSteps()) {
						step.setStepIndex(index++);
					}
				}
				// 最后设置后处理步骤的索引
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
	 * 根据过滤条件添加步骤到字符串构建器
	 * @param sb 字符串构建器
	 * @param steps 步骤列表
	 * @param onlyCompletedAndFirstNotStarted 是否只显示已完成的步骤和第一个未开始的步骤
	 */
	private void appendFilteredSteps(StringBuilder sb, List<ExecutionStep> steps,
			boolean onlyCompletedAndFirstNotStarted) {
		boolean foundNotStarted = false;

		for (ExecutionStep step : steps) {
			if (onlyCompletedAndFirstNotStarted) {
				// 如果是COMPLETED状态，始终显示
				if (step.getStatus() == AgentState.COMPLETED) {
					sb.append("    ").append(step.getStepInStr()).append("\n");
					if (step.getResult() != null) {
						sb.append("      结果: ").append(step.getResult()).append("\n");
					}
				}
				// 如果是NOT_STARTED状态，且还没找到其他NOT_STARTED的步骤
				else if (step.getStatus() == AgentState.NOT_STARTED && !foundNotStarted) {
					foundNotStarted = true; // 标记已找到NOT_STARTED步骤
					sb.append("    ").append(step.getStepInStr()).append("\n");
					// NOT_STARTED状态的步骤通常没有结果，所以不显示结果
				}
				// 其他所有情况（不是COMPLETED且不是第一个NOT_STARTED）
				// 跳过不符合条件的步骤
			}
			else {
				// 显示所有步骤
				sb.append("    ").append(step.getStepInStr()).append("\n");
				if (step.getResult() != null) {
					sb.append("      结果: ").append(step.getResult()).append("\n");
				}
			}
		}
	}

	/**
	 * 判断是否正在执行Map阶段的第一个步骤
	 * @param mrNode MapReduce节点
	 * @return 如果正在执行Map阶段的第一个步骤则返回true
	 */
	private boolean isExecutingMapPhaseFirstStep(MapReduceNode mrNode) {
		// 检查数据准备阶段是否全部完成
		boolean dataPreparedCompleted = true;
		if (mrNode.getDataPreparedSteps() != null && !mrNode.getDataPreparedSteps().isEmpty()) {
			for (ExecutionStep step : mrNode.getDataPreparedSteps()) {
				if (step.getStatus() != AgentState.COMPLETED) {
					dataPreparedCompleted = false;
					break;
				}
			}
		}

		// 检查Map阶段是否有第一个步骤正在执行或即将执行
		boolean mapFirstStepInProgress = false;
		if (mrNode.getMapSteps() != null && !mrNode.getMapSteps().isEmpty()) {
			ExecutionStep firstMapStep = mrNode.getMapSteps().get(0);
			// 如果第一个Map步骤是NOT_STARTED状态，说明即将执行
			if (firstMapStep.getStatus() == AgentState.NOT_STARTED) {
				mapFirstStepInProgress = true;
			}
		}

		// 只有当数据准备阶段全部完成，且Map阶段的第一个步骤即将执行时，才返回true
		return dataPreparedCompleted && mapFirstStepInProgress;
	}

}
