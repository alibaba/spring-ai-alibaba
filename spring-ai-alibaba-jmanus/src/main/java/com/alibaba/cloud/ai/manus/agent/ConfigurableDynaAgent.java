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
import com.alibaba.cloud.ai.manus.prompt.service.PromptService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.UserInputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ConfigurableDynaAgent - A flexible agent that allows passing tool lists dynamically
 * This agent can be configured with different tool sets at runtime and extends
 * DynamicAgent to inherit all the core functionality while adding configurable tool
 * management.
 */
public class ConfigurableDynaAgent extends DynamicAgent {

	private static final Logger log = LoggerFactory.getLogger(ConfigurableDynaAgent.class);

	private final List<String> availableToolKeys;

	/**
	 * Constructor for ConfigurableDynaAgent with configurable parameters
	 * @param llmService LLM service
	 * @param planExecutionRecorder Plan execution recorder
	 * @param manusProperties Manus properties
	 * @param name Agent name (configurable)
	 * @param description Agent description (configurable)
	 * @param nextStepPrompt Next step prompt (configurable)
	 * @param availableToolKeys List of available tool keys (can be null/empty)
	 * @param toolCallingManager Tool calling manager
	 * @param initialAgentSetting Initial agent settings
	 * @param userInputService User input service
	 * @param promptService Prompt service
	 * @param model Dynamic model entity
	 * @param streamingResponseHandler Streaming response handler
	 * @param step Execution step
	 * @param planIdDispatcher Plan ID dispatcher
	 */
	public ConfigurableDynaAgent(ILlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, String name, String description, String nextStepPrompt,
			List<String> availableToolKeys, ToolCallingManager toolCallingManager,
			Map<String, Object> initialAgentSetting, UserInputService userInputService, PromptService promptService,
			DynamicModelEntity model, StreamingResponseHandler streamingResponseHandler, ExecutionStep step,
			PlanIdDispatcher planIdDispatcher) {
		super(llmService, planExecutionRecorder, manusProperties, name, description, nextStepPrompt, availableToolKeys,
				toolCallingManager, initialAgentSetting, userInputService, promptService, model,
				streamingResponseHandler, step, planIdDispatcher);
		this.availableToolKeys = availableToolKeys != null ? new ArrayList<>(availableToolKeys) : new ArrayList<>();
		boolean hasTerminateTool = false;
		for (String toolKey : availableToolKeys) {
			if (toolKey.equals(com.alibaba.cloud.ai.manus.tool.TerminateTool.name)) {
				hasTerminateTool = true;
				break;
			}
		}
		if (!hasTerminateTool) {
			availableToolKeys.add(com.alibaba.cloud.ai.manus.tool.TerminateTool.name);
		}
	}

	/**
	 * Add a single tool key to the available tools
	 * @param toolKey Tool key to add
	 */
	public void addToolKey(String toolKey) {
		if (toolKey != null && !availableToolKeys.contains(toolKey)) {
			availableToolKeys.add(toolKey);
			log.info("Added tool {} to agent {}", toolKey, getName());
		}
	}

	/**
	 * Remove a single tool key from the available tools
	 * @param toolKey Tool key to remove
	 */
	public void removeToolKey(String toolKey) {
		if (toolKey != null && availableToolKeys.remove(toolKey)) {
			log.info("Removed tool {} from agent {}", toolKey, getName());
		}
	}

	/**
	 * Get the current available tool keys
	 * @return List of currently available tool keys
	 */
	public List<String> getAvailableToolKeys() {
		return new ArrayList<>(availableToolKeys);
	}

	/**
	 * Clear all available tool keys
	 */
	public void clearToolKeys() {
		availableToolKeys.clear();
		log.info("Cleared all tools for agent {}", getName());
	}

	/**
	 * Check if a specific tool key is available
	 * @param toolKey Tool key to check
	 * @return true if the tool is available, false otherwise
	 */
	public boolean hasToolKey(String toolKey) {
		return availableToolKeys.contains(toolKey);
	}

	/**
	 * Get the number of available tools
	 * @return Number of available tools
	 */
	public int getToolCount() {
		return availableToolKeys.size();
	}

	/**
	 * Set the available tool keys for this agent This allows overriding the default tools
	 * with user-selected tools
	 * @param toolKeys List of tool keys to make available to this agent
	 */
	public void setAvailableToolKeys(List<String> toolKeys) {
		availableToolKeys.clear();
		if (toolKeys != null) {
			availableToolKeys.addAll(toolKeys);
		}
		log.info("Updated available tools for agent {}: {}", getName(), availableToolKeys);
	}

	/**
	 * Add multiple tool keys at once
	 * @param toolKeys List of tool keys to add
	 */
	public void addToolKeys(List<String> toolKeys) {
		if (toolKeys != null) {
			for (String toolKey : toolKeys) {
				addToolKey(toolKey);
			}
		}
	}

	/**
	 * Remove multiple tool keys at once
	 * @param toolKeys List of tool keys to remove
	 */
	public void removeToolKeys(List<String> toolKeys) {
		if (toolKeys != null) {
			for (String toolKey : toolKeys) {
				removeToolKey(toolKey);
			}
		}
	}

	/**
	 * Check if the agent has any tools configured
	 * @return true if the agent has tools, false otherwise
	 */
	public boolean hasTools() {
		return !availableToolKeys.isEmpty();
	}

	/**
	 * Get a copy of the current tool configuration as a string
	 * @return String representation of current tool configuration
	 */
	public String getToolConfigurationSummary() {
		return String.format("Agent '%s' has %d tools: %s", getName(), availableToolKeys.size(),
				availableToolKeys.isEmpty() ? "none" : String.join(", ", availableToolKeys));
	}

}
