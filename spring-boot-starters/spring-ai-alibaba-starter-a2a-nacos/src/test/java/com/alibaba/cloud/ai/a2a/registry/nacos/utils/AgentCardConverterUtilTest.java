/*
 * Copyright 2025-2026 the original author or authors.
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

import com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link AgentCardConverterUtil}.
 *
 * Verifies that null Boolean fields (e.g. supportsAuthenticatedExtendedCard) do not
 * cause NullPointerException during conversion.
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

	@Test
	void convertToA2aAgentCard_withNullSupportsAuthenticatedExtendedCard_shouldNotThrow() {
		AgentCard nacosCard = createMinimalAgentCard();
		// supportsAuthenticatedExtendedCard is left null (default)

		assertThatNoException().isThrownBy(() -> {
			io.a2a.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
			assertThat(result).isNotNull();
			assertThat(result.supportsAuthenticatedExtendedCard()).isFalse();
		});
	}

	@Test
	void convertToA2aAgentCard_withTrueSupportsAuthenticatedExtendedCard_shouldReturnTrue() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setSupportsAuthenticatedExtendedCard(true);

		io.a2a.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
		assertThat(result).isNotNull();
		assertThat(result.supportsAuthenticatedExtendedCard()).isTrue();
	}

	@Test
	void convertToA2aAgentCard_withFalseSupportsAuthenticatedExtendedCard_shouldReturnFalse() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setSupportsAuthenticatedExtendedCard(false);

		io.a2a.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
		assertThat(result).isNotNull();
		assertThat(result.supportsAuthenticatedExtendedCard()).isFalse();
	}

	@Test
	void convertToA2aAgentCard_withNullAgentCard_shouldReturnNull() {
		assertThat(AgentCardConverterUtil.convertToA2aAgentCard(null)).isNull();
	}

	@Test
	void convertToNacosAgentCard_withNullSupportsAuthenticatedExtendedCard_shouldNotThrow() {
		io.a2a.spec.AgentCard a2aCard = new io.a2a.spec.AgentCard.Builder().name("test-agent")
			.description("Test Agent")
			.url("http://localhost:8080")
			.version("1.0.0")
			.protocolVersion("0.2")
			.preferredTransport("JSONRPC")
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.capabilities(new io.a2a.spec.AgentCapabilities.Builder().streaming(false).build())
			// supportsAuthenticatedExtendedCard not set — defaults to null in record
			.build();

		assertThatNoException().isThrownBy(() -> {
			AgentCard result = AgentCardConverterUtil.convertToNacosAgentCard(a2aCard);
			assertThat(result).isNotNull();
			assertThat(result.getSupportsAuthenticatedExtendedCard()).isFalse();
		});
	}

	private static AgentCard createMinimalAgentCard() {
		AgentCard card = new AgentCard();
		card.setProtocolVersion("0.3.0");
		card.setName("test-agent");
		card.setDescription("Test Agent");
		card.setVersion("1.0.0");
		card.setUrl("http://localhost:8080");
		card.setPreferredTransport("JSONRPC");
		card.setCapabilities(createCapabilities());
		card.setSkills(List.of(createSkill()));
		card.setDefaultInputModes(List.of("text/plain"));
		card.setDefaultOutputModes(List.of("text/plain"));
		return card;
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
