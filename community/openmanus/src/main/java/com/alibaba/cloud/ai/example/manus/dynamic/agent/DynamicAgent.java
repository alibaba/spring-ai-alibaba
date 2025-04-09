
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

import com.alibaba.cloud.ai.example.manus.agent.ReActAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

public class DynamicAgent extends ReActAgent {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgent.class);

	private final String agentName;

	private final String agentDescription;

	private final String systemPrompt;

	private final String nextStepPrompt;

	private Map<String, ToolCallBackContext> toolCallbackMap;

	private final List<String> availableToolKeys;

	private ChatResponse response;

	private Prompt userPrompt;

	protected ThinkActRecord thinkActRecord;

	private final ToolCallingManager toolCallingManager;

	private static final String EXECUTION_ENV_KEY_STRING = "current_step_env_data";

	public DynamicAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, String name, String description, String systemPrompt,
			String nextStepPrompt, List<String> availableToolKeys, ToolCallingManager toolCallingManager) {
		super(llmService, planExecutionRecorder, manusProperties);
		this.agentName = name;
		this.agentDescription = description;
		this.systemPrompt = systemPrompt;
		this.nextStepPrompt = nextStepPrompt;
		this.availableToolKeys = availableToolKeys;
		this.toolCallingManager = toolCallingManager;
	}

	@Override
	protected boolean think() {
		AgentExecutionRecord planExecutionRecord = planExecutionRecorder.getCurrentAgentExecutionRecord(getPlanId());
		thinkActRecord = new ThinkActRecord(planExecutionRecord.getId());
		planExecutionRecorder.recordThinkActExecution(getPlanId(), planExecutionRecord.getId(), thinkActRecord);

		try {
			List<Message> messages = new ArrayList<>();
			addThinkPrompt(messages);

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

	@Override
	protected String act() {
		try {
			List<String> results = new ArrayList<>();
			ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);

			thinkActRecord.startAction("Executing tool: " + toolCall.name(), toolCall.name(), toolCall.arguments());
			ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);

			addEnvData(EXECUTION_ENV_KEY_STRING, collectEnvData(toolCall.name()));
			setData(getData());
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
		String nextStepPrompt = """

				CURRENT STEP ENVIRONMENT STATUS:
				{current_step_env_data}

				""";
		nextStepPrompt = nextStepPrompt += this.nextStepPrompt;
		PromptTemplate promptTemplate = new PromptTemplate(nextStepPrompt);
		Message userMessage = promptTemplate.createMessage(getData());
		return userMessage;
	}

	@Override
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(this.systemPrompt);
		Message systemMessage = promptTemplate.createMessage(getData());
		messages.add(systemMessage);
		return systemMessage;
	}

	@Override
	public List<ToolCallback> getToolCallList() {
		List<ToolCallback> toolCallbacks = new ArrayList<>();
		for (String toolKey : availableToolKeys) {
			if (toolCallbackMap.containsKey(toolKey)) {
				ToolCallBackContext toolCallback = toolCallbackMap.get(toolKey);
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
		Map<String, Object> data = super.getData();
		if (data == null) {
			throw new IllegalStateException("Data map is null. Cannot add environment data.");
		}
		data.put(key, value);
	}

	public void setToolCallbackMap(Map<String, ToolCallBackContext> toolCallbackMap) {
		this.toolCallbackMap = toolCallbackMap;
	}

	protected String collectEnvData(String toolCallName) {
		ToolCallBackContext context = toolCallbackMap.get(toolCallName);
		if (context != null) {
			return context.getFunctionInstance().getCurrentToolStateString();
		}
		// 如果没有找到对应的工具回调上下文，返回空字符串
		return "";
	}

}
