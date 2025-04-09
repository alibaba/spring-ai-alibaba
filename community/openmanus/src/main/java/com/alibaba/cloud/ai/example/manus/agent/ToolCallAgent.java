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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

/**
 * 工具调用智能体，专门负责管理和执行工具调用的智能体实现 继承自ReActAgent，实现了基于工具调用的思考-行动模式
 */
public class ToolCallAgent extends ReActAgent {

	private static final Logger log = LoggerFactory.getLogger(ToolCallAgent.class);

	private final ToolCallingManager toolCallingManager;

	private ChatResponse response;

	private Prompt userPrompt;

	protected ThinkActRecord thinkActRecord;

	private static final String EXECUTION_ENV_KEY_STRING = "current_step_env_data";

	private Map<String, ToolCallBackContext> toolCallbackMap;

	private List<String> availableToolKeys = List.of("FileSaver", "PythonExecute", "TerminateTool");

	public ToolCallAgent(LlmService llmService, ToolCallingManager toolCallingManager,
			PlanExecutionRecorder planExecutionRecorder, ManusProperties manusProperties,
			Map<String, ToolCallBackContext> toolCallbackMap) {
		super(llmService, planExecutionRecorder, manusProperties);
		this.toolCallingManager = toolCallingManager;
		this.toolCallbackMap = toolCallbackMap;
	}

	/**
	 * 执行思考过程 实现说明： 1. 准备思考所需的消息列表 2. 设置工具调用选项 3. 构建提示并获取LLM响应 4. 分析响应中的工具调用 5.
	 * 记录思考过程和工具选择
	 * @param retry 当前重试次数
	 * @return true 如果有工具需要调用，false 如果不需要执行任何工具
	 */

	@Override
	protected boolean think() {

		AgentExecutionRecord planExecutionRecord = planExecutionRecorder.getCurrentAgentExecutionRecord(getPlanId());
		thinkActRecord = new ThinkActRecord(planExecutionRecord.getId());
		planExecutionRecorder.recordThinkActExecution(getPlanId(), planExecutionRecord.getId(), thinkActRecord);

		try {
			List<Message> messages = new ArrayList<>();
			addThinkPrompt(messages);
			// provided Java code is responsible
			// for managing and executing tool
			// calls within the ReAct agent. It
			// extends the `ReActAgent` class
			// and implements the logic for
			// handling tool calls during the
			// thinking and acting phases of the
			// agent.

			// calltool with mem
			ChatOptions chatOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
			Message nextStepMessage = getNextStepWithEnvMessage();
			messages.add(nextStepMessage);
			thinkActRecord.startThinking(messages.toString());// The `ToolCallAgent` class
																// in the

			log.debug("Messages prepared for the prompt: {}", messages);

			userPrompt = new Prompt(messages, chatOptions);

			response = llmService.getAgentChatClient(getPlanId())
				.getChatClient()
				.prompt(userPrompt)
				.advisors(memoryAdvisor -> memoryAdvisor.param(CHAT_MEMORY_CONVERSATION_ID_KEY, getConversationId())
					.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.tools(getToolCallList())
				.call()
				.chatResponse();

			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
			String responseByLLm = response.getResult().getOutput().getText();

			thinkActRecord.finishThinking(responseByLLm);

			log.info(String.format("✨ %s's thoughts: %s", getName(), responseByLLm));
			log.info(String.format("🛠️ %s selected %d tools to use", getName(), toolCalls.size()));

			if (responseByLLm != null && !responseByLLm.isEmpty()) {
				log.info(String.format("💬 %s's response: %s", getName(), responseByLLm));
			}
			if (!toolCalls.isEmpty()) {
				log.info(String.format("🧰 Tools being prepared: %s",
						toolCalls.stream().map(ToolCall::name).collect(Collectors.toList())));
				thinkActRecord.setActionNeeded(true);
				thinkActRecord.setToolName(toolCalls.get(0).name());
				thinkActRecord.setToolParameters(toolCalls.get(0).arguments());
			}

			thinkActRecord.setStatus("SUCCESS");

			return !toolCalls.isEmpty();
		}
		catch (Exception e) {
			log.error(String.format("🚨 Oops! The %s's thinking process hit a snag: %s", getName(), e.getMessage()));
			thinkActRecord.recordError(e.getMessage());
			return false;
		}
	}

	/**
	 * 工具调用智能体的功能描述 实现说明：提供该智能体的核心功能说明 描述包含工具调用管理和执行能力等关键特性
	 * @return 智能体的功能描述文本
	 */
	@Override
	public String getDescription() {
		return "ToolCallAgent: A class responsible for managing tool calls in the ReAct agent.";
	}

	/**
	 * 工具调用智能体的名称 实现说明：返回固定的智能体标识符"ToolCallAgent" 用于在日志和调试中标识该类型的智能体
	 * @return "ToolCallAgent"
	 */
	@Override
	public String getName() {
		return "ToolCallAgent";
	}

	/**
	 * 添加工具调用相关的思考提示 实现说明： 1. 首先调用父类的addThinkPrompt添加基础提示 2. 构建特定的工具调用相关提示，包括： - 当前计划状态 -
	 * 当前步骤信息 - 执行指南 - 完成协议 3. 返回包含完整提示信息的系统消息
	 * @param messages 当前的消息列表
	 * @return 添加了工具调用相关提示的系统消息
	 */
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String stepPrompt = """
				EXECUTION GUIDELINES:
						1. This is a SINGLE task step that may require multiple actions to complete
						2. Use appropriate tools to accomplish the current task step
						3. Stay focused on THIS task step until ALL requirements are met
						4. Each task step may need multiple actions/tools to be fully complete

						COMPLETION PROTOCOL:
						Only call Terminate tool when ALL of the following are true:
						1. ALL requirements for THIS task step are completed
						2. ALL necessary actions for THIS task step are done
						3. You have verified the results
						4. You can provide:
						- Complete summary of accomplishments
						- All relevant data/metrics
						- Final status confirmation

						⚠️ IMPORTANT:
						- You are working on ONE task step that may need multiple actions
						- Do NOT proceed to next TASK step until current one is 100% complete
						- Do NOT confuse task steps with action steps

					""";

		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(stepPrompt);

		Message systemMessage = promptTemplate.createMessage(getData());

		messages.add(systemMessage);
		return systemMessage;
	}

