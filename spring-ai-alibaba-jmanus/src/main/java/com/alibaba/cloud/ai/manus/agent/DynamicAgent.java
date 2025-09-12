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
package com.alibaba.cloud.ai.manus.agent;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.llm.StreamingResponseHandler;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.manus.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.manus.prompt.service.PromptService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder.ActToolParam;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder.ThinkActRecordParams;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.executor.PlanExecutor;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.UserInputService;
import com.alibaba.cloud.ai.manus.tool.FormInputTool;
import com.alibaba.cloud.ai.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.manus.tool.ToolCallBiFunctionDef;
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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DynamicAgent extends ReActAgent {

	private static final String CURRENT_STEP_ENV_DATA_KEY = "current_step_env_data";

	private static final Logger log = LoggerFactory.getLogger(DynamicAgent.class);

	private final String agentName;

	private final String agentDescription;

	private final String nextStepPrompt;

	private ToolCallbackProvider toolCallbackProvider;

	private final List<String> availableToolKeys;

	private ChatResponse response;

	private StreamingResponseHandler.StreamingResult streamResult;

	private Prompt userPrompt;

	private List<ActToolParam> actToolInfoList = new ArrayList<>();

	private final ToolCallingManager toolCallingManager;

	private final UserInputService userInputService;

	private final DynamicModelEntity model;

	private final StreamingResponseHandler streamingResponseHandler;

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

	public DynamicAgent(ILlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, String name, String description, String nextStepPrompt,
			List<String> availableToolKeys, ToolCallingManager toolCallingManager,
			Map<String, Object> initialAgentSetting, UserInputService userInputService, PromptService promptService,
			DynamicModelEntity model, StreamingResponseHandler streamingResponseHandler, ExecutionStep step,
			PlanIdDispatcher planIdDispatcher) {
		super(llmService, planExecutionRecorder, manusProperties, initialAgentSetting, promptService, step,
				planIdDispatcher);
		this.agentName = name;
		this.agentDescription = description;
		this.nextStepPrompt = nextStepPrompt;
		this.availableToolKeys = availableToolKeys;
		this.toolCallingManager = toolCallingManager;
		this.userInputService = userInputService;
		this.model = model;
		this.streamingResponseHandler = streamingResponseHandler;
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
			return false;
		}

	}

	private boolean executeWithRetry(int maxRetries) throws Exception {
		int attempt = 0;
		Exception lastException = null;

		while (attempt < maxRetries) {
			attempt++;
			try {
				log.info("Attempt {}/{}: Executing agent thinking process", attempt, maxRetries);

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
				String toolcallId = planIdDispatcher.generateToolCallId();
				// Call the LLM
				ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
					.internalToolExecutionEnabled(false)
					.toolContext(Map.of("toolcallId", toolcallId))
					// can't support by toocall options :
					// .parallelToolCalls(manusProperties.getParallelToolCalls())
					.build();
				userPrompt = new Prompt(messages, chatOptions);
				List<ToolCallback> callbacks = getToolCallList();
				ChatClient chatClient;
				if (model == null) {
					chatClient = llmService.getAgentChatClient();
				}
				else {
					chatClient = llmService.getDynamicChatClient(model);
				}
				// Use streaming response handler for better user experience and content
				// merging
				Flux<ChatResponse> responseFlux = chatClient.prompt(userPrompt)
					.toolCallbacks(callbacks)
					.stream()
					.chatResponse();
				streamResult = streamingResponseHandler.processStreamingResponse(responseFlux,
						"Agent " + getName() + " thinking", getCurrentPlanId());

				response = streamResult.getLastResponse();

				// Use merged content from streaming handler
				List<ToolCall> toolCalls = streamResult.getEffectiveToolCalls();
				String responseByLLm = streamResult.getEffectiveText();

				log.info(String.format("✨ %s's thoughts: %s", getName(), responseByLLm));
				log.info(String.format("🛠️ %s selected %d tools to use", getName(), toolCalls.size()));

				if (!toolCalls.isEmpty()) {
					log.info(String.format("🧰 Tools being prepared: %s",
							toolCalls.stream().map(ToolCall::name).collect(Collectors.toList())));

					String stepId = super.step.getStepId();
					String thinkActId = planIdDispatcher.generateThinkActId();

					actToolInfoList = new ArrayList<>();
					for (ToolCall toolCall : toolCalls) {
						ActToolParam actToolInfo = new ActToolParam(toolCall.name(), toolCall.arguments(), toolcallId);
						actToolInfoList.add(actToolInfo);
					}

					ThinkActRecordParams paramsN = new ThinkActRecordParams(thinkActId, stepId, thinkInput,
							responseByLLm, null, actToolInfoList);
					planExecutionRecorder.recordThinkingAndAction(step, paramsN);
					return true;
				}
				log.warn("Attempt {}: No tools selected. Retrying...", attempt);

			}
			catch (Exception e) {
				lastException = e;
				log.warn("Attempt {} failed: {}", attempt, e.getMessage());

				// Check if this is a network-related error that should be retried
				if (isRetryableException(e)) {
					if (attempt < maxRetries) {
						long waitTime = calculateBackoffDelay(attempt);
						log.info("Retrying in {}ms due to retryable error: {}", waitTime, e.getMessage());
						try {
							Thread.sleep(waitTime);
						}
						catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							throw new Exception("Retry interrupted", ie);
						}
						continue;
					}
				}
				else {
					// Non-retryable error, throw immediately
					throw e;
				}
			}
		}

		// All retries exhausted
		if (lastException != null) {
			throw new Exception("All retry attempts failed. Last error: " + lastException.getMessage(), lastException);
		}
		return false;
	}

	/**
	 * Check if the exception is retryable (network issues, timeouts, etc.)
	 */
	private boolean isRetryableException(Exception e) {
		String message = e.getMessage();
		if (message == null)
			return false;

		// Check for network-related errors
		return message.contains("Failed to resolve") || message.contains("timeout") || message.contains("connection")
				|| message.contains("DNS") || message.contains("WebClientRequestException")
				|| message.contains("DnsNameResolverTimeoutException");
	}

	/**
	 * Calculate exponential backoff delay
	 */
	private long calculateBackoffDelay(int attempt) {
		// Exponential backoff: 2^attempt * 1000ms, max 30 seconds
		long delay = Math.min(1000L * (1L << (attempt - 1)), 30000L);
		return delay;
	}

	@Override
	protected AgentExecResult act() {
		ToolExecutionResult toolExecutionResult = null;
		try {
			List<ToolCall> toolCalls = streamResult.getEffectiveToolCalls();
			// Execute tool calls
			toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);
			processMemory(toolExecutionResult);

			// Get tool response messages
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
				.get(toolExecutionResult.conversationHistory().size() - 1);

			// Get execution result of the last tool
			List<String> resultList = new ArrayList<>();
			boolean shouldTerminate = false;
			int executedToolCount = 0;

			if (!toolResponseMessage.getResponses().isEmpty()) {
				for (ToolResponseMessage.ToolResponse toolCallResponse : toolResponseMessage.getResponses()) {
					ToolCall toolCall = toolCalls.get(executedToolCount);
					String toolName = toolCall.name();
					ActToolParam param = actToolInfoList.get(executedToolCount);

					ToolCallBiFunctionDef<?> toolInstance = getToolCallBackContext(toolName).getFunctionInstance();

					if (toolInstance instanceof FormInputTool) {
						AgentExecResult formResult = handleFormInputTool((FormInputTool) toolInstance, param);
						param.setResult(formResult.getResult());
						resultList.add(param.getResult());
					}
					else if (toolInstance instanceof TerminableTool) {
						TerminableTool terminableTool = (TerminableTool) toolInstance;
						param.setResult(toolCallResponse.responseData());
						resultList.add(param.getResult());

						if (terminableTool.canTerminate()) {
							log.info("TerminableTool can terminate for planId: {}", getCurrentPlanId());
							userInputService.removeFormInputTool(getCurrentPlanId());
							shouldTerminate = true;
							executedToolCount++;
							break; // Stop processing remaining tools when termination is
									// indicated
						}
						else {
							log.info("TerminableTool cannot terminate yet for planId: {}", getCurrentPlanId());
						}
					}
					else {
						param.setResult(toolCallResponse.responseData());
						resultList.add(toolCallResponse.responseData());
						log.info("Tool {} executed successfully for planId: {}", toolName, getCurrentPlanId());
					}
					executedToolCount++;
				}

				// Record the results of executed tools
				List<ActToolParam> executedTools = actToolInfoList.subList(0, executedToolCount);
				recordActionResult(executedTools);

				// Return result with appropriate state
				return new AgentExecResult(resultList.toString(),
						shouldTerminate ? AgentState.COMPLETED : AgentState.IN_PROGRESS);
			}
			return new AgentExecResult("tool call is empty", AgentState.IN_PROGRESS);

		}
		catch (Exception e) {
			log.error(e.getMessage());
			log.info("Exception occurred", e);

			StringBuilder errorMessage = new StringBuilder("Error executing tools: ");
			errorMessage.append(e.getMessage());

			String firstToolcall = actToolInfoList != null && !actToolInfoList.isEmpty()
					&& actToolInfoList.get(0).getParameters() != null
							? actToolInfoList.get(0).getParameters().toString() : "unknown";
			errorMessage.append("  . llm return param :  ").append(firstToolcall);

			userInputService.removeFormInputTool(getCurrentPlanId()); // Clean up on error
			processMemory(toolExecutionResult); // Process memory even on error
			return new AgentExecResult(e.getMessage(), AgentState.FAILED);
		}
	}

	/**
	 * Handle FormInputTool specific logic
	 */
	private AgentExecResult handleFormInputTool(FormInputTool formInputTool, ActToolParam param) {
		// Check if the tool is waiting for user input
		if (formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) {
			log.info("FormInputTool is awaiting user input for planId: {}", getCurrentPlanId());
			userInputService.storeFormInputTool(getCurrentPlanId(), formInputTool);
			// Wait for user input or timeout
			waitForUserInputOrTimeout(formInputTool);

			// After waiting, check the state again
			if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_RECEIVED) {
				log.info("User input received for planId: {}", getCurrentPlanId());

				UserMessage userMessage = UserMessage.builder()
					.text("User input received for form: " + formInputTool.getCurrentToolStateString())
					.build();
				processUserInputToMemory(userMessage);

				// Update the result in actToolInfoList
				param.setResult(formInputTool.getCurrentToolStateString());
				return new AgentExecResult(param.getResult(), AgentState.IN_PROGRESS);

			}
			else if (formInputTool.getInputState() == FormInputTool.InputState.INPUT_TIMEOUT) {
				log.warn("Input timeout occurred for FormInputTool for planId: {}", getCurrentPlanId());

				UserMessage userMessage = UserMessage.builder().text("Input timeout occurred for form: ").build();
				processUserInputToMemory(userMessage);
				userInputService.removeFormInputTool(getCurrentPlanId());
				param.setResult("Input timeout occurred");

				return new AgentExecResult("Input timeout occurred.", AgentState.IN_PROGRESS);
			}
			else {
				throw new RuntimeException("FormInputTool is not in the correct state");
			}
		}
		else {
			throw new RuntimeException("FormInputTool is not in the correct state");
		}
	}

	/**
	 * Record action result with simplified parameters
	 */
	private void recordActionResult(List<ActToolParam> actToolInfoList) {
		planExecutionRecorder.recordActionResult(actToolInfoList);
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
		SystemMessage thinkMessage = new SystemMessage("""
				<SystemInfo>
				%s
				</SystemInfo>

				<AgentInfo>
				%s
				</AgentInfo>
				""".formatted(baseThinkPrompt.getText(), nextStepWithEnvMessage.getText()));
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
		log.info("🔍 collectEnvData called for tool: {}", toolCallName);
		ToolCallBackContext context = toolCallbackProvider.getToolCallBackContext().get(toolCallName);
		if (context != null) {
			String envData = context.getFunctionInstance().getCurrentToolStateString();
			log.info("📊 Tool '{}' env data: {}", toolCallName, envData);
			return envData;
		}
		// If corresponding tool callback context is not found, return empty string
		log.warn("⚠️ No context found for tool: {}", toolCallName);
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
