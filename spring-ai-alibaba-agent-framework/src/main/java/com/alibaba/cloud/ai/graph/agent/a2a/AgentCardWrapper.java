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

package com.alibaba.cloud.ai.graph.agent.a2a;

import java.util.List;
import java.util.Map;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.SecurityScheme;

/**
 * The Wrapper of AgentCard.
 *
 * @author xiweng.yy
 */
public class AgentCardWrapper {

	private AgentCard agentCard;

	public AgentCardWrapper(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	public String name() {
		return this.agentCard.name();
	}

	public String description() {
		return this.agentCard.description();
	}

	public String url() {
		return this.agentCard.url();
	}

	public AgentProvider provider() {
		return this.agentCard.provider();
	}

	public String version() {
		return this.agentCard.version();
	}

	public String documentationUrl() {
		return this.agentCard.documentationUrl();
	}

	public AgentCapabilities capabilities() {
		return this.agentCard.capabilities();
	}

	public List<String> defaultInputModes() {
		return this.agentCard.defaultInputModes();
	}

	public List<String> defaultOutputModes() {
		return this.agentCard.defaultOutputModes();
	}

	public List<AgentSkill> skills() {
		return this.agentCard.skills();
	}

	public boolean supportsAuthenticatedExtendedCard() {
		return this.agentCard.supportsAuthenticatedExtendedCard();
	}

	public Map<String, SecurityScheme> securitySchemes() {
		return this.agentCard.securitySchemes();
	}

	public List<Map<String, List<String>>> security() {
		return this.agentCard.security();
	}

	public String iconUrl() {
		return this.agentCard.iconUrl();
	}

	public List<AgentInterface> additionalInterfaces() {
		return this.agentCard.additionalInterfaces();
	}

	public String preferredTransport() {
		return this.agentCard.preferredTransport();
	}

	public String protocolVersion() {
		return this.agentCard.protocolVersion();
	}

	public AgentCard getAgentCard() {
		return agentCard;
	}

	public void setAgentCard(AgentCard agentCard) {
		this.agentCard = agentCard;
	}
}
