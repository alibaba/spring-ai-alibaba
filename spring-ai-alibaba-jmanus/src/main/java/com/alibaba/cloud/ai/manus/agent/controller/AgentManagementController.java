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
package com.alibaba.cloud.ai.manus.agent.controller;

import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.model.enums.AgentEnum;
import com.alibaba.cloud.ai.manus.agent.service.AgentInitializationService;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent management REST API controller
 */
@RestController
@RequestMapping("/api/agent-management")
@CrossOrigin(origins = "*")
public class AgentManagementController {

	private static final Logger logger = LoggerFactory.getLogger(AgentManagementController.class);

	@Autowired
	private AgentInitializationService agentInitializationService;

	@Autowired
	private AgentService agentService;

	@Value("${namespace.value}")
	private String namespace;

	/**
	 * Get all agents for current namespace
	 */
	@GetMapping
	public ResponseEntity<List<DynamicAgentEntity>> getAllAgents() {
		try {
			List<DynamicAgentEntity> agents = agentService.getAllAgents();
			return ResponseEntity.ok(agents);
		}
		catch (Exception e) {
			logger.error("Error getting all agents", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Get supported languages
	 */
	@GetMapping("/languages")
	public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
		try {
			String[] languages = AgentEnum.getSupportedLanguages();
			return ResponseEntity.ok(Map.of("languages", languages, "default", "en"));
		}
		catch (Exception e) {
			logger.error("Error getting supported languages", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Reset all agents to specific language
	 */
	@PostMapping("/reset")
	public ResponseEntity<Map<String, String>> resetAllAgents(@RequestBody Map<String, String> request) {
		try {
			String language = request.get("language");
			if (language == null || language.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Language parameter is required"));
			}

			// Validate language
			boolean isValidLanguage = false;
			for (String supportedLang : AgentEnum.getSupportedLanguages()) {
				if (supportedLang.equals(language)) {
					isValidLanguage = true;
					break;
				}
			}

			if (!isValidLanguage) {
				return ResponseEntity.badRequest().body(Map.of("error", "Unsupported language: " + language));
			}

			logger.info("Resetting all agents to language: {} for namespace: {}", language, namespace);
			agentInitializationService.resetAllAgentsToLanguage(namespace, language);

			return ResponseEntity.ok(Map.of("message", "All agents have been reset to language: " + language,
					"language", language, "namespace", namespace));
		}
		catch (Exception e) {
			logger.error("Error resetting all agents", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to reset agents: " + e.getMessage()));
		}
	}

	/**
	 * Initialize agents for specific language (used during initial setup)
	 */
	@PostMapping("/initialize")
	public ResponseEntity<Map<String, String>> initializeAgents(@RequestBody Map<String, String> request) {
		try {
			String language = request.get("language");
			if (language == null || language.trim().isEmpty()) {
				language = "en"; // Default to English
			}

			logger.info("Initializing agents with language: {} for namespace: {}", language, namespace);
			agentInitializationService.initializeAgentsForNamespaceWithLanguage(namespace, language);

			return ResponseEntity.ok(Map.of("message", "Agents initialized successfully with language: " + language,
					"language", language, "namespace", namespace));
		}
		catch (Exception e) {
			logger.error("Error initializing agents", e);
			return ResponseEntity.internalServerError()
				.body(Map.of("error", "Failed to initialize agents: " + e.getMessage()));
		}
	}

	/**
	 * Get agent statistics
	 */
	@GetMapping("/stats")
	public ResponseEntity<Map<String, Object>> getAgentStats() {
		try {
			List<DynamicAgentEntity> agents = agentService.getAllAgents();

			return ResponseEntity.ok(Map.of("total", agents.size(), "namespace", namespace, "supportedLanguages",
					AgentEnum.getSupportedLanguages()));
		}
		catch (Exception e) {
			logger.error("Error getting agent statistics", e);
			return ResponseEntity.internalServerError().build();
		}
	}

}
