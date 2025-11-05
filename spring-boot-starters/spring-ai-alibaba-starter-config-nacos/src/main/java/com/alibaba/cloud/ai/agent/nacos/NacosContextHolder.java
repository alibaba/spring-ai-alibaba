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

package com.alibaba.cloud.ai.agent.nacos;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;

public class NacosContextHolder {

	AgentVO agentVO;

	PromptVO promptVO;

	ModelVO modelVO;

	McpServersVO mcpServersVO;

	ObservationMetadataAwareOptions observationMetadataAwareOptions;

	ReactAgent reactAgent;

	Map<String, PromptListener> promptListeners = new HashMap<>();

	public AgentVO getAgentVO() {
		return agentVO;
	}

	public void setAgentVO(AgentVO agentVO) {
		this.agentVO = agentVO;
	}

	public PromptVO getPromptVO() {
		return promptVO;
	}

	public void setPromptVO(PromptVO promptVO) {
		this.promptVO = promptVO;
	}

	public ModelVO getModelVO() {
		return modelVO;
	}

	public void setModelVO(ModelVO modelVO) {
		this.modelVO = modelVO;
	}

	public McpServersVO getMcpServersVO() {
		return mcpServersVO;
	}

	public void setMcpServersVO(McpServersVO mcpServersVO) {
		this.mcpServersVO = mcpServersVO;
	}

	public ObservationMetadataAwareOptions getObservationMetadataAwareOptions() {
		return observationMetadataAwareOptions;
	}

	public void setObservationMetadataAwareOptions(ObservationMetadataAwareOptions observationMetadataAwareOptions) {
		this.observationMetadataAwareOptions = observationMetadataAwareOptions;
	}

	public ReactAgent getReactAgent() {
		return reactAgent;
	}

	public void setReactAgent(ReactAgent reactAgent) {
		this.reactAgent = reactAgent;
	}

	public Map<String, PromptListener> getPromptListeners() {
		return promptListeners;
	}

	public void setPromptListeners(Map<String, PromptListener> promptListeners) {
		this.promptListeners = promptListeners;
	}

}
