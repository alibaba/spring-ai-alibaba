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

package com.alibaba.cloud.ai.agent.studio.service;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for creating and caching Runner instances. */
@Component
public class RunnerService {
	private static final Logger log = LoggerFactory.getLogger(RunnerService.class);

	private final AgentLoader agentProvider;

	public RunnerService(
			AgentLoader agentProvider) {
		this.agentProvider = agentProvider;
	}

	/** Called by hot loader when agents are updated */
	public Agent getAgent(String agentName) {
		return agentProvider.loadAgent(agentName);
	}
}
