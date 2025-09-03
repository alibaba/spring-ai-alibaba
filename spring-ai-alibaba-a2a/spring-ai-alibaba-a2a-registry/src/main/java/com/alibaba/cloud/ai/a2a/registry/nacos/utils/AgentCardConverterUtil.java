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
import java.util.stream.Collectors;

import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import io.a2a.spec.AgentCapabilities;

/**
 * Agent card converter util.
 */
public class AgentCardConverterUtil {

	public static io.a2a.spec.AgentCard convertToA2aAgentCard(AgentCard agentCard) {
		if (agentCard == null) {
			return null;
		}

		return new io.a2a.spec.AgentCard.Builder().name(agentCard.getName())
			.description(agentCard.getDescription())
			.url(agentCard.getUrl())
			.version(agentCard.getVersion())
			.documentationUrl(agentCard.getDocumentationUrl())
			.capabilities(convertToA2aAgentCapabilities(agentCard.getCapabilities()))
			.defaultInputModes(agentCard.getDefaultInputModes())
			.defaultOutputModes(agentCard.getDefaultOutputModes())
			.skills(convertToA2aAgentSkills(agentCard.getSkills()))
			.protocolVersion(agentCard.getProtocolVersion())
			.build();
	}

	public static AgentCapabilities convertToA2aAgentCapabilities(
			com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities) {
		if (nacosCapabilities == null) {
			return null;
		}

		return new AgentCapabilities.Builder().streaming(nacosCapabilities.getStreaming())
			.pushNotifications(nacosCapabilities.getPushNotifications())
			.stateTransitionHistory(nacosCapabilities.getStateTransitionHistory())
			.build();
	}

	public static List<io.a2a.spec.AgentSkill> convertToA2aAgentSkills(List<AgentSkill> nacosSkills) {
		if (nacosSkills == null) {
			return null;
		}

		return nacosSkills.stream().map(AgentCardConverterUtil::transferAgentSkill).collect(Collectors.toList());
	}

	public static io.a2a.spec.AgentSkill transferAgentSkill(AgentSkill nacosSkill) {
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
		card.setName(agentCard.name());
		card.setDescription(agentCard.description());
		card.setUrl(agentCard.url());
		card.setVersion(agentCard.version());
		card.setDocumentationUrl(agentCard.documentationUrl());
		card.setCapabilities(convertToNacosAgentCapabilities(agentCard.capabilities()));
		card.setDefaultInputModes(agentCard.defaultInputModes());
		card.setDefaultOutputModes(agentCard.defaultOutputModes());
		card.setSkills(agentCard.skills().stream().map(AgentCardConverterUtil::convertToNacosAgentSkill).toList());
		card.setProtocolVersion(agentCard.protocolVersion());

		return card;
	}

	public static com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities convertToNacosAgentCapabilities(
			AgentCapabilities capabilities) {
		com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities = new com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities();
		nacosCapabilities.setStreaming(capabilities.streaming());
		nacosCapabilities.setPushNotifications(capabilities.pushNotifications());
		nacosCapabilities.setStateTransitionHistory(capabilities.stateTransitionHistory());
		return nacosCapabilities;
	}

	public static AgentSkill convertToNacosAgentSkill(io.a2a.spec.AgentSkill agentSkill) {
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

}
