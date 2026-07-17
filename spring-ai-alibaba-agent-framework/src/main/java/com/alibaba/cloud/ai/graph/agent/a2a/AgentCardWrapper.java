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

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentProvider;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.Legacy_0_3_AgentInterface;
import org.a2aproject.sdk.spec.SecurityScheme;

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
		return preferredInterface().url();
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
		return this.agentCard.capabilities().extendedAgentCard();
	}

	public Map<String, SecurityScheme> securitySchemes() {
		return this.agentCard.securitySchemes();
	}

	public List<Map<String, List<String>>> security() {
		if (this.agentCard.securityRequirements() == null) {
			return List.of();
		}
		return this.agentCard.securityRequirements().stream().map(requirement -> requirement.schemes()).toList();
	}

	public String iconUrl() {
		return this.agentCard.iconUrl();
	}

	public List<AgentInterface> additionalInterfaces() {
		if (this.agentCard.supportedInterfaces() != null && !this.agentCard.supportedInterfaces().isEmpty()) {
			return this.agentCard.supportedInterfaces();
		}
		if (this.agentCard.additionalInterfaces() == null) {
			return List.of();
		}
		return this.agentCard.additionalInterfaces()
			.stream()
			.map(agentInterface -> new AgentInterface(agentInterface.transport(), agentInterface.url()))
			.toList();
	}

	public String preferredTransport() {
		return preferredInterface().protocolBinding();
	}

	public String protocolVersion() {
		return preferredInterface().protocolVersion();
	}

	public AgentCard getAgentCard() {
		return agentCard;
	}

	public void setAgentCard(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	private AgentInterface preferredInterface() {
		if (this.agentCard.supportedInterfaces() != null && !this.agentCard.supportedInterfaces().isEmpty()) {
			return this.agentCard.supportedInterfaces().get(0);
		}
		if (this.agentCard.url() != null) {
			String transport = this.agentCard.preferredTransport() == null ? "JSONRPC"
					: this.agentCard.preferredTransport();
			return new AgentInterface(transport, this.agentCard.url());
		}
		List<Legacy_0_3_AgentInterface> additionalInterfaces = this.agentCard.additionalInterfaces();
		if (additionalInterfaces != null && !additionalInterfaces.isEmpty()) {
			Legacy_0_3_AgentInterface agentInterface = additionalInterfaces.get(0);
			return new AgentInterface(agentInterface.transport(), agentInterface.url());
		}
		throw new IllegalStateException("Agent card does not declare a supported interface");
	}
}
