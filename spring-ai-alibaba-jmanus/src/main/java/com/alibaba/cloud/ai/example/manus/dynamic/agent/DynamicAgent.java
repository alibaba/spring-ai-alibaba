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
package com.alibaba.cloud.ai.example.manus.dynamic.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.ReActAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

public class DynamicAgent extends ReActAgent {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgent.class);

	private final String agentName;

	private final String agentDescription;

	private final String nextStepPrompt;

	private ToolCallbackProvider toolCallbackProvider;

	private final List<String> availableToolKeys;

	private ChatResponse response;

	private Prompt userPrompt;

	protected ThinkActRecord thinkActRecord;

	private final ToolCallingManager toolCallingManager;

	private final UserInputService userInputService;

	public void clearUp(String planId) {
		Map<String, ToolCallBackContext> toolCallBackContext = toolCallbackProvider.getToolCallBackContext();
		for (ToolCallBackContext toolCallBack : toolCallBackContext.values()) {
			try {
				toolCallBack.getFunctionInstance().cleanup(planId);
			}
			catch (Exception e) {
				log.error("Error cleaning up tool callback context: {}", e.getMessage(), e);
			}
		}
		// Also remove any pending form input tool for this planId
		if (userInputService != null) {
			userInputService.removeFormInputTool(planId);
		}
	}

	public DynamicAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, String name, String description, String nextStepPrompt,
			List<String> availableToolKeys, ToolCallingManager toolCallingManager,
			Map<String, Object> initialAgentSetting, UserInputService userInputService) {
		super(llmService, planExecutionRecorder, manusProperties, initialAgentSetting);
		this.agentName = name;
		this.agentDescription = description;
		this.nextStepPrompt = nextStepPrompt;
		this.availableToolKeys = availableToolKeys;
		this.toolCallingManager = toolCallingManager;
		this.userInputService = userInputService;
	}

	@Override
	protected boolean think() {
		collectAndSetEnvDataForTools();

		AgentExecutionRecord planExecutionRecord = planExecutionRecorder.getCurrentAgentExecutionRecord(getPlanId());
		thinkActRecord = new ThinkActRecord(planExecutionRecord.getId());
		thinkActRecord.setActStartTime(LocalDateTime.now());
		planExecutionRecorder.recordThinkActExecution(getPlanId(), planExecutionRecord.getId(), thinkActRecord);

		try {
			return executeWithRetry(3);
		}
		catch (Exception e) {
			log.error(String.format("🚨 Oops! The %s's thinking process hit a snag: %s", getName(), e.getMessage()), e);
			thinkActRecord.recordError(e.getMessage());
			return false;
		}
	}

	private boolean executeWithRetry(int maxRetries) throws Exception {
		int attempt = 0;
		while (attempt < maxRetries) {
			attempt++;
			List<Message> messages = new ArrayList<>();
			addThinkPrompt(messages);

			ChatOptions chatOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
			Message nextStepMessage = getNextStepWithEnvMessage();
			messages.add(nextStepMessage);
			thinkActRecord.startThinking(messages.toString());

			log.debug("Messages prepared for the prompt: {}", messages);

			userPrompt = new Prompt(messages, chatOptions);

			List<ToolCallback> callbacks = getToolCallList();
			ChatClient chatClient = llmService.getAgentChatClient();
			response = chatClient.prompt(userPrompt)
				.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, getPlanId()))
				.toolCallbacks(callbacks)
				.call()
				.chatResponse();

			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
			String responseByLLm = response.getResult().getOutput().getText();

			thinkActRecord.finishThinking(responseByLLm);

			log.info(String.format("✨ %s's thoughts: %s", getName(), responseByLLm));
			log.info(String.format("🛠️ %s selected %d tools to use", getName(), toolCalls.size()));

			if (!toolCalls.isEmpty()) {
				log.info(String.format("🧰 Tools being prepared: %s",
						toolCalls.stream().map(ToolCall::name).collect(Collectors.toList())));
				thinkActRecord.setActionNeeded(true);
				thinkActRecord.setToolName(toolCalls.get(0).name());
				thinkActRecord.setToolParameters(toolCalls.get(0).arguments());
				thinkActRecord.setStatus("SUCCESS");
				return true;
			}

			log.warn("Attempt {}: No tools selected. Retrying...", attempt);
		}

		thinkActRecord.setStatus("FAILED");
		return false;
	}

	@Override
	protected AgentExecResult act() {
		try {
			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
			ToolCall toolCall = toolCalls.get(0);

			thinkActRecord.startAction("Executing tool: " + toolCall.name(), toolCall.name(), toolCall.arguments());
			ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);

			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
				.get(toolExecutionResult.conversationHistory().size() - 1);

			llmService.getAgentMemory().add(getPlanId(), toolResponseMessage);
			String llmCallResponse = toolResponseMessage.getResponses().get(0).responseData();

			log.info(String.format("🔧 Tool %s's executing result: %s", getName(), llmCallResponse));

			thinkActRecord.finishAction(llmCallResponse, "SUCCESS");
			String toolcallName = toolCall.name();

			// Handle FormInputTool logic
			if (FormInputTool.name.equals(toolcallName)) {
				ToolCallBiFunctionDef formInputToolDef = getToolCallBackContext(toolcallName).getFunctionInstance();
				if (formInputToolDef instanceof FormInputTool) {
					FormInputTool formInputTool = (FormInputTool) formInputToolDef;
					// Check if the tool is waiting for user input
					if (formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) {
						log.info("FormInputTool is awaiting user input for planId: {}", getPlanId());
						userInputService.storeFormInputTool(getPlanId(), formInputTool);
						// Wait for user input or timeout
						waitForUserInputOrTimeout(formInputTool);

						// After waiting, check the state again
						if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_RECEIVED) {
							log.info("User input received for planId: {}", getPlanId());
							// The UserInputService.submitUserInputs would have updated
							// the tool's internal state.
							// We can now get the updated state string for the LLM.
							llmCallResponse = formInputTool.getCurrentToolStateString();
							// Update the toolResponseMessage in memory for the next LLM
							// call
							ToolResponseMessage.ToolResponse updatedToolResponse = new ToolResponseMessage.ToolResponse(
									toolCall.id(), toolCall.name(), llmCallResponse);
							ToolResponseMessage updatedToolResponseMessage = new ToolResponseMessage(
									List.of(updatedToolResponse), Map.of());
							llmService.getAgentMemory().add(getPlanId(), updatedToolResponseMessage);

						}
						else if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_TIMEOUT) {
							log.warn("Input timeout occurred for FormInputTool for planId: {}", getPlanId());
							userInputService.removeFormInputTool(getPlanId()); // Clean up
							return new AgentExecResult("Input timeout occurred.", AgentState.IN_PROGRESS); // Or
																											// FAILED
						}
					}
				}
			}

			// If the tool is TerminateTool, return completed state
			if (TerminateTool.name.equals(toolcallName)) {
				userInputService.removeFormInputTool(getPlanId()); // Clean up any pending
																	// form
				return new AgentExecResult(llmCallResponse, AgentState.COMPLETED);
			}

			return new AgentExecResult(llmCallResponse, AgentState.IN_PROGRESS);
		}
		catch (Exception e) {
			ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);
			ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(toolCall.id(),
					toolCall.name(), "Error: " + e.getMessage());
			ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(toolResponse), Map.of());
			llmService.getAgentMemory().add(getPlanId(), toolResponseMessage);
			log.error(e.getMessage());

			thinkActRecord.recordError(e.getMessage());
			userInputService.removeFormInputTool(getPlanId()); // Clean up on error

			return new AgentExecResult(e.getMessage(), AgentState.FAILED);
		}
	}

	@Override
	public String getName() {
		return this.agentName;
	}

	@Override
	public String getDescription() {
		return this.agentDescription;
	}

	@Override
	protected Message getNextStepWithEnvMessage() {
		if (StringUtils.isBlank(this.nextStepPrompt)) {
			return new UserMessage("");
		}
		PromptTemplate promptTemplate = new PromptTemplate(this.nextStepPrompt);
		Message userMessage = promptTemplate.createMessage(getMergedData());
		return userMessage;
	}

	private Map<String, Object> getMergedData() {
		Map<String, Object> data = new HashMap<>();
		data.putAll(getInitSettingData());
		data.put(PlanExecutor.EXECUTION_ENV_STRING_KEY, convertEnvDataToString());
		return data;
	}

	@Override
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String envPrompt = """

				当前步骤的环境信息是:
				{current_step_env_data}

				""";

		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(envPrompt);
		Message systemMessage = promptTemplate.createMessage(getMergedData());
		messages.add(systemMessage);
		return systemMessage;
	}

	private ToolCallBackContext getToolCallBackContext(String toolKey) {
		Map<String, ToolCallBackContext> toolCallBackContext = toolCallbackProvider.getToolCallBackContext();
		if (toolCallBackContext.containsKey(toolKey)) {
			return toolCallBackContext.get(toolKey);
		}
		else {
			log.warn("Tool callback for {} not found in the map.", toolKey);
			return null;
		}
	}

	@Override
	public List<ToolCallback> getToolCallList() {
		List<ToolCallback> toolCallbacks = new ArrayList<>();
		Map<String, ToolCallBackContext> toolCallBackContext = toolCallbackProvider.getToolCallBackContext();
		for (String toolKey : availableToolKeys) {
			if (toolCallBackContext.containsKey(toolKey)) {
				ToolCallBackContext toolCallback = toolCallBackContext.get(toolKey);
				if (toolCallback != null) {
					toolCallbacks.add(toolCallback.getToolCallback());
				}
			}
			else {
				log.warn("Tool callback for {} not found in the map.", toolKey);
			}
		}
		return toolCallbacks;
	}

	public void addEnvData(String key, String value) {
		Map<String, Object> data = super.getInitSettingData();
		if (data == null) {
			throw new IllegalStateException("Data map is null. Cannot add environment data.");
		}
		data.put(key, value);
	}

	public void setToolCallbackProvider(ToolCallbackProvider toolCallbackProvider) {
		this.toolCallbackProvider = toolCallbackProvider;
	}

	protected String collectEnvData(String toolCallName) {
		ToolCallBackContext context = toolCallbackProvider.getToolCallBackContext().get(toolCallName);
		if (context != null) {
			return context.getFunctionInstance().getCurrentToolStateString();
		}
		// 如果没有找到对应的工具回调上下文，返回空字符串
		return "";
	}

	public void collectAndSetEnvDataForTools() {

		Map<String, Object> toolEnvDataMap = new HashMap<>();

		Map<String, Object> oldMap = getEnvData();
		toolEnvDataMap.putAll(oldMap);

		// 用新数据覆盖旧数据
		for (String toolKey : availableToolKeys) {
			String envData = collectEnvData(toolKey);
			toolEnvDataMap.put(toolKey, envData);
		}
		log.debug("收集到的工具环境数据: {}", toolEnvDataMap);

		setEnvData(toolEnvDataMap);
	}

	public String convertEnvDataToString() {
		StringBuilder envDataStringBuilder = new StringBuilder();

		for (String toolKey : availableToolKeys) {
			Object value = getEnvData().get(toolKey);
			if (value == null || value.toString().isEmpty()) {
				continue; // Skip tools with no data
			}
			envDataStringBuilder.append(toolKey).append(" 的上下文信息：\n");
			envDataStringBuilder.append("    ").append(value.toString()).append("\n");
		}

		return envDataStringBuilder.toString();
	}

	// Add a method to wait for user input or handle timeout.
	private void waitForUserInputOrTimeout(FormInputTool formInputTool) {
		log.info("Waiting for user input for planId: {}...", getPlanId());
		long startTime = System.currentTimeMillis();
		// Get timeout from ManusProperties and convert to milliseconds
		long userInputTimeoutMs = getManusProperties().getUserInputTimeout() * 1000L;

		while (formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) {
			if (System.currentTimeMillis() - startTime > userInputTimeoutMs) {
				log.warn("Timeout waiting for user input for planId: {}", getPlanId());
				formInputTool.handleInputTimeout(); // This will change its state to
													// INPUT_TIMEOUT
				break;
			}
			try {
				// Poll for input state change. In a real scenario, this might involve
				// a more sophisticated mechanism like a Future or a callback from the UI.
				TimeUnit.MILLISECONDS.sleep(500); // Check every 500ms
			}
			catch (InterruptedException e) {
				log.warn("Interrupted while waiting for user input for planId: {}", getPlanId());
				Thread.currentThread().interrupt();
				formInputTool.handleInputTimeout(); // Treat interruption as timeout for
													// simplicity
				break;
			}
		}
		if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_RECEIVED) {
			log.info("User input received for planId: {}", getPlanId());
		}
		else if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_TIMEOUT) {
			log.warn("User input timed out for planId: {}", getPlanId());
		}
	}

}
