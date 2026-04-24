/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for multi-agent A2A server support.
 * <p>
 * Supports registering multiple agents from a single application:
 * <pre>
 * spring:
 *   ai:
 *     alibaba:
 *       a2a:
 *         server:
 *           agents:
 *             weather-agent:
 *               name: "Weather Agent"
 *               description: "Provides weather information"
 *             translate-agent:
 *               name: "Translate Agent"
 *               description: "Translation service"
 * </pre>
 * <p>
 * Each agent will be accessible at its own URL path:
 * <ul>
 *   <li>weather-agent: /a2a/weather-agent</li>
 *   <li>translate-agent: /a2a/translate-agent</li>
 * </ul>
 * <p>
 * Note: Multi-agent mode (using 'agents') and single-agent mode (using 'card')
 * are mutually exclusive. Do not use both configurations simultaneously.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = A2aMultiAgentProperties.CONFIG_PREFIX)
public class A2aMultiAgentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.a2a.server";

	/**
	 * Multi-agent configuration map.
	 * Key is the agent identifier (used in URL path), value is the agent card properties.
	 */
	private Map<String, A2aAgentCardProperties> agents = new LinkedHashMap<>();

	public Map<String, A2aAgentCardProperties> getAgents() {
		return agents;
	}

	public void setAgents(Map<String, A2aAgentCardProperties> agents) {
		this.agents = agents;
	}

	/**
	 * Check if multi-agent mode is enabled (agents map is not empty).
	 * @return true if multi-agent mode is enabled
	 */
	public boolean isMultiAgentMode() {
		return agents != null && !agents.isEmpty();
	}

}
