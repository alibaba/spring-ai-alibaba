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
import java.util.Map;

import com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import com.alibaba.nacos.api.ai.model.a2a.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

		org.a2aproject.sdk.spec.AgentCard converted =
				AgentCardConverterUtil.convertToA2aAgentCard(agentCard);

		assertThat(converted.capabilities().extendedAgentCard()).isFalse();
	}

	@Test
	void convertToA2aAgentCard_withNullSupportsAuthenticatedExtendedCard_shouldNotThrow() {
		AgentCard nacosCard = createMinimalAgentCard();
		// supportsAuthenticatedExtendedCard is left null (default)

		assertThatNoException().isThrownBy(() -> {
			org.a2aproject.sdk.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
			assertThat(result).isNotNull();
			assertThat(result.capabilities().extendedAgentCard()).isFalse();
		});
	}

	@Test
	void convertToA2aAgentCard_withTrueSupportsAuthenticatedExtendedCard_shouldReturnTrue() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setSupportsAuthenticatedExtendedCard(true);

		org.a2aproject.sdk.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
		assertThat(result).isNotNull();
		assertThat(result.capabilities().extendedAgentCard()).isTrue();
	}

	@Test
	void convertToA2aAgentCard_withFalseSupportsAuthenticatedExtendedCard_shouldReturnFalse() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setSupportsAuthenticatedExtendedCard(false);

		org.a2aproject.sdk.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);
		assertThat(result).isNotNull();
		assertThat(result.capabilities().extendedAgentCard()).isFalse();
	}

	@Test
	void convertToA2aAgentCard_defaultsMissingProtocolVersionToLegacy03() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setProtocolVersion(null);

		org.a2aproject.sdk.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);

		assertThat(result.supportedInterfaces()).singleElement().satisfies(agentInterface ->
			assertThat(agentInterface.protocolVersion()).isEqualTo("0.3"));
	}

	@Test
	void convertToA2aAgentCard_preservesLegacy02ProtocolVersion() {
		AgentCard nacosCard = createMinimalAgentCard();
		nacosCard.setProtocolVersion("0.2.5");

		org.a2aproject.sdk.spec.AgentCard result = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);

		assertThat(result.supportedInterfaces()).singleElement().satisfies(agentInterface ->
			assertThat(agentInterface.protocolVersion()).isEqualTo("0.2.5"));
	}

	@Test
	void convertToA2aAgentCard_withNullAgentCard_shouldReturnNull() {
		assertThat(AgentCardConverterUtil.convertToA2aAgentCard(null)).isNull();
	}

	@Test
	void convertToNacosAgentCard_withNullSupportsAuthenticatedExtendedCard_shouldNotThrow() {
		org.a2aproject.sdk.spec.AgentCard a2aCard = org.a2aproject.sdk.spec.AgentCard.builder()
			.name("test-agent")
			.description("Test Agent")
			.url("http://localhost:8080")
			.version("1.0.0")
			.preferredTransport("JSONRPC")
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.capabilities(org.a2aproject.sdk.spec.AgentCapabilities.builder().streaming(false).build())
			.supportedInterfaces(List.of(new org.a2aproject.sdk.spec.AgentInterface("JSONRPC",
					"http://localhost:8080"),
					new org.a2aproject.sdk.spec.AgentInterface("GRPC", "http://localhost:8081")))
			// supportsAuthenticatedExtendedCard not set — defaults to null in record
			.build();

		assertThatNoException().isThrownBy(() -> {
			AgentCard result = AgentCardConverterUtil.convertToNacosAgentCard(a2aCard);
			assertThat(result).isNotNull();
			assertThat(result.getSupportsAuthenticatedExtendedCard()).isFalse();
			assertThat(result.getUrl()).isEqualTo("http://localhost:8080");
			assertThat(result.getAdditionalInterfaces()).singleElement().satisfies(agentInterface -> {
				assertThat(agentInterface.getTransport()).isEqualTo("GRPC");
				assertThat(agentInterface.getUrl()).isEqualTo("http://localhost:8081");
			});
		});
	}

	@Test
	void shouldRoundTripLegacyApiKeySecurityScheme() {
		AgentCard nacosCard = createMinimalAgentCard();
		SecurityScheme apiKey = new SecurityScheme();
		apiKey.put("type", "apiKey");
		apiKey.put("in", "header");
		apiKey.put("name", "X-API-Key");
		nacosCard.setSecuritySchemes(Map.of("apiKey", apiKey));

		org.a2aproject.sdk.spec.AgentCard a2aCard = AgentCardConverterUtil.convertToA2aAgentCard(nacosCard);

		assertThat(a2aCard.securitySchemes().get("apiKey"))
			.isInstanceOf(org.a2aproject.sdk.spec.APIKeySecurityScheme.class);
		AgentCard roundTripped = AgentCardConverterUtil.convertToNacosAgentCard(a2aCard);
		assertThat(roundTripped.getSecuritySchemes().get("apiKey"))
			.containsEntry("type", "apiKey")
			.containsEntry("in", "header")
			.containsEntry("name", "X-API-Key");
	}

	@Test
	void convertToNacosAgentCard_rejectsTenantThatNacosCannotRepresent() {
		org.a2aproject.sdk.spec.AgentCard card = a2aCardWithInterfaces(List.of(
				new org.a2aproject.sdk.spec.AgentInterface("JSONRPC", "http://localhost:8080", "tenant-a", "1.0")));

		assertThatThrownBy(() -> AgentCardConverterUtil.convertToNacosAgentCard(card))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("index 0")
			.hasMessageContaining("tenant-a");
	}

	@Test
	void convertToNacosAgentCard_rejectsMixedProtocolVersions() {
		org.a2aproject.sdk.spec.AgentCard card = a2aCardWithInterfaces(List.of(
				new org.a2aproject.sdk.spec.AgentInterface("JSONRPC", "http://localhost:8080", null, "1.0"),
				new org.a2aproject.sdk.spec.AgentInterface("JSONRPC", "http://localhost:8081", null, "0.3")));

		assertThatThrownBy(() -> AgentCardConverterUtil.convertToNacosAgentCard(card))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("index 1")
			.hasMessageContaining("0.3")
			.hasMessageContaining("1.0");
	}

	@Test
	void convertToNacosAgentCard_preservesLegacyTopLevelAndAdditionalInterfaces() {
		org.a2aproject.sdk.spec.AgentCard legacyCard = org.a2aproject.sdk.spec.AgentCard.builder()
			.name("legacy-agent")
			.description("Legacy Agent")
			.version("0.3.0")
			.url("http://localhost:8080")
			.preferredTransport("JSONRPC")
			.additionalInterfaces(List.of(
					new org.a2aproject.sdk.spec.Legacy_0_3_AgentInterface("GRPC", "http://localhost:8081")))
			.supportedInterfaces(List.of())
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.capabilities(org.a2aproject.sdk.spec.AgentCapabilities.builder().streaming(false).build())
			.build();

		AgentCard converted = AgentCardConverterUtil.convertToNacosAgentCard(legacyCard);

		assertThat(converted.getProtocolVersion()).isEqualTo("0.3");
		assertThat(converted.getUrl()).isEqualTo("http://localhost:8080");
		assertThat(converted.getPreferredTransport()).isEqualTo("JSONRPC");
		assertThat(converted.getAdditionalInterfaces()).singleElement().satisfies(agentInterface -> {
			assertThat(agentInterface.getTransport()).isEqualTo("GRPC");
			assertThat(agentInterface.getUrl()).isEqualTo("http://localhost:8081");
		});
		assertThat(AgentCardConverterUtil.convertToA2aAgentCard(converted).supportedInterfaces())
			.hasSize(2)
			.allSatisfy(agentInterface -> assertThat(agentInterface.protocolVersion()).isEqualTo("0.3"));
	}

	@Test
	void convertToNacosAgentCard_rejectsSignaturesThatWouldBeLost() {
		org.a2aproject.sdk.spec.AgentCard card = org.a2aproject.sdk.spec.AgentCard
			.builder(a2aCardWithInterfaces(List.of(
					new org.a2aproject.sdk.spec.AgentInterface("JSONRPC", "http://localhost:8080", null, "1.0"))))
			.signatures(List.of(org.a2aproject.sdk.spec.AgentCardSignature.builder()
				.signature("signed")
				.protectedHeader("header")
				.build()))
			.build();

		assertThatThrownBy(() -> AgentCardConverterUtil.convertToNacosAgentCard(card))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("signatures");
	}

	@Test
	void convertToNacosAgentCard_rejectsCapabilityExtensionsThatWouldBeLost() {
		org.a2aproject.sdk.spec.AgentCard base = a2aCardWithInterfaces(List.of(
				new org.a2aproject.sdk.spec.AgentInterface("JSONRPC", "http://localhost:8080", null, "1.0")));
		org.a2aproject.sdk.spec.AgentCard card = org.a2aproject.sdk.spec.AgentCard.builder(base)
			.capabilities(org.a2aproject.sdk.spec.AgentCapabilities.builder()
				.extensions(List.of(org.a2aproject.sdk.spec.AgentExtension.builder()
					.uri("https://example.test/extension")
					.required(true)
					.build()))
				.build())
			.build();

		assertThatThrownBy(() -> AgentCardConverterUtil.convertToNacosAgentCard(card))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("extensions");
	}

	private static org.a2aproject.sdk.spec.AgentCard a2aCardWithInterfaces(
			List<org.a2aproject.sdk.spec.AgentInterface> interfaces) {
		return org.a2aproject.sdk.spec.AgentCard.builder()
			.name("test-agent")
			.description("Test Agent")
			.version("1.0.0")
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.capabilities(org.a2aproject.sdk.spec.AgentCapabilities.builder().streaming(false).build())
			.supportedInterfaces(interfaces)
			.build();
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
