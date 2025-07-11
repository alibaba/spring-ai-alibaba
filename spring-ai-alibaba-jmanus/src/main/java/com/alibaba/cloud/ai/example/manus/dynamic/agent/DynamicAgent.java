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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
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
import com.alibaba.cloud.ai.example.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;

public class DynamicAgent extends ReActAgent {

	private static final String CURRENT_STEP_ENV_DATA_KEY = "current_step_env_data";

	private static final Logger log = LoggerFactory.getLogger(DynamicAgent.class);

	private final String agentName;

	private final String agentDescription;

	private final String nextStepPrompt;

	private ToolCallbackProvider toolCallbackProvider;

	private final List<String> availableToolKeys;

	private ChatResponse response;

	private Prompt userPrompt;

	// 存储当前创建的ThinkActRecord ID，用于后续的action记录
	private Long currentThinkActRecordId;

	private final ToolCallingManager toolCallingManager;

	private final UserInputService userInputService;

	private final DynamicModelEntity model;

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
			Map<String, Object> initialAgentSetting, UserInputService userInputService, PromptService promptService,
			DynamicModelEntity model) {
		super(llmService, planExecutionRecorder, manusProperties, initialAgentSetting, promptService);
		this.agentName = name;
		this.agentDescription = description;
		this.nextStepPrompt = nextStepPrompt;
		this.availableToolKeys = availableToolKeys;
		this.toolCallingManager = toolCallingManager;
		this.userInputService = userInputService;
		this.model = model;
	}

	@Override
	protected boolean think() {
		collectAndSetEnvDataForTools();

		try {
			return executeWithRetry(3);
		}
		catch (Exception e) {
			log.error(String.format("🚨 Oops! The %s's thinking process hit a snag: %s", getName(), e.getMessage()), e);
			log.info("Exception occurred", e);

			// 记录思考失败
			PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
			params.setCurrentPlanId(getCurrentPlanId());
			params.setRootPlanId(getRootPlanId());
			params.setThinkActRecordId(getThinkActRecordId());
			params.setAgentName(getName());
			params.setAgentDescription(getDescription());
			params.setThinkInput(null);
			params.setThinkOutput(null);
			params.setActionNeeded(false);
			params.setToolName(null);
			params.setToolParameters(null);
			params.setModelName(null);
			params.setErrorMessage(e.getMessage());
			planExecutionRecorder.recordThinkingAndAction(params);

			return false;
		}

	}

	private boolean executeWithRetry(int maxRetries) throws Exception {
		int attempt = 0;
		while (attempt < maxRetries) {
			attempt++;
			Message systemMessage = getThinkMessage();
			// Use current env as user message
			Message currentStepEnvMessage = currentStepEnvMessage();
			// Record think message
			List<Message> thinkMessages = Arrays.asList(systemMessage, currentStepEnvMessage);
			String thinkInput = thinkMessages.toString();

			log.debug("Messages prepared for the prompt: {}", thinkMessages);
			// Build current prompt. System message is the first message
			List<Message> messages = new ArrayList<>(Collections.singletonList(systemMessage));
			// Add history message.
			ChatMemory chatMemory = llmService.getAgentMemory(manusProperties.getMaxMemory());
			List<Message> historyMem = chatMemory.get(getCurrentPlanId());
			messages.addAll(historyMem);
			messages.add(currentStepEnvMessage);
			// Call the LLM
			ChatOptions chatOptions = ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
			userPrompt = new Prompt(messages, chatOptions);
			List<ToolCallback> callbacks = getToolCallList();
			ChatClient chatClient;
			if (model == null) {
				chatClient = llmService.getAgentChatClient();
			}
			else {
				chatClient = llmService.getDynamicChatClient(model.getBaseUrl(), model.getApiKey(),
						model.getModelName());
			}
			response = chatClient.prompt(userPrompt).toolCallbacks(callbacks).call().chatResponse();
			String modelName = response.getMetadata().getModel();

			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
			String responseByLLm = response.getResult().getOutput().getText();

			log.info(String.format("✨ %s's thoughts: %s", getName(), responseByLLm));
			log.info(String.format("🛠️ %s selected %d tools to use", getName(), toolCalls.size()));

			if (!toolCalls.isEmpty()) {
				log.info(String.format("🧰 Tools being prepared: %s",
						toolCalls.stream().map(ToolCall::name).collect(Collectors.toList())));

				// 记录成功的思考和动作准备
				String toolName = toolCalls.get(0).name();
				String toolParameters = toolCalls.get(0).arguments();
				PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
				params.setCurrentPlanId(getCurrentPlanId());
				params.setRootPlanId(getRootPlanId());
				params.setThinkActRecordId(getThinkActRecordId());
				params.setAgentName(getName());
				params.setAgentDescription(getDescription());
				params.setThinkInput(thinkInput);
				params.setThinkOutput(responseByLLm);
				params.setActionNeeded(true);
				params.setToolName(toolName);
				params.setToolParameters(toolParameters);
				params.setModelName(modelName);
				params.setErrorMessage(null);
				currentThinkActRecordId = planExecutionRecorder.recordThinkingAndAction(params);

				return true;
			}
			log.warn("Attempt {}: No tools selected. Retrying...", attempt);
		}

		// 记录思考失败（没有选择工具）
		PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
		params.setCurrentPlanId(getCurrentPlanId());
		params.setRootPlanId(getRootPlanId());
		params.setThinkActRecordId(getThinkActRecordId());
		params.setAgentName(getName());
		params.setAgentDescription(getDescription());
		params.setThinkInput(null);
		params.setThinkOutput("No tools selected after retries");
		params.setActionNeeded(false);
		params.setToolName(null);
		params.setToolParameters(null);
		params.setModelName(null);
		params.setErrorMessage("Failed to select tools after " + maxRetries + " attempts");
		planExecutionRecorder.recordThinkingAndAction(params);

		return false;
	}

	@Override
	protected AgentExecResult act() {
		ToolExecutionResult toolExecutionResult = null;

		try {
			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
			ToolCall toolCall = toolCalls.get(0);
			String toolName = toolCall.name();
			String toolParameters = toolCall.arguments();
			String actionDescription = "Executing tool: " + toolName;

			toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);

			processMemory(toolExecutionResult);
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
				.get(toolExecutionResult.conversationHistory().size() - 1);

			String llmCallResponse = toolResponseMessage.getResponses().get(0).responseData();

			log.info(String.format("🔧 Tool %s's executing result: %s", getName(), llmCallResponse));

			String toolcallName = toolCall.name();

			// Get the tool instance based on toolCallName
			ToolCallBiFunctionDef<?> toolInstance = getToolCallBackContext(toolcallName).getFunctionInstance();

			// Handle FormInputTool logic
			if (toolInstance instanceof FormInputTool) {
				FormInputTool formInputTool = (FormInputTool) toolInstance;
				// Check if the tool is waiting for user input
				if (formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) {
					log.info("FormInputTool is awaiting user input for planId: {}", getCurrentPlanId());
					userInputService.storeFormInputTool(getCurrentPlanId(), formInputTool);
					// Wait for user input or timeout
					waitForUserInputOrTimeout(formInputTool);

					// After waiting, check the state again
					if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_RECEIVED) {
						log.info("User input received for planId: {}", getCurrentPlanId());
						// The UserInputService.submitUserInputs would have updated
						// the tool's internal state.
						// We can now get the updated state string for the LLM.

						UserMessage userMessage = UserMessage.builder()
							.text("User input received for form: " + formInputTool.getCurrentToolStateString())
							.build();
						processUserInputToMemory(userMessage); // Process user input
						// to memory
						llmCallResponse = formInputTool.getCurrentToolStateString();

					}
					else if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_TIMEOUT) {
						log.warn("Input timeout occurred for FormInputTool for planId: {}", getCurrentPlanId());
						// Handle input timeout

						UserMessage userMessage = UserMessage.builder()
							.text("Input timeout occurred for form: ")
							.build();
						processUserInputToMemory(userMessage);
						userInputService.removeFormInputTool(getCurrentPlanId()); // Clean
																					// up

						// 记录输入超时的动作结果
						PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
						params.setCurrentPlanId(getCurrentPlanId());
						params.setRootPlanId(getRootPlanId());
						params.setThinkActRecordId(getThinkActRecordId());
						params.setCreatedThinkActRecordId(currentThinkActRecordId);
						params.setActionDescription(actionDescription);
						params.setActionResult("Input timeout occurred");
						params.setStatus("TIMEOUT");
						params.setErrorMessage("Input timeout occurred for FormInputTool");
						params.setToolName(toolName);
						params.setToolParameters(toolParameters);
						params.setSubPlanCreated(false);
						planExecutionRecorder.recordActionResult(params);

						return new AgentExecResult("Input timeout occurred.", AgentState.IN_PROGRESS);
					}
				}
			}

			// Handle TerminableTool logic
			if (toolInstance instanceof TerminableTool) {
				TerminableTool terminableTool = (TerminableTool) toolInstance;
				// Use canTerminate() to decide whether to terminate
				if (terminableTool.canTerminate()) {
					log.info("TerminableTool can terminate for planId: {}", getCurrentPlanId());
					userInputService.removeFormInputTool(getCurrentPlanId()); // Clean up
																				// any
																				// pending
																				// form

					// 记录成功完成的动作结果
					PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
					params.setCurrentPlanId(getCurrentPlanId());
					params.setRootPlanId(getRootPlanId());
					params.setThinkActRecordId(getThinkActRecordId());
					params.setCreatedThinkActRecordId(currentThinkActRecordId);
					params.setActionDescription(actionDescription);
					params.setActionResult(llmCallResponse);
					params.setStatus("COMPLETED");
					params.setErrorMessage(null);
					params.setToolName(toolName);
					params.setToolParameters(toolParameters);
					params.setSubPlanCreated(false);
					planExecutionRecorder.recordActionResult(params);

					return new AgentExecResult(llmCallResponse, AgentState.COMPLETED);
				}
				else {
					log.info("TerminableTool cannot terminate yet for planId: {}", getCurrentPlanId());
				}
			}

			// 记录成功的动作结果
			PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
			params.setCurrentPlanId(getCurrentPlanId());
			params.setRootPlanId(getRootPlanId());
			params.setThinkActRecordId(getThinkActRecordId());
			params.setCreatedThinkActRecordId(currentThinkActRecordId);
			params.setActionDescription(actionDescription);
			params.setActionResult(llmCallResponse);
			params.setStatus("SUCCESS");
			params.setErrorMessage(null);
			params.setToolName(toolName);
			params.setToolParameters(toolParameters);
			params.setSubPlanCreated(false);
			planExecutionRecorder.recordActionResult(params);

			return new AgentExecResult(llmCallResponse, AgentState.IN_PROGRESS);
		}
		catch (Exception e) {

			log.error(e.getMessage());
			log.info("Exception occurred", e);

			// 记录失败的动作结果
			String toolName = null;
			String toolParameters = null;
			String actionDescription = "Tool execution failed";
			if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
				List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
				if (!toolCalls.isEmpty()) {
					toolName = toolCalls.get(0).name();
					toolParameters = toolCalls.get(0).arguments();
					actionDescription = "Executing tool: " + toolName;
				}
			}

			PlanExecutionRecorder.PlanExecutionParams params = new PlanExecutionRecorder.PlanExecutionParams();
			params.setCurrentPlanId(getCurrentPlanId());
			params.setRootPlanId(getRootPlanId());
			params.setThinkActRecordId(getThinkActRecordId());
			params.setCreatedThinkActRecordId(currentThinkActRecordId);
			params.setActionDescription(actionDescription);
			params.setActionResult(null);
			params.setStatus("FAILED");
			params.setErrorMessage(e.getMessage());
			params.setToolName(toolName);
			params.setToolParameters(toolParameters);
			params.setSubPlanCreated(false);
			planExecutionRecorder.recordActionResult(params);

			userInputService.removeFormInputTool(getCurrentPlanId()); // Clean up on error
			processMemory(toolExecutionResult); // Process memory even on error
			return new AgentExecResult(e.getMessage(), AgentState.FAILED);
		}

	}

	private void processUserInputToMemory(UserMessage userMessage) {
		if (userMessage != null && userMessage.getText() != null) {
			// Process the user message to update memory
			String userInput = userMessage.getText();

			if (!StringUtils.isBlank(userInput)) {
				// Add user input to memory

				llmService.getAgentMemory(manusProperties.getMaxMemory()).add(getCurrentPlanId(), userMessage);

			}
		}
	}

	private void processMemory(ToolExecutionResult toolExecutionResult) {
		if (toolExecutionResult == null) {
			return;
		}
		// Process the conversation history to update memory
		List<Message> messages = toolExecutionResult.conversationHistory();
		if (messages.isEmpty()) {
			return;
		}
		// clear current plan memory
		llmService.getAgentMemory(manusProperties.getMaxMemory()).clear(getCurrentPlanId());
		for (Message message : messages) {
			// exclude all system message
			if (message instanceof SystemMessage) {
				continue;
			}
			// exclude env data message
			if (message instanceof UserMessage userMessage
					&& userMessage.getMetadata().containsKey(CURRENT_STEP_ENV_DATA_KEY)) {
				continue;
			}
			// only keep assistant message and tool_call message
			llmService.getAgentMemory(manusProperties.getMaxMemory()).add(getCurrentPlanId(), message);
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
		PromptTemplate promptTemplate = new SystemPromptTemplate(this.nextStepPrompt);
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
	protected Message getThinkMessage() {
		Message baseThinkPrompt = super.getThinkMessage();
		Message nextStepWithEnvMessage = getNextStepWithEnvMessage();
		SystemMessage thinkMessage = new SystemMessage(
				baseThinkPrompt.getText() + System.lineSeparator() + nextStepWithEnvMessage.getText());
		return thinkMessage;
	}

	/**
	 * Current step env data
	 * @return User message for current step environment data
	 */
	private Message currentStepEnvMessage() {
		Message stepEnvMessage = promptService.createUserMessage(PromptEnum.AGENT_CURRENT_STEP_ENV.getPromptName(),
				getMergedData());
		// mark as current step env data
		stepEnvMessage.getMetadata().put(CURRENT_STEP_ENV_DATA_KEY, Boolean.TRUE);
		return stepEnvMessage;
	}

	public ToolCallBackContext getToolCallBackContext(String toolKey) {
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
		// If corresponding tool callback context is not found, return empty string
		return "";
	}

	public void collectAndSetEnvDataForTools() {

		Map<String, Object> toolEnvDataMap = new HashMap<>();

		Map<String, Object> oldMap = getEnvData();
		toolEnvDataMap.putAll(oldMap);

		// Overwrite old data with new data
		for (String toolKey : availableToolKeys) {
			String envData = collectEnvData(toolKey);
			toolEnvDataMap.put(toolKey, envData);
		}
		log.debug("Collected tool environment data: {}", toolEnvDataMap);

		setEnvData(toolEnvDataMap);
	}

	public String convertEnvDataToString() {
		StringBuilder envDataStringBuilder = new StringBuilder();

		for (String toolKey : availableToolKeys) {
			Object value = getEnvData().get(toolKey);
			if (value == null || value.toString().isEmpty()) {
				continue; // Skip tools with no data
			}
			envDataStringBuilder.append(toolKey).append(" context information:\n");
			envDataStringBuilder.append("    ").append(value.toString()).append("\n");
		}

		return envDataStringBuilder.toString();
	}

	// Add a method to wait for user input or handle timeout.
	private void waitForUserInputOrTimeout(FormInputTool formInputTool) {
		log.info("Waiting for user input for planId: {}...", getCurrentPlanId());
		long startTime = System.currentTimeMillis();
		// Get timeout from ManusProperties and convert to milliseconds
		long userInputTimeoutMs = getManusProperties().getUserInputTimeout() * 1000L;

		while (formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) {
			if (System.currentTimeMillis() - startTime > userInputTimeoutMs) {
				log.warn("Timeout waiting for user input for planId: {}", getCurrentPlanId());
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
				log.warn("Interrupted while waiting for user input for planId: {}", getCurrentPlanId());
				Thread.currentThread().interrupt();
				formInputTool.handleInputTimeout(); // Treat interruption as timeout for
				// simplicity
				break;
			}
		}
		if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_RECEIVED) {
			log.info("User input received for planId: {}", getCurrentPlanId());
		}
		else if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_TIMEOUT) {
			log.warn("User input timed out for planId: {}", getCurrentPlanId());
		}
	}

}
