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

package com.alibaba.cloud.ai.a2a.registry.nacos.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;

/**
 * Agent card converter util.
 */
public class AgentCardConverterUtil {

	public static org.a2aproject.sdk.spec.AgentCard convertToA2aAgentCard(AgentCard agentCard) {
		if (agentCard == null) {
			return null;
		}

		return org.a2aproject.sdk.spec.AgentCard.builder().name(agentCard.getName())
			.description(agentCard.getDescription())
			.version(agentCard.getVersion())
			.iconUrl(agentCard.getIconUrl())
			.capabilities(convertToA2aAgentCapabilities(agentCard.getCapabilities(),
					Boolean.TRUE.equals(agentCard.getSupportsAuthenticatedExtendedCard())))
			.skills(convertToA2aAgentSkills(agentCard.getSkills()))
			.supportedInterfaces(convertToA2aAgentInterfaces(agentCard))
			.provider(convertToA2aAgentProvider(agentCard.getProvider()))
			.documentationUrl(agentCard.getDocumentationUrl())
			.securitySchemes(convertToA2aAgentSecuritySchemes(agentCard.getSecuritySchemes()))
			.securityRequirements(agentCard.getSecurity() == null ? null
					: agentCard.getSecurity().stream().map(org.a2aproject.sdk.spec.SecurityRequirement::new).toList())
			.defaultInputModes(agentCard.getDefaultInputModes() == null ? List.of() : agentCard.getDefaultInputModes())
			.defaultOutputModes(agentCard.getDefaultOutputModes() == null ? List.of() : agentCard.getDefaultOutputModes())
			.build();
	}

	private static Map<String, org.a2aproject.sdk.spec.SecurityScheme> convertToA2aAgentSecuritySchemes(
			Map<String, SecurityScheme> securitySchemes) {
		if (null == securitySchemes) {
			return null;
		}
		Map<String, org.a2aproject.sdk.spec.SecurityScheme> converted = new LinkedHashMap<>();
		securitySchemes.forEach((name, scheme) -> converted.put(name, convertToA2aSecurityScheme(name, scheme)));
		return converted;
	}

	private static org.a2aproject.sdk.spec.SecurityScheme convertToA2aSecurityScheme(String name,
			SecurityScheme securityScheme) {
		Map<String, Object> scheme = new LinkedHashMap<>(securityScheme);
		String discriminator;
		if (scheme.size() == 1 && scheme.values().iterator().next() instanceof Map<?, ?>) {
			discriminator = scheme.keySet().iterator().next();
		}
		else {
			discriminator = switch (String.valueOf(scheme.remove("type"))) {
				case "apiKey" -> "apiKeySecurityScheme";
				case "http" -> "httpAuthSecurityScheme";
				case "oauth2" -> "oauth2SecurityScheme";
				case "openIdConnect" -> "openIdConnectSecurityScheme";
				case "mutualTLS" -> "mtlsSecurityScheme";
				default -> throw new IllegalArgumentException("Unsupported security scheme type for " + name);
			};
			if ("apiKeySecurityScheme".equals(discriminator) && scheme.containsKey("in")) {
				scheme.put("location", scheme.remove("in"));
			}
			scheme = Map.of(discriminator, scheme);
		}
		try {
			return JsonUtil.fromJson(JacksonUtils.toJson(scheme), org.a2aproject.sdk.spec.SecurityScheme.class);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to convert security scheme " + name, e);
		}
	}

	private static org.a2aproject.sdk.spec.AgentProvider convertToA2aAgentProvider(AgentProvider provider) {
		if (null == provider) {
			return null;
		}
		return new org.a2aproject.sdk.spec.AgentProvider(provider.getOrganization(), provider.getUrl());
	}

