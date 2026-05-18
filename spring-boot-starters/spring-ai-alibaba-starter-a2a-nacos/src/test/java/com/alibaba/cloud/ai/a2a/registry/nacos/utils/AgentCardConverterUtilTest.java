/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.registry.nacos.utils;

import java.util.List;

import com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AgentCardConverterUtil}.
 */
class AgentCardConverterUtilTest {

	@Test
	void shouldDefaultMissingSupportsAuthenticatedExtendedCardToFalse() {
		AgentCard agentCard = new AgentCard();
		agentCard.setProtocolVersion("0.3.0");
		agentCard.setName("test-agent");
		agentCard.setDescription("Test agent");
		agentCard.setVersion("1.0.0");
		agentCard.setUrl("http://localhost:8080");
		agentCard.setPreferredTransport("JSONRPC");
		agentCard.setCapabilities(createCapabilities());
		agentCard.setSkills(List.of(createSkill()));
		agentCard.setDefaultInputModes(List.of("text/plain"));
		agentCard.setDefaultOutputModes(List.of("text/plain"));
		agentCard.setSupportsAuthenticatedExtendedCard(null);

		io.a2a.spec.AgentCard converted =
				AgentCardConverterUtil.convertToA2aAgentCard(agentCard);

		assertThat(converted.supportsAuthenticatedExtendedCard()).isFalse();
	}

	private static AgentCapabilities createCapabilities() {
		AgentCapabilities capabilities = new AgentCapabilities();
		capabilities.setStreaming(false);
		capabilities.setPushNotifications(false);
		capabilities.setStateTransitionHistory(false);
		return capabilities;
	}

	private static AgentSkill createSkill() {
		AgentSkill skill = new AgentSkill();
		skill.setId("test-skill");
		skill.setName("Test Skill");
		skill.setDescription("Skill used for converter testing");
		skill.setTags(List.of("test"));
		skill.setExamples(List.of("hello"));
		skill.setInputModes(List.of("text/plain"));
		skill.setOutputModes(List.of("text/plain"));
		return skill;
	}

}
