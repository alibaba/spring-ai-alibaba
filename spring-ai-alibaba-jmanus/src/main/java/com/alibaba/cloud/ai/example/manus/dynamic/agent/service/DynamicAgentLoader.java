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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.DynamicAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import com.alibaba.cloud.ai.example.manus.prompt.PromptLoader;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

@Service
public class DynamicAgentLoader {

	private final DynamicAgentRepository repository;

	private final LlmService llmService;

	private final PlanExecutionRecorder recorder;

	private final ManusProperties properties;

	private final ToolCallingManager toolCallingManager;

	private final UserInputService userInputService;

	private final PromptLoader promptLoader;

	public DynamicAgentLoader(DynamicAgentRepository repository, @Lazy LlmService llmService,
			PlanExecutionRecorder recorder, ManusProperties properties, @Lazy ToolCallingManager toolCallingManager,
			UserInputService userInputService, PromptLoader promptLoader) {
		this.repository = repository;
		this.llmService = llmService;
		this.recorder = recorder;
		this.properties = properties;
		this.toolCallingManager = toolCallingManager;
		this.userInputService = userInputService;
		this.promptLoader = promptLoader;
	}

	public DynamicAgent loadAgent(String agentName, Map<String, Object> initialAgentSetting) {
		DynamicAgentEntity entity = repository.findByAgentName(agentName);
		if (entity == null) {
			throw new IllegalArgumentException("Agent not found: " + agentName);
		}

		return new DynamicAgent(llmService, recorder, properties, entity.getAgentName(), entity.getAgentDescription(),
				entity.getNextStepPrompt(), entity.getAvailableToolKeys(), toolCallingManager, initialAgentSetting,
				userInputService, promptLoader);
	}

	public List<DynamicAgentEntity> getAllAgents() {
		return repository.findAll();
	}

}