	private static List<org.a2aproject.sdk.spec.AgentInterface> convertToA2aAgentInterfaces(AgentCard agentCard) {
		List<org.a2aproject.sdk.spec.AgentInterface> interfaces = new ArrayList<>();
		if (agentCard.getUrl() != null) {
			interfaces.add(new org.a2aproject.sdk.spec.AgentInterface(
					agentCard.getPreferredTransport() == null ? "JSONRPC" : agentCard.getPreferredTransport(),
					agentCard.getUrl(), null, agentCard.getProtocolVersion()));
		}
		if (agentCard.getAdditionalInterfaces() != null) {
			agentCard.getAdditionalInterfaces()
				.stream()
				.map(agentInterface -> transferAgentInterface(agentInterface, agentCard.getProtocolVersion()))
				.filter(agentInterface -> agentInterface != null && !interfaces.contains(agentInterface))
				.forEach(interfaces::add);
		}
		return interfaces;
	}

	private static org.a2aproject.sdk.spec.AgentInterface transferAgentInterface(AgentInterface agentInterface,
			String protocolVersion) {
		if (null == agentInterface) {
			return null;
		}
		return new org.a2aproject.sdk.spec.AgentInterface(agentInterface.getTransport(), agentInterface.getUrl(), null,
				protocolVersion);
	}

	private static org.a2aproject.sdk.spec.AgentCapabilities convertToA2aAgentCapabilities(
			com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities, boolean extendedAgentCard) {
		if (nacosCapabilities == null) {
			return org.a2aproject.sdk.spec.AgentCapabilities.builder().extendedAgentCard(extendedAgentCard).build();
		}

		return org.a2aproject.sdk.spec.AgentCapabilities.builder()
			.streaming(Boolean.TRUE.equals(nacosCapabilities.getStreaming()))
			.pushNotifications(Boolean.TRUE.equals(nacosCapabilities.getPushNotifications()))
			.extendedAgentCard(extendedAgentCard)
			.build();
	}

	private static List<org.a2aproject.sdk.spec.AgentSkill> convertToA2aAgentSkills(List<AgentSkill> nacosSkills) {
		if (nacosSkills == null) {
			return List.of();
		}

		return nacosSkills.stream().map(AgentCardConverterUtil::transferAgentSkill).collect(Collectors.toList());
	}

	private static org.a2aproject.sdk.spec.AgentSkill transferAgentSkill(AgentSkill nacosSkill) {
		return org.a2aproject.sdk.spec.AgentSkill.builder().id(nacosSkill.getId())
			.tags(nacosSkill.getTags())
			.examples(nacosSkill.getExamples())
			.name(nacosSkill.getName())
			.description(nacosSkill.getDescription())
			.inputModes(nacosSkill.getInputModes())
			.outputModes(nacosSkill.getOutputModes())
			.build();
	}

	public static AgentCard convertToNacosAgentCard(org.a2aproject.sdk.spec.AgentCard agentCard) {
		AgentCard card = new AgentCard();
		org.a2aproject.sdk.spec.AgentInterface primaryInterface = agentCard.supportedInterfaces().isEmpty() ? null
				: agentCard.supportedInterfaces().get(0);
		card.setProtocolVersion(primaryInterface == null ? null : primaryInterface.protocolVersion());
		card.setName(agentCard.name());
		card.setDescription(agentCard.description());
		card.setVersion(agentCard.version());
		card.setIconUrl(agentCard.iconUrl());
		card.setCapabilities(convertToNacosAgentCapabilities(agentCard.capabilities()));
		card.setSkills(agentCard.skills().stream().map(AgentCardConverterUtil::convertToNacosAgentSkill).toList());
		card.setUrl(primaryInterface == null ? null : primaryInterface.url());
		card.setPreferredTransport(primaryInterface == null ? null : primaryInterface.protocolBinding());
		card.setAdditionalInterfaces(convertToNacosAgentInterfaces(agentCard.supportedInterfaces()));
		card.setProvider(convertToNacosAgentProvider(agentCard.provider()));
		card.setDocumentationUrl(agentCard.documentationUrl());
		card.setSecuritySchemes(convertToNacosSecuritySchemes(agentCard.securitySchemes()));
		card.setSecurity(agentCard.securityRequirements() == null ? null
				: agentCard.securityRequirements().stream().map(org.a2aproject.sdk.spec.SecurityRequirement::schemes).toList());
		card.setDefaultInputModes(agentCard.defaultInputModes());
		card.setDefaultOutputModes(agentCard.defaultOutputModes());
		card.setSupportsAuthenticatedExtendedCard(agentCard.capabilities().extendedAgentCard());
		return card;
	}

