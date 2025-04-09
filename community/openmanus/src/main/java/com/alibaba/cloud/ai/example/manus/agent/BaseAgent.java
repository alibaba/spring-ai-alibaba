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
package com.alibaba.cloud.ai.example.manus.agent;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An abstract base class for implementing AI agents that can execute multi-step tasks.
 * This class provides the core functionality for managing agent state, conversation flow,
 * and step-by-step execution of tasks.
 *
 * <p>
 * The agent supports a finite number of execution steps and includes mechanisms for:
 * <ul>
 * <li>State management (idle, running, finished)</li>
 * <li>Conversation tracking</li>
 * <li>Step limitation and monitoring</li>
 * <li>Thread-safe execution</li>
 * <li>Stuck-state detection and handling</li>
 * </ul>
 *
 * <p>
 * Implementing classes must define:
 * <ul>
 * <li>{@link #getName()} - Returns the agent's name</li>
 * <li>{@link #getDescription()} - Returns the agent's description</li>
 * <li>{@link #addThinkPrompt(List)} - Implements the thinking chain logic</li>
 * <li>{@link #getNextStepMessage()} - Provides the next step's prompt template</li>
 * <li>{@link #step()} - Implements the core logic for each execution step</li>
 * </ul>
 *
 * @see AgentState
 * @see LlmService
 */
public abstract class BaseAgent {

	private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

	private final ReentrantLock lock = new ReentrantLock();

	private String conversationId;

	private String planId = null;

	private AgentState state = AgentState.IDLE;

	protected LlmService llmService;

	private final ManusProperties manusProperties;

	private int maxSteps;

	private int currentStep = 0;

	private Map<String, Object> data = new HashMap<>();

	protected PlanExecutionRecorder planExecutionRecorder;

	/**
	 * 获取智能体的名称
	 *
	 * 实现要求： 1. 返回一个简短但具有描述性的名称 2. 名称应该反映该智能体的主要功能或特性 3. 名称应该是唯一的，便于日志和调试
	 *
	 * 示例实现： - ToolCallAgent 返回 "ToolCallAgent" - BrowserAgent 返回 "BrowserAgent"
	 * @return 智能体的名称
	 */
	public abstract String getName();

	/**
	 * 获取智能体的详细描述
	 *
	 * 实现要求： 1. 返回对该智能体功能的详细描述 2. 描述应包含智能体的主要职责和能力 3. 应说明该智能体与其他智能体的区别
	 *
	 * 示例实现： - ToolCallAgent: "负责管理和执行工具调用的智能体，支持多工具组合调用" - ReActAgent:
	 * "实现思考(Reasoning)和行动(Acting)交替执行的智能体"
	 * @return 智能体的详细描述文本
	 */
	public abstract String getDescription();

	/**
	 * 添加思考提示到消息列表中，构建智能体的思考链
	 *
	 * 实现要求： 1. 根据当前上下文和状态生成合适的系统提示词 2. 提示词应该指导智能体如何思考和决策 3. 可以递归地构建提示链，形成层次化的思考过程 4.
	 * 返回添加的系统提示消息对象
	 *
	 * 子类实现参考： 1. ReActAgent: 实现基础的思考-行动循环提示 2. ToolCallAgent: 添加工具选择和执行相关的提示
	 * @param messages 当前的消息列表，用于构建上下文
	 * @return 添加的系统提示消息对象
	 */
	protected Message addThinkPrompt(List<Message> messages) {
		// 获取操作系统信息
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");

		// 获取当前日期时间，格式为yyyy-MM-dd
		String currentDateTime = java.time.LocalDate.now().toString(); // 格式为yyyy-MM-dd

		String stepPrompt = """
				SYSTEM INFORMATION:
				OS: %s %s (%s)

				Current Date:
				%s

				CURRENT TASK STATUS:
				{planStatus}

				CURRENT TASK STEP ({currentStepIndex}):
				{stepText}

				""".formatted(osName, osVersion, osArch, currentDateTime);

		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(stepPrompt);

		Message systemMessage = promptTemplate.createMessage(getData());

		messages.add(systemMessage);
		return systemMessage;
	}

	/**
	 * 获取下一步操作的提示消息
	 *
	 * 实现要求： 1. 生成引导智能体执行下一步操作的提示消息 2. 提示内容应该基于当前执行状态和上下文 3. 消息应该清晰指导智能体要执行什么任务
	 *
	 * 子类实现参考： 1. ToolCallAgent：返回工具选择和调用相关的提示 2. ReActAgent：返回思考或行动决策相关的提示
	 * @return 下一步操作的提示消息对象
	 */
	protected abstract Message getNextStepWithEnvMessage();

	public abstract List<ToolCallback> getToolCallList();

	public BaseAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties) {
		this.llmService = llmService;
		this.planExecutionRecorder = planExecutionRecorder;
		this.manusProperties = manusProperties;
		this.maxSteps = manusProperties.getMaxSteps();
	}

	public String run(Map<String, Object> data) {
		currentStep = 0;
		if (state != AgentState.IDLE) {
			throw new IllegalStateException("Cannot run agent from state: " + state);
		}

		setData(data);

		// Create agent execution record
		AgentExecutionRecord agentRecord = new AgentExecutionRecord(getConversationId(), getName(), getDescription());
		agentRecord.setMaxSteps(maxSteps);
		agentRecord.setStatus(state.toString());
		// Record execution in recorder if we have a plan ID
		if (planId != null && planExecutionRecorder != null) {
			planExecutionRecorder.recordAgentExecution(planId, agentRecord);
		}
		List<String> results = new ArrayList<>();
		lock.lock();
		try {
			state = AgentState.RUNNING;
			agentRecord.setStatus(state.toString());

			while (currentStep < maxSteps && !state.equals(AgentState.FINISHED)) {
				currentStep++;
				log.info("Executing round " + currentStep + "/" + maxSteps);

				String stepResult = step();

				if (isStuck()) {
					handleStuckState(agentRecord);
				}

				results.add("Round " + currentStep + ": " + stepResult);

				// Update agent record after each step
				agentRecord.setCurrentStep(currentStep);
			}

			if (currentStep >= maxSteps) {
				results.add("Terminated: Reached max rounds (" + maxSteps + ")");
			}

			// Set final state in record
			agentRecord.setEndTime(LocalDateTime.now());
			agentRecord.setStatus(AgentState.IDLE.toString());
			agentRecord.setCompleted(state.equals(AgentState.FINISHED));

			// Calculate execution time in seconds
			long executionTimeSeconds = java.time.Duration.between(agentRecord.getStartTime(), agentRecord.getEndTime())
				.getSeconds();
			String status = agentRecord.isCompleted() ? "成功" : (agentRecord.isStuck() ? "执行卡住" : "未完成");
			agentRecord.setResult(String.format("执行%s [耗时%d秒] [消耗步骤%d] ", status, executionTimeSeconds, currentStep));

		}
		finally {
			lock.unlock();
			state = AgentState.IDLE; // Reset state after execution
			agentRecord.setStatus(state.toString());
		}
		return String.join("\n", results);
	}

	protected abstract String step();

	private void handleStuckState(AgentExecutionRecord agentRecord) {
		log.warn("Agent stuck detected - Missing tool calls");

		// End current step
		setState(AgentState.FINISHED);

		String stuckPrompt = """
				Agent response detected missing required tool calls.
				Please ensure each response includes at least one tool call to progress the task.
				Current step: %d
				Execution status: Force terminated
				""".formatted(currentStep);

		// Update agent record
		agentRecord.setStuck(true);
		agentRecord.setErrorMessage(stuckPrompt);
		agentRecord.setStatus(state.toString());

		log.error(stuckPrompt);
	}

	/**
	 * 检查是否处于卡住状态
	 */
	protected boolean isStuck() {
		// 目前判断是如果三次没有调用工具就认为是卡住了，就退出当前step。
		List<Message> memoryEntries = llmService.getAgentChatClient(getPlanId()).getMemory().get(conversationId, 6);
		int zeroToolCallCount = 0;
		for (Message msg : memoryEntries) {
			if (msg instanceof AssistantMessage) {
				AssistantMessage assistantMsg = (AssistantMessage) msg;
				if (assistantMsg.getToolCalls() == null || assistantMsg.getToolCalls().isEmpty()) {
					zeroToolCallCount++;
				}
			}
		}
		return zeroToolCallCount >= 3;
	}

	public void setState(AgentState state) {
		this.state = state;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	/**
	 * 获取智能体的数据上下文
	 *
	 * 使用说明： 1. 返回智能体在执行过程中需要的所有上下文数据 2. 数据可包含： - 当前执行状态 - 步骤信息 - 中间结果 - 配置参数 3.
	 * 数据在run()方法执行时通过setData()设置
	 *
	 * 不要修改这个方法的实现，如果你需要传递上下文，继承并修改setData方法，这样可以提高getData()的的效率。
	 * @return 包含智能体上下文数据的Map对象
	 */
	protected final Map<String, Object> getData() {
		return data;
	}

	protected void setData(Map<String, Object> data) {
		this.data = data;
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

}
