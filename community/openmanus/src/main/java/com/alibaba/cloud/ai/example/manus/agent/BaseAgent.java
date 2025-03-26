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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An abstract base class for implementing AI agents that can execute multi-step tasks.
 * This class provides the core functionality for managing agent state, conversation flow,
 * and step-by-step execution of tasks.
 *
 * <p>The agent supports a finite number of execution steps and includes mechanisms for:
 * <ul>
 *   <li>State management (idle, running, finished)</li>
 *   <li>Conversation tracking</li>
 *   <li>Step limitation and monitoring</li>
 *   <li>Thread-safe execution</li>
 *   <li>Stuck-state detection and handling</li>
 * </ul>
 *
 * <p>Implementing classes must define:
 * <ul>
 *   <li>{@link #getName()} - Returns the agent's name</li>
 *   <li>{@link #getDescription()} - Returns the agent's description</li>
 *   <li>{@link #addThinkPrompt(List)} - Implements the thinking chain logic</li>
 *   <li>{@link #getNextStepMessage()} - Provides the next step's prompt template</li>
 *   <li>{@link #step()} - Implements the core logic for each execution step</li>
 * </ul>
 *
 * @see AgentState
 * @see LlmService
 */
public abstract class BaseAgent {

	private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

	private final ReentrantLock lock = new ReentrantLock();

	private String conversationId;
	
	private AgentState state = AgentState.IDLE;

	protected LlmService llmService;

	private int maxSteps = 8;

	private int currentStep = 0;

	private Map<String, Object> data = new HashMap<>();

	public abstract String getName();
	public abstract String getDescription();
	/**
	 * 递归的增加思考提示，形成一个自上而下的思考链条
	 * @param messages
	 * @return
	 */
	protected abstract Message addThinkPrompt(List<Message> messages);
	/**
	 * 获取下一步提示词模板，这个跟着具体的agent走，因为每个agent不一样，也不需要继承
	 * @return
	 */
	protected abstract Message getNextStepMessage();

	public abstract List<ToolCallback> getToolCallList();

	public BaseAgent(LlmService llmService) {
		this.llmService = llmService;
	}

	public String run(Map<String, Object> data) {
		currentStep = 0;
		if (state != AgentState.IDLE) {
			throw new IllegalStateException("Cannot run agent from state: " + state);
		}

		setData(data);

		List<String> results = new ArrayList<>();
		lock.lock();
		try {
			state = AgentState.RUNNING;
			while (currentStep < maxSteps && !state.equals(AgentState.FINISHED)) {
				currentStep++;
				log.info("Executing round " + currentStep + "/" + maxSteps);
				String stepResult = step();
				if (isStuck()) {
					handleStuckState();
				}
				results.add("Round " + currentStep + ": " + stepResult);
			}
			if (currentStep >= maxSteps) {
				results.add("Terminated: Reached max rounds (" + maxSteps + ")");
			}
		}
		finally {
			lock.unlock();
			state = AgentState.IDLE; // Reset state after execution
		}
		return String.join("\n", results);
	}

	protected abstract String step();

	private void handleStuckState() {
		log.warn("Agent stuck detected - Missing tool calls");
		
		// End current step
		setState(AgentState.FINISHED);
		
		String stuckPrompt = """
			Agent response detected missing required tool calls.
			Please ensure each response includes at least one tool call to progress the task.
			Current step: %d
			Execution status: Force terminated
			""".formatted(currentStep);
			
		log.error(stuckPrompt);
	}

	
    /**
     * 检查是否处于卡住状态
     */
    protected boolean isStuck() {
        //目前判断是如果三次没有调用工具就认为是卡住了，就退出当前step。
		List<Message> memoryEntries = llmService.getMemory().get(conversationId, 6);
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

	Map<String, Object> getData() {
		return data;
	}

	void setData(Map<String, Object> data) {
		this.data = data;
	}

}
