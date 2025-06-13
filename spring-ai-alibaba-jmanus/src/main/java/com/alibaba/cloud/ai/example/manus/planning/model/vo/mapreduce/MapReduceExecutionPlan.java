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

import com.alibaba.cloud.ai.example.manus.planning.model.vo.AbstractExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import java.util.ArrayList;
import java.util.List;

/**
 * MapReduce模式的执行计划
 */
public class MapReduceExecutionPlan extends AbstractExecutionPlan {

	private List<Object> nodes; // 存储 SequentialNode 或 MapReduceNode

	private long createdTime;

	public MapReduceExecutionPlan() {
		super();
		this.nodes = new ArrayList<>();
		this.createdTime = System.currentTimeMillis();
	}

	public MapReduceExecutionPlan(String planId, String title) {
		super(planId, title);
		this.nodes = new ArrayList<>();
		this.createdTime = System.currentTimeMillis();
	}

	// MapReduceExecutionPlan 特有的字段访问器

	public List<Object> getNodes() {
		return nodes;
	}

	public void setNodes(List<Object> nodes) {
		this.nodes = nodes;
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
		nodes.add(node);
	}

	/**
	 * 添加MapReduce执行节点
	 * @param node MapReduce执行节点
	 */
	public void addMapReduceNode(MapReduceNode node) {
		nodes.add(node);
	}

	/**
	 * 获取节点数量
	 * @return 节点总数
	 */
	public int getNodeCount() {
		return nodes.size();
	}

	/**
	 * 根据索引获取节点
	 * @param index 节点索引
	 * @return 节点对象，如果索引无效则返回null
	 */
	public Object getNode(int index) {
		if (index >= 0 && index < nodes.size()) {
			return nodes.get(index);
		}
		return null;
	}

	/**
	 * 获取所有执行步骤（展平所有节点中的步骤）
	 * @return 所有执行步骤的列表
	 */
	@Override
	public List<ExecutionStep> getAllSteps() {
		List<ExecutionStep> allSteps = new ArrayList<>();
		
		for (Object node : nodes) {
			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				if (seqNode.getSteps() != null) {
					allSteps.addAll(seqNode.getSteps());
				}
			} else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				allSteps.addAll(mrNode.getAllSteps());
			}
		}
		
		return allSteps;
	}

	/**
	 * 获取总步骤数量
	 * @return 总步骤数
	 */
	@Override
	public int getTotalStepCount() {
		return getAllSteps().size();
	}

	/**
	 * 获取顺序节点列表
	 * @return 顺序节点列表
	 */
	public List<SequentialNode> getSequentialNodes() {
		List<SequentialNode> sequentialNodes = new ArrayList<>();
		for (Object node : nodes) {
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
	public List<MapReduceNode> getMapReduceNodes() {
		List<MapReduceNode> mapReduceNodes = new ArrayList<>();
		for (Object node : nodes) {
			if (node instanceof MapReduceNode) {
				mapReduceNodes.add((MapReduceNode) node);
			}
		}
		return mapReduceNodes;
	}

	/**
	 * 获取计划执行状态的字符串格式
	 * @param includeResults 是否包含步骤结果
	 * @return 计划状态字符串
	 */
	@Override
	public String getPlanExecutionStateStringFormat(boolean includeResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("=== MapReduce执行计划 ===\n");
		sb.append("计划ID: ").append(planId).append("\n");
		sb.append("标题: ").append(title).append("\n");
		sb.append("创建时间: ").append(new java.util.Date(createdTime)).append("\n");
		sb.append("节点数量: ").append(nodes.size()).append("\n");
		sb.append("总步骤数: ").append(getTotalStepCount()).append("\n");
		sb.append("\n");

		for (int i = 0; i < nodes.size(); i++) {
			Object node = nodes.get(i);
			sb.append("--- 节点 ").append(i + 1).append(" ---\n");
			
			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				sb.append("类型: 顺序执行\n");
				sb.append("步骤数: ").append(seqNode.getStepCount()).append("\n");
				
				if (seqNode.getSteps() != null) {
					for (ExecutionStep step : seqNode.getSteps()) {
						sb.append("  ").append(step.getStepInStr()).append("\n");
						if (includeResults && step.getResult() != null) {
							sb.append("    结果: ").append(step.getResult()).append("\n");
						}
					}
				}
			} else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				sb.append("类型: MapReduce\n");
				sb.append("Map步骤数: ").append(mrNode.getMapStepCount()).append("\n");
				sb.append("Reduce步骤数: ").append(mrNode.getReduceStepCount()).append("\n");
				
				if (mrNode.getMapSteps() != null && !mrNode.getMapSteps().isEmpty()) {
					sb.append("  Map阶段:\n");
					for (ExecutionStep step : mrNode.getMapSteps()) {
						sb.append("    ").append(step.getStepInStr()).append("\n");
						if (includeResults && step.getResult() != null) {
							sb.append("      结果: ").append(step.getResult()).append("\n");
						}
					}
				}
				
				if (mrNode.getReduceSteps() != null && !mrNode.getReduceSteps().isEmpty()) {
					sb.append("  Reduce阶段:\n");
					for (ExecutionStep step : mrNode.getReduceSteps()) {
						sb.append("    ").append(step.getStepInStr()).append("\n");
						if (includeResults && step.getResult() != null) {
							sb.append("      结果: ").append(step.getResult()).append("\n");
						}
					}
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
		nodes.clear();
	}

	/**
	 * 检查计划是否为空
	 * @return 如果没有节点则返回true
	 */
	@Override
	public boolean isEmpty() {
		return nodes.isEmpty();
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
		for (Object node : nodes) {
			if (node instanceof SequentialNode) {
				SequentialNode seqNode = (SequentialNode) node;
				seqNode.removeStep(step);
			} else if (node instanceof MapReduceNode) {
				MapReduceNode mrNode = (MapReduceNode) node;
				mrNode.getMapSteps().remove(step);
				mrNode.getReduceSteps().remove(step);
			}
		}
	}

	@Override
	public void clear() {
		clearNodes();
		planningThinking = null;
		executionParams = "";
	}

	@Override
	public String toString() {
		return getPlanExecutionStateStringFormat(false);
	}
}
