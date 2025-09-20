/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.registry.nacos.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentInterface;
import com.alibaba.nacos.api.ai.model.a2a.AgentProvider;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import com.alibaba.nacos.api.ai.model.a2a.SecurityScheme;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Agent card converter util.
 */
public class AgentCardConverterUtil {

	public static io.a2a.spec.AgentCard convertToA2aAgentCard(AgentCard agentCard) {
		if (agentCard == null) {
			return null;
		}

		return new io.a2a.spec.AgentCard.Builder().protocolVersion(agentCard.getProtocolVersion())
			.name(agentCard.getName())
			.description(agentCard.getDescription())
			.version(agentCard.getVersion())
			.iconUrl(agentCard.getIconUrl())
			.capabilities(convertToA2aAgentCapabilities(agentCard.getCapabilities()))
			.skills(convertToA2aAgentSkills(agentCard.getSkills()))
			.url(agentCard.getUrl())
			.preferredTransport(agentCard.getPreferredTransport())
			.additionalInterfaces(convertToA2aAgentInterfaces(agentCard.getAdditionalInterfaces()))
			.provider(convertToA2aAgentProvider(agentCard.getProvider()))
			.documentationUrl(agentCard.getDocumentationUrl())
			.securitySchemes(convertToA2aAgentSecuritySchemes(agentCard.getSecuritySchemes()))
			.security(agentCard.getSecurity())
			.defaultInputModes(agentCard.getDefaultInputModes())
			.defaultOutputModes(agentCard.getDefaultOutputModes())
			.supportsAuthenticatedExtendedCard(agentCard.getSupportsAuthenticatedExtendedCard())
			.build();
	}

	private static Map<String, io.a2a.spec.SecurityScheme> convertToA2aAgentSecuritySchemes(
			Map<String, SecurityScheme> securitySchemes) {
		if (null == securitySchemes) {
			return null;
		}
		String securitySchemesJson = JacksonUtils.toJson(securitySchemes);
		return JacksonUtils.toObj(securitySchemesJson, new TypeReference<>() {
		});
	}

	private static io.a2a.spec.AgentProvider convertToA2aAgentProvider(AgentProvider provider) {
		if (null == provider) {
			return null;
		}
		return new io.a2a.spec.AgentProvider(provider.getOrganization(), provider.getUrl());
	}

	private static List<io.a2a.spec.AgentInterface> convertToA2aAgentInterfaces(List<AgentInterface> nacosInterfaces) {
		if (nacosInterfaces == null) {
			return List.of();
		}
		return nacosInterfaces.stream()
			.map(AgentCardConverterUtil::transferAgentInterface)
			.collect(Collectors.toList());
	}

	private static io.a2a.spec.AgentInterface transferAgentInterface(AgentInterface agentInterface) {
		if (null == agentInterface) {
			return null;
		}
		return new io.a2a.spec.AgentInterface(agentInterface.getTransport(), agentInterface.getUrl());
	}

	private static io.a2a.spec.AgentCapabilities convertToA2aAgentCapabilities(
			com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities) {
		if (nacosCapabilities == null) {
			return null;
		}

		return new io.a2a.spec.AgentCapabilities.Builder().streaming(nacosCapabilities.getStreaming())
			.pushNotifications(nacosCapabilities.getPushNotifications())
			.stateTransitionHistory(nacosCapabilities.getStateTransitionHistory())
			.build();
	}

	private static List<io.a2a.spec.AgentSkill> convertToA2aAgentSkills(List<AgentSkill> nacosSkills) {
		if (nacosSkills == null) {
			return null;
		}

		return nacosSkills.stream().map(AgentCardConverterUtil::transferAgentSkill).collect(Collectors.toList());
	}

	private static io.a2a.spec.AgentSkill transferAgentSkill(AgentSkill nacosSkill) {
		return new io.a2a.spec.AgentSkill.Builder().id(nacosSkill.getId())
			.tags(nacosSkill.getTags())
			.examples(nacosSkill.getExamples())
			.name(nacosSkill.getName())
			.description(nacosSkill.getDescription())
			.inputModes(nacosSkill.getInputModes())
			.outputModes(nacosSkill.getOutputModes())
			.build();
	}

