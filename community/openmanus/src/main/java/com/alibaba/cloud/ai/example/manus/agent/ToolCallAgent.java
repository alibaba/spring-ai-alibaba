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

import com.alibaba.cloud.ai.example.manus.llm.LlmService;

import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;

import com.alibaba.cloud.ai.example.manus.llm.ToolBuilder;
import com.alibaba.cloud.ai.example.manus.tool.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

public class ToolCallAgent extends ReActAgent {

	private static final Logger log = LoggerFactory.getLogger(ToolCallAgent.class);

	private static final Integer REPLY_MAX = 3;

	private final ToolCallingManager toolCallingManager;

	protected final ToolBuilder toolBuilder;

	private ChatResponse response;

	private Prompt userPrompt;

	public ToolCallAgent(LlmService llmService, ToolCallingManager toolCallingManager, ToolBuilder toolBuilder) {
		super(llmService);
		this.toolCallingManager = toolCallingManager;
		this.toolBuilder = toolBuilder;
	}

	@Override
	protected boolean think() {
		int retry = 0;
		return _think(retry);
	}

	@Override
	public String getDescription() {
		return "ToolCallAgent: A class responsible for managing tool calls in the ReAct agent.";
	}

	@Override
	public String getName() {
		return "ToolCallAgent";
	}

	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String stepPrompt = """
				CURRENT PLAN STATUS:
				{planStatus}

				FOCUS ON CURRENT STEP:
				You are now working on step {currentStepIndex} : {stepText}

				EXECUTION GUIDELINES:
				1. Focus ONLY on completing the current step's requirements
				2. Use appropriate tools to accomplish the task
				3. DO NOT proceed to next steps until current step is fully complete
				4. Verify all requirements are met before marking as complete

				COMPLETION PROTOCOL:
				Once you have FULLY completed the current step:
				1. MUST call Summary tool with following information:
				- Detailed results of what was accomplished
				- Any relevant data or metrics
				- Status confirmation
				2. The Summary tool call will automatically:
				- Mark this step as complete
				- Save the results
				- Enable progression to next step

				⚠️ IMPORTANT:
				- Stay focused on current step only
				- Do not skip or combine steps
				- Only call Summary tool when current step is 100% complete
				- Provide comprehensive summary before moving forward

				""";

		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(stepPrompt);

		Message systemMessage = promptTemplate.createMessage(getData());

		messages.add(systemMessage);
		return systemMessage;
	}

	protected Message getNextStepMessage() {

		String nextStepPrompt = """
				What is the next step you would like to take?
				Please provide the step number or the name of the next step.
				""";

		return new UserMessage(nextStepPrompt);
	}

	private boolean _think(int retry) {
		try {
			List<Message> messages = new ArrayList<>();
			addThinkPrompt(messages);

			// calltool with mem
			ChatOptions chatOptions = ToolCallingChatOptions.builder()
					.internalToolExecutionEnabled(false)
					.build();
			Message nextStepMessage = getNextStepMessage();
			messages.add(nextStepMessage);

			log.debug("Messages prepared for the prompt: {}", messages);

			userPrompt = new Prompt(messages, chatOptions);

			response = llmService.getChatClient()
					.prompt(userPrompt)
					.advisors(memoryAdvisor -> memoryAdvisor.param(CHAT_MEMORY_CONVERSATION_ID_KEY, getConversationId())
							.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
					.tools(getFunctionToolCallbacks())
					.call()
					.chatResponse();

			List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();

			log.info(String.format("✨ %s's thoughts: %s", getName(), response.getResult().getOutput().getText()));
			log.info(String.format("🛠️ %s selected %d tools to use", getName(), toolCalls.size()));
			String responseByLLm = response.getResult().getOutput().getText();
			if (responseByLLm != null && !responseByLLm.isEmpty()) {
				log.info(String.format("💬 %s's response: %s", getName(), responseByLLm));
			}
			if (!toolCalls.isEmpty()) {
				log.info(String.format("🧰 Tools being prepared: %s",
						toolCalls.stream().map(ToolCall::name).collect(Collectors.toList())));
			}

			return !toolCalls.isEmpty();
		} catch (Exception e) {
			log.error(String.format("🚨 Oops! The %s's thinking process hit a snag: %s", getName(), e.getMessage()));
			// 异常重试
			if (retry < REPLY_MAX) {
				return _think(retry + 1);
			}
			return false;
		}
	}

	@Override
	protected String act() {
		try {
			List<String> results = new ArrayList<>();

			ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(userPrompt, response);
			ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
					.get(toolExecutionResult.conversationHistory().size() - 1);
			llmService.getMemory().add(getConversationId(), toolResponseMessage);
			String llmCallResponse = toolResponseMessage.getResponses().get(0).responseData();
			results.add(llmCallResponse);
			log.info(String.format("🔧 Tool %s's executing result: %s", getName(), llmCallResponse));
			return String.join("\n\n", results);
		} catch (Exception e) {
			ToolCall toolCall = response.getResult().getOutput().getToolCalls().get(0);
			ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(toolCall.id(),
					toolCall.name(), "Error: " + e.getMessage());
			ToolResponseMessage toolResponseMessage = new ToolResponseMessage(List.of(toolResponse), Map.of());
			llmService.getMemory().add(getConversationId(), toolResponseMessage);
			log.error(e.getMessage());
			return "Error: " + e.getMessage();
		}
	}

	protected List<ToolCallback> getFunctionToolCallbacks() {
		return toolBuilder.getManusAgentToolCalls(this, llmService.getMemory(), getConversationId());
	}

}
