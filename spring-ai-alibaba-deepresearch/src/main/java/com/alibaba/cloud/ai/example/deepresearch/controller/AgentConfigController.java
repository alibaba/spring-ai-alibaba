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
package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.agents.AgentFactory;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import com.alibaba.cloud.ai.example.deepresearch.service.ModelConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent-config")
public class AgentConfigController {

	private final ModelConfigService modelConfigService;

	private final AgentFactory agentFactory;

	private static final Logger logger = LoggerFactory.getLogger(AgentConfigController.class);

	public AgentConfigController(ModelConfigService modelConfigService, AgentFactory agentFactory) {
		this.modelConfigService = modelConfigService;
		this.agentFactory = agentFactory;
	}

	/**
	 * 获取所有模型配置
	 */
	@GetMapping("/getModelConfigs")
	public ResponseEntity<List<ModelParamRepositoryImpl.AgentModel>> getModelConfigs() {
		try {
			return ResponseEntity.ok(modelConfigService.getModelConfigs());
		}
		catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 批量更新模型配置
	 */
	@PostMapping("/updateModelConfigs")
	public ResponseEntity<Void> updateModelConfigs(@RequestBody List<ModelParamRepositoryImpl.AgentModel> models) {
		try {
			modelConfigService.updateModelConfigs(models);
			return ResponseEntity.ok().build();
		}
		catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 调用指定Agent模型
	 */
	@PostMapping(value = "/agent/call", produces = MediaType.APPLICATION_JSON_VALUE)
	public String call(@RequestBody Map<String, Object> message) {
		logger.info("Received chat request: {}", message);
		ChatClient chatClient = agentFactory.getAgentByName((String) message.get("agentName"));
		return chatClient.prompt((String) message.get("message")).call().content();
	}

}