	public static AgentCard convertToNacosAgentCard(io.a2a.spec.AgentCard agentCard) {
		AgentCard card = new AgentCard();
		card.setProtocolVersion(agentCard.protocolVersion());
		card.setName(agentCard.name());
		card.setDescription(agentCard.description());
		card.setVersion(agentCard.version());
		card.setIconUrl(agentCard.iconUrl());
		card.setCapabilities(convertToNacosAgentCapabilities(agentCard.capabilities()));
		card.setSkills(agentCard.skills().stream().map(AgentCardConverterUtil::convertToNacosAgentSkill).toList());
		card.setUrl(agentCard.url());
		card.setPreferredTransport(agentCard.preferredTransport());
		card.setAdditionalInterfaces(convertToNacosAgentInterfaces(agentCard.additionalInterfaces()));
		card.setProvider(convertToNacosAgentProvider(agentCard.provider()));
		card.setDocumentationUrl(agentCard.documentationUrl());
		card.setSecuritySchemes(convertToNacosSecuritySchemes(agentCard.securitySchemes()));
		card.setSecurity(agentCard.security());
		card.setDefaultInputModes(agentCard.defaultInputModes());
		card.setDefaultOutputModes(agentCard.defaultOutputModes());
		card.setSupportsAuthenticatedExtendedCard(agentCard.supportsAuthenticatedExtendedCard());
		return card;
	}

	private static AgentCapabilities convertToNacosAgentCapabilities(io.a2a.spec.AgentCapabilities capabilities) {
		com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities = new com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities();
		nacosCapabilities.setStreaming(capabilities.streaming());
		nacosCapabilities.setPushNotifications(capabilities.pushNotifications());
		nacosCapabilities.setStateTransitionHistory(capabilities.stateTransitionHistory());
		return nacosCapabilities;
	}

	private static AgentSkill convertToNacosAgentSkill(io.a2a.spec.AgentSkill agentSkill) {
		AgentSkill skill = new AgentSkill();
		skill.setId(agentSkill.id());
		skill.setName(agentSkill.name());
		skill.setDescription(agentSkill.description());
		skill.setTags(agentSkill.tags());
		skill.setExamples(agentSkill.examples());
		skill.setInputModes(agentSkill.inputModes());
		skill.setOutputModes(agentSkill.outputModes());
		return skill;
	}

	private static List<AgentInterface> convertToNacosAgentInterfaces(
			List<io.a2a.spec.AgentInterface> agentInterfaces) {
		if (agentInterfaces == null) {
			return List.of();
		}
		return agentInterfaces.stream()
			.map(AgentCardConverterUtil::convertToNacosAgentInterface)
			.collect(Collectors.toList());
	}

	private static AgentInterface convertToNacosAgentInterface(io.a2a.spec.AgentInterface agentInterface) {
		AgentInterface nacosAgentInterface = new AgentInterface();
		nacosAgentInterface.setUrl(agentInterface.url());
		nacosAgentInterface.setTransport(agentInterface.transport());
		return nacosAgentInterface;
	}

	private static AgentProvider convertToNacosAgentProvider(io.a2a.spec.AgentProvider agentProvider) {
		if (null == agentProvider) {
			return null;
		}
		AgentProvider nacosAgentProvider = new AgentProvider();
		nacosAgentProvider.setOrganization(agentProvider.organization());
		nacosAgentProvider.setUrl(agentProvider.url());
		return nacosAgentProvider;
	}

	private static Map<String, SecurityScheme> convertToNacosSecuritySchemes(
			Map<String, io.a2a.spec.SecurityScheme> securitySchemes) {
		if (securitySchemes == null) {
			return null;
		}
		String originalJson = JacksonUtils.toJson(securitySchemes);
		return JacksonUtils.toObj(originalJson, new TypeReference<>() {
		});
	}

}
