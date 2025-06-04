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

import java.time.LocalDateTime;

/**
 * 记录智能体在单个执行步骤中的思考和行动过程。 作为AgentExecutionRecord的子步骤存在，专注于记录思考和行动阶段的处理消息。
 *
 * 数据结构简化为三个主要部分：
 *
 * 1. 基本信息 (Basic Info) - id: 记录的唯一标识 - stepNumber: 步骤编号 - parentExecutionId: 父执行记录ID
 *
 * 2. 思考阶段 (Think Phase) - thinkStartTime: 思考开始时间 - thinkInput: 思考输入内容 - thinkOutput:
 * 思考输出结果 - thinkEndTime: 思考结束时间
 *
 * 3. 行动阶段 (Act Phase) - actStartTime: 行动开始时间 - toolName: 使用的工具名称 - toolParameters: 工具参数 -
 * actionNeeded: 是否需要执行动作 - actionDescription: 行动描述 - actionResult: 行动执行结果 - actEndTime:
 * 行动结束时间 - status: 执行状态 - errorMessage: 错误信息（如有）
 *
 * @see AgentExecutionRecord
 * @see JsonSerializable
 */
public class ThinkActRecord {

	// 记录的唯一标识符
	private Long id;

	// 父执行记录的ID，关联到AgentExecutionRecord
	private Long parentExecutionId;

	// 思考开始的时间戳
	private LocalDateTime thinkStartTime;

	// 思考完成的时间戳
	private LocalDateTime thinkEndTime;

	// 行动开始的时间戳
	private LocalDateTime actStartTime;

	// 行动完成的时间戳
	private LocalDateTime actEndTime;

	// 思考过程的输入上下文
	private String thinkInput;

	// 思考过程的输出结果
	private String thinkOutput;

	// 思考是否确定需要采取行动
	private boolean actionNeeded;

	// 将要采取的行动描述
	private String actionDescription;

	// 行动执行的结果
	private String actionResult;

	// 此思考-行动周期的状态（成功、失败等）
	private String status;

	// 如果周期遇到问题的错误消息
	private String errorMessage;

	// 用于行动的工具名称（如适用）
	private String toolName;

	// 用于行动的工具参数（序列化，如适用）
	private String toolParameters;

	// 默认构造函数
	public ThinkActRecord() {
	}

	// 带父执行ID的构造函数
	public ThinkActRecord(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
		this.thinkStartTime = LocalDateTime.now();
	}

	/**
	 * 记录思考阶段开始
	 */
	public void startThinking(String thinkInput) {

		this.thinkStartTime = LocalDateTime.now();
		this.thinkInput = thinkInput;
	}

	/**
	 * 记录思考阶段结束
	 */
	public void finishThinking(String thinkOutput) {
		this.thinkEndTime = LocalDateTime.now();
		this.thinkOutput = thinkOutput;
	}

	/**
	 * 记录行动阶段开始
	 */
	public void startAction(String actionDescription, String toolName, String toolParameters) {
		this.actStartTime = LocalDateTime.now();
		this.actionNeeded = true;
		this.actionDescription = actionDescription;
		this.toolName = toolName;
		this.toolParameters = toolParameters;
	}

	/**
	 * 记录行动阶段结束
	 */
	public void finishAction(String actionResult, String status) {
		this.actEndTime = LocalDateTime.now();
		this.actionResult = actionResult;
		this.status = status;
	}

	/**
	 * 记录错误信息
	 */
	public void recordError(String errorMessage) {
		this.errorMessage = errorMessage;
		this.status = "ERROR";
	}

	/**
	 * 保存记录到持久化存储 空实现，由具体的存储实现来覆盖
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
		return this.id;
	}

	// Getters and setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	public LocalDateTime getThinkStartTime() {
		return thinkStartTime;
	}

	public void setThinkStartTime(LocalDateTime thinkStartTime) {
		this.thinkStartTime = thinkStartTime;
	}

	public LocalDateTime getThinkEndTime() {
		return thinkEndTime;
	}

	public void setThinkEndTime(LocalDateTime thinkEndTime) {
		this.thinkEndTime = thinkEndTime;
	}

	public LocalDateTime getActStartTime() {
		return actStartTime;
	}

	public void setActStartTime(LocalDateTime actStartTime) {
		this.actStartTime = actStartTime;
	}

	public LocalDateTime getActEndTime() {
		return actEndTime;
	}

	public void setActEndTime(LocalDateTime actEndTime) {
		this.actEndTime = actEndTime;
	}

	public String getThinkInput() {
		return thinkInput;
	}

	public void setThinkInput(String thinkInput) {
		this.thinkInput = thinkInput;
	}

	public String getThinkOutput() {
		return thinkOutput;
	}

	public void setThinkOutput(String thinkOutput) {
		this.thinkOutput = thinkOutput;
	}

	public boolean isActionNeeded() {
		return actionNeeded;
	}

	public void setActionNeeded(boolean actionNeeded) {
		this.actionNeeded = actionNeeded;
	}

	public String getActionDescription() {
		return actionDescription;
	}

	public void setActionDescription(String actionDescription) {
		this.actionDescription = actionDescription;
	}

	public String getActionResult() {
		return actionResult;
	}

	public void setActionResult(String actionResult) {
		this.actionResult = actionResult;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public String getToolParameters() {
		return toolParameters;
	}

	public void setToolParameters(String toolParameters) {
		this.toolParameters = toolParameters;
	}

	@Override
	public String toString() {
		return "ThinkActRecord{" + "id='" + id + '\'' + ", parentExecutionId='" + parentExecutionId + '\''
				+ ", actionNeeded=" + actionNeeded + ", status='" + status + '\'' + '}';
	}

}