	private static AgentCapabilities convertToNacosAgentCapabilities(org.a2aproject.sdk.spec.AgentCapabilities capabilities) {
		com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities = new com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities();
		nacosCapabilities.setStreaming(capabilities.streaming());
		nacosCapabilities.setPushNotifications(capabilities.pushNotifications());
		return nacosCapabilities;
	}

	private static AgentSkill convertToNacosAgentSkill(org.a2aproject.sdk.spec.AgentSkill agentSkill) {
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
			List<org.a2aproject.sdk.spec.AgentInterface> agentInterfaces) {
		if (agentInterfaces == null) {
			return List.of();
		}
		return agentInterfaces.stream()
			.skip(1)
			.map(AgentCardConverterUtil::convertToNacosAgentInterface)
			.collect(Collectors.toList());
	}

	private static AgentInterface convertToNacosAgentInterface(org.a2aproject.sdk.spec.AgentInterface agentInterface) {
		AgentInterface nacosAgentInterface = new AgentInterface();
		nacosAgentInterface.setUrl(agentInterface.url());
		nacosAgentInterface.setTransport(agentInterface.protocolBinding());
		return nacosAgentInterface;
	}

	private static AgentProvider convertToNacosAgentProvider(org.a2aproject.sdk.spec.AgentProvider agentProvider) {
		if (null == agentProvider) {
			return null;
		}
		AgentProvider nacosAgentProvider = new AgentProvider();
		nacosAgentProvider.setOrganization(agentProvider.organization());
		nacosAgentProvider.setUrl(agentProvider.url());
		return nacosAgentProvider;
	}

	private static Map<String, SecurityScheme> convertToNacosSecuritySchemes(
			Map<String, org.a2aproject.sdk.spec.SecurityScheme> securitySchemes) {
		if (securitySchemes == null) {
			return null;
		}
		Map<String, SecurityScheme> converted = new LinkedHashMap<>();
		securitySchemes.forEach((name, scheme) -> converted.put(name, convertToNacosSecurityScheme(name, scheme)));
		return converted;
	}

	private static SecurityScheme convertToNacosSecurityScheme(String name,
			org.a2aproject.sdk.spec.SecurityScheme securityScheme) {
		try {
			Map<String, Map<String, Object>> serialized = JacksonUtils.toObj(JsonUtil.toJson(securityScheme),
					new TypeReference<>() {
					});
			Map<String, Object> values = new LinkedHashMap<>(serialized.get(securityScheme.type()));
			String legacyType = switch (securityScheme.type()) {
				case "apiKeySecurityScheme" -> "apiKey";
				case "httpAuthSecurityScheme" -> "http";
				case "oauth2SecurityScheme" -> "oauth2";
				case "openIdConnectSecurityScheme" -> "openIdConnect";
				case "mtlsSecurityScheme" -> "mutualTLS";
				default -> throw new IllegalArgumentException("Unsupported security scheme type for " + name);
			};
			if ("apiKey".equals(legacyType) && values.containsKey("location")) {
				values.put("in", values.remove("location"));
			}
			values.put("type", legacyType);
			SecurityScheme converted = new SecurityScheme();
			converted.putAll(values);
			return converted;
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to convert security scheme " + name, e);
		}
	}

}