	/**
	 *
	 * 获取下一步执行提示的用户消息对象 实现说明： 1. 构建提示模板，包含当前步骤的环境状态 2. 创建用户消息对象 这里会额外带 上当前步骤的环境数据 ，所以，如果
	 * 你想自定义环境数据 则 重写这个方法， 如果不想，则
	 * @return 下一步执行提示的用户消息对象
	 */
	protected Message getNextStepWithEnvMessage() {

		String nextStepPrompt = """

				CURRENT STEP ENVIRONMENT STATUS:
				{current_step_env_data}

				""";

		nextStepPrompt += getNextStepPromptString();
		PromptTemplate promptTemplate = new PromptTemplate(nextStepPrompt);
		Message userMessage = promptTemplate.createMessage(getData());
		return userMessage;
	}

	/**
	 * 获取下一步执行的用户自定义 Prompt ， 写的时候可以不带环境数据（因为全局会自动拼）
	 * 这个环境数据物理上属于toolcall的一个参数，如果需要修改或携带环境数据，在tool里面去修改
	 * @return 下一步执行的用户自定义 Prompt
	 */
	protected String getNextStepPromptString() {
		String nextStepPrompt = """
				What should I do for next action to achieve my goal?
				""";
		return nextStepPrompt;
	}

	@Override
	protected String act() {
		try {
			List<String> results = new ArrayList<>();
			ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);

			thinkActRecord.startAction("Executing tool: " + toolCall.name(), toolCall.name(), toolCall.arguments());

			ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);

			addEnvData(EXECUTION_ENV_KEY_STRING, collectEnvData(toolCall.name()));

			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
				.get(toolExecutionResult.conversationHistory().size() - 1);
			llmService.getAgentChatClient(getPlanId()).getMemory().add(getConversationId(), toolResponseMessage);
			String llmCallResponse = toolResponseMessage.getResponses().get(0).responseData();
			results.add(llmCallResponse);

			String finalResult = String.join("\n\n", results);
			log.info(String.format("🔧 Tool %s's executing result: %s", getName(), llmCallResponse));

			thinkActRecord.finishAction(finalResult, "SUCCESS");

			return finalResult;
		}
		catch (Exception e) {
			ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);
			ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(toolCall.id(),
					toolCall.name(), "Error: " + e.getMessage());
			ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(toolResponse), Map.of());
			llmService.getAgentChatClient(getPlanId()).getMemory().add(getConversationId(), toolResponseMessage);
			log.error(e.getMessage());

			thinkActRecord.recordError(e.getMessage());

			return "Error: " + e.getMessage();
		}
	}

	public List<ToolCallback> getToolCallList() {
		List<ToolCallback> toolCallList = new ArrayList<>();
		for (String key : availableToolKeys) {
			if (toolCallbackMap.containsKey(key)) {
				ToolCallBackContext context = toolCallbackMap.get(key);
				ToolCallback toolCallback = context.getToolCallback();
				toolCallList.add(toolCallback);
			}
		}
		return toolCallList;
	}

	protected String collectEnvData(String toolCallName) {
		ToolCallBackContext context = toolCallbackMap.get(toolCallName);
		if (context != null) {
			return context.getFunctionInstance().getCurrentToolStateString();
		}
		// 如果没有找到对应的工具回调上下文，返回空字符串
		return "";
	}

	public void addEnvData(String key, String value) {
		Map<String, Object> data = super.getData();
		if (data == null) {
			throw new IllegalStateException("Data map is null. Cannot add environment data.");
		}
		data.put(key, value);
	}

}
