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

package com.alibaba.cloud.ai.example.manus.recorder.entity;

import com.alibaba.cloud.ai.example.manus.flow.PlanStepStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划执行记录类，用于跟踪和记录PlanningFlow执行过程的详细信息。
 *
 * 数据结构分为四个主要部分：
 *
 * 1. 基本信息 (Basic Info) - id: 记录的唯一标识 - planId: 计划的唯一标识 - title: 计划标题 - startTime: 执行开始时间
 * - endTime: 执行结束时间 - userRequest: 用户的原始请求
 *
 * 2. 计划结构 (Plan Structure) - steps: 计划步骤列表 - stepStatuses: 步骤状态列表 - stepNotes: 步骤备注列表 -
 * stepAgents: 与每个步骤关联的智能体
 *
 * 3. 执行过程数据 (Execution Data) - currentStepIndex: 当前执行的步骤索引 - agentExecutionRecords:
 * 每个智能体执行的记录 - executorKeys: 执行者键列表 - resultState: 共享结果状态
 *
 * 4. 执行结果 (Execution Result) - completed: 是否完成 - progress: 执行进度（百分比） - summary: 执行总结
 */
public class PlanExecutionRecord implements JsonSerializable {

	// 记录的唯一标识符
	private Long id;

	// 计划的唯一标识符
	private String planId;

	// 计划标题
	private String title;

	// 用户的原始请求
	private String userRequest;

	// 执行开始的时间戳
	private LocalDateTime startTime;

	// 执行结束的时间戳
	private LocalDateTime endTime;

	// 计划的步骤列表
	private List<String> steps;

	// 当前执行的步骤索引
	private Integer currentStepIndex;

	// 是否完成
	private boolean completed;

	// 执行总结
	private String summary;

	// List to maintain the sequence of agent executions
	private List<AgentExecutionRecord> agentExecutionSequence;

	/**
	 * 默认构造函数
	 */
	public PlanExecutionRecord() {
		this.steps = new ArrayList<>();
		this.startTime = LocalDateTime.now();
		this.completed = false;
		this.agentExecutionSequence = new ArrayList<>();
	}

	/**
	 * 带参数的构造函数
	 * @param planId 计划ID
	 * @param title 计划标题
	 * @param userRequest 用户请求
	 */
	public PlanExecutionRecord(String planId, String title, String userRequest) {
		this();
		this.planId = planId;
		this.title = title;
		this.userRequest = userRequest;
	}

	/**
	 * 添加一个执行步骤
	 * @param step 步骤描述
	 * @param agentName 执行智能体名称
	 */
	public void addStep(String step, String agentName) {
		this.steps.add(step);
	}

	/**
	 * 添加智能体执行记录
	 * @param agentName 智能体名称
	 * @param record 执行记录
	 */
	public void addAgentExecutionRecord(AgentExecutionRecord record) {
		this.agentExecutionSequence.add(record);
	}

	/**
	 * 获取按执行顺序排列的智能体执行记录列表
	 * @return 执行记录列表
	 */
	public List<AgentExecutionRecord> getAgentExecutionSequence() {
		return agentExecutionSequence;
	}

	public void setAgentExecutionSequence(List<AgentExecutionRecord> agentExecutionSequence) {
		this.agentExecutionSequence = agentExecutionSequence;
	}

	/**
	 * 完成执行，设置结束时间
	 */
	public void complete(String summary) {
		this.endTime = LocalDateTime.now();
		this.completed = true;
		this.summary = summary;
	}

	/**
	 * 保存记录到持久化存储 空实现，由具体的存储实现来覆盖 同时会递归保存所有AgentExecutionRecord
	 * @return 保存后的记录ID
	 */
	public Long save() {
		// 如果ID为空，生成一个随机ID
		if (this.id == null) {
			// 使用时间戳和随机数组合生成ID
			long timestamp = System.currentTimeMillis();
			int random = (int) (Math.random() * 1000000);
			this.id = timestamp * 1000 + random;
		}

		// Save all AgentExecutionRecords
		if (agentExecutionSequence != null) {
			for (AgentExecutionRecord agentRecord : agentExecutionSequence) {
				agentRecord.save();
			}
		}
		return this.id;
	}

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserRequest() {
		return userRequest;
	}

	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public List<String> getSteps() {
		return steps;
	}

	public void setSteps(List<String> steps) {
		this.steps = steps;
	}

	public Integer getCurrentStepIndex() {
		return currentStepIndex;
	}

	public void setCurrentStepIndex(Integer currentStepIndex) {
		this.currentStepIndex = currentStepIndex;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * 返回此记录的字符串表示形式，包含关键字段信息
	 * @return 包含记录关键信息的字符串
	 */
	@Override
	public String toString() {
		return String.format(
				"PlanExecutionRecord{id=%d, planId='%s', title='%s', steps=%d, currentStep=%d/%d, completed=%b}", id,
				planId, title, steps.size(), currentStepIndex != null ? currentStepIndex + 1 : 0, steps.size(),
				completed);
	}

	/**
	 * 将记录转换为JSON格式的字符串 包含所有关键字段，包括： - 基本信息（id, planId, title等） - 时间信息（startTime, endTime）
	 * - 执行状态（currentStepIndex, completed等） - 步骤信息（steps） -
	 * 智能体执行记录（agentExecutionSequence）
	 * @return JSON格式的字符串
	 */
	@Override
	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("{");

		// 基本信息
		appendField(json, "id", id, true);
		appendField(json, "planId", planId, true);
		appendField(json, "title", title, true);
		appendField(json, "userRequest", userRequest, true);

		// 时间信息
		if (startTime != null) {
			appendField(json, "startTime", startTime.toString(), true);
		}
		if (endTime != null) {
			appendField(json, "endTime", endTime.toString(), true);
		}

		// 执行状态
		appendField(json, "currentStepIndex", currentStepIndex, false);
		appendField(json, "completed", completed, false);
		appendField(json, "summary", summary, true);

		// 步骤信息
		if (steps != null && !steps.isEmpty()) {
			json.append("\"steps\":[");
			for (int i = 0; i < steps.size(); i++) {
				if (i > 0)
					json.append(",");
				json.append("\"").append(escapeJson(steps.get(i))).append("\"");
			}
			json.append("],");
		}

		// 智能体执行记录
		if (agentExecutionSequence != null && !agentExecutionSequence.isEmpty()) {
			json.append("\"agentExecutionSequence\":[");
			for (int i = 0; i < agentExecutionSequence.size(); i++) {
				if (i > 0)
					json.append(",");
				json.append(agentExecutionSequence.get(i).toJson());
			}
			json.append("],");
		}

		// 移除末尾多余的逗号
		if (json.charAt(json.length() - 1) == ',') {
			json.setLength(json.length() - 1);
		}

		json.append("}");
		return json.toString();
	}

}
