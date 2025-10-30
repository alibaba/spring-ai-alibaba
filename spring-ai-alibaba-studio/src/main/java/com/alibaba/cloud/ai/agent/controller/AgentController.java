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

package com.alibaba.cloud.ai.agent.controller;

import com.alibaba.cloud.ai.agent.loader.AgentLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/** Spring Boot REST Controller handling agent-related API endpoints. */
@RestController
public class AgentController {

	private static final Logger log = LoggerFactory.getLogger(AgentController.class);

	private final AgentLoader agentProvider;

	/**
	 * Constructs the AgentController.
	 *
	 * @param agentProvider The provider for loading agents.
	 */
	@Autowired
	public AgentController(AgentLoader agentProvider) {
		this.agentProvider = agentProvider;
		List<String> agentNames = this.agentProvider.listAgents();
		log.info(
				"AgentController initialized with {} dynamic agents: {}", agentNames.size(), agentNames);
		if (agentNames.isEmpty()) {
			log.warn(
					"Agent registry is empty. Check 'saa.agents.source-dir' property and compilation"
							+ " logs.");
		}
	}

	/**
	 * Lists available applications. Currently returns only the configured root agent's name.
	 *
	 * @return A list containing the root agent's name.
	 */
	@GetMapping("/list-apps")
	public List<String> listApps() {
		List<String> agentNames = agentProvider.listAgents();
		log.info("Listing apps from dynamic registry. Found: {}", agentNames);
		return agentNames.stream().sorted().collect(toList());
	}
}
