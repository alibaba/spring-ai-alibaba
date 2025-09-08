/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.generator.model.agent;

import java.util.List;
import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/25 17:28
 */
public class Agent {

	// 基础属性
	private String agentClass; // ReactAgent, SequentialAgent, ParallelAgent.etc

	private String name;

	private String description;

	private String outputKey;

	private String inputKey;

	// 支持多输入键（与 schema: input_keys 对齐）
	private List<String> inputKeys;

	// LLM相关配置
	private String model;

	private String instruction;

	private Integer maxIterations;

	private Map<String, Object> chatOptions;

	// 工具配置
	private List<String> tools;

	private Map<String, Object> toolConfig;

	// 子agent配置
	private List<Agent> subAgents;

	// 流程控制配置
	private Map<String, Object> flowConfig;

	// 状态管理配置
	private Map<String, String> stateConfig;

	// 钩子配置
	private Map<String, Object> hooks;

	// 动态 handle：原样透传每种 agent type 的专属配置
	private Map<String, Object> handle;

	public Agent() {
	}

	public String getAgentClass() {
		return agentClass;
	}

	public void setAgentClass(String agentClass) {
		this.agentClass = agentClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public String getInputKey() {
		return inputKey;
	}

	public void setInputKey(String inputKey) {
		this.inputKey = inputKey;
	}

	public List<String> getInputKeys() {
		return inputKeys;
	}

	public void setInputKeys(List<String> inputKeys) {
		this.inputKeys = inputKeys;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public Integer getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(Integer maxIterations) {
		this.maxIterations = maxIterations;
	}

	public Map<String, Object> getChatOptions() {
		return chatOptions;
	}

	public void setChatOptions(Map<String, Object> chatOptions) {
		this.chatOptions = chatOptions;
	}

	public List<String> getTools() {
		return tools;
	}

	public void setTools(List<String> tools) {
		this.tools = tools;
	}

	public Map<String, Object> getToolConfig() {
		return toolConfig;
	}

	public void setToolConfig(Map<String, Object> toolConfig) {
		this.toolConfig = toolConfig;
	}

	public List<Agent> getSubAgents() {
		return subAgents;
	}

	public void setSubAgents(List<Agent> subAgents) {
		this.subAgents = subAgents;
	}

	public Map<String, Object> getFlowConfig() {
		return flowConfig;
	}

	public void setFlowConfig(Map<String, Object> flowConfig) {
		this.flowConfig = flowConfig;
	}

	public Map<String, String> getStateConfig() {
		return stateConfig;
	}

	public void setStateConfig(Map<String, String> stateConfig) {
		this.stateConfig = stateConfig;
	}

	public Map<String, Object> getHooks() {
		return hooks;
	}

	public void setHooks(Map<String, Object> hooks) {
		this.hooks = hooks;
	}

	public Map<String, Object> getHandle() {
		return handle;
	}

	public void setHandle(Map<String, Object> handle) {
		this.handle = handle;
	}

}
