/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OverAllStateBuilderTest {

	@Test
	void testBuildWithMinimalConfiguration_shouldApplyDefaultInputKey() {
		// Arrange & Act
		OverAllState state = OverAllStateBuilder.builder().build();

		// Assert
		assertThat(state).isNotNull();
		assertThat(state.data()).isEmpty(); // No data added
		assertThat(state.containStrategy(OverAllState.DEFAULT_INPUT_KEY)).isTrue();
		assertThat(state.value("input", String.class)).isEmpty();
	}

	@Test
	void testBuildWithDataAndStrategy_shouldPreserveValues() {
		// Arrange
		Map<String, Object> data = new HashMap<>();
		data.put("input", "Hello World");
		data.put("count", 1);

		// Act
		OverAllState state = OverAllStateBuilder.builder()
			.withData(data)
			.withKeyStrategy("input", new ReplaceStrategy())
			.setResume(true)
			.build();

		// Assert
		assertThat(state).isNotNull();
		assertThat(state.data()).containsEntry("input", "Hello World").containsEntry("count", 1);
		assertThat(state.containStrategy("input")).isTrue();
		assertThat(state.value("input", String.class)).hasValue("Hello World");
	}

	@Test
	void testBuildWithData_shouldPreserveAssistantMessageThoughtSignatures() {
		// Arrange
		byte[] signature = new byte[] { 1, -2, 3, -4 };
		AssistantMessage assistantMessage = AssistantMessage.builder()
			.content("Call tool")
			.properties(Map.of("thoughtSignatures", List.of(signature)))
			.build();

		// Act
		OverAllState state = OverAllStateBuilder.builder()
			.withData(Map.of("messages", List.of(assistantMessage)))
			.build();

		// Assert
		List<?> messages = (List<?>) state.data().get("messages");
		AssistantMessage copiedMessage = (AssistantMessage) messages.get(0);
		List<?> signatures = (List<?>) copiedMessage.getMetadata().get("thoughtSignatures");
		assertThat(signatures).hasSize(1);
		assertThat(signatures.get(0)).isInstanceOf(byte[].class);
		assertThat((byte[]) signatures.get(0)).containsExactly(signature);
	}

	@Test
	void testBuildWithCustomStrategyMap_shouldApplyAllStrategies() {
		// Arrange
		Map<String, KeyStrategy> strategies = new HashMap<>();
		strategies.put("input", new ReplaceStrategy());
		strategies.put("metadata", (oldVal, newVal) -> newVal + "-merged");

		Map<String, Object> data = Map.of("input", "initial", "metadata", "meta");

		// Act
		OverAllState state = OverAllStateBuilder.builder().withData(data).withKeyStrategies(strategies).build();

		// Apply update
		Map<String, Object> update = Map.of("input", "new input", "metadata", "new meta");
		state.updateState(update);

		// Assert
		assertThat(state.value("input", String.class)).hasValue("new input");
		assertThat(state.value("metadata", String.class)).hasValue("new meta-merged");
	}

	@Test
	void testBuildWithoutExplicitInputStrategy_shouldAutoRegisterReplaceStrategy() {
		// Act
		OverAllState state = OverAllStateBuilder.builder().putData("otherKey", "value").build();

		// Assert
		assertThat(state.containStrategy(OverAllState.DEFAULT_INPUT_KEY)).isTrue();
		assertThat(state.value("otherKey", String.class)).hasValue("value");

		// Test that default REPLACE strategy works at runtime
		Map<String, Object> updates = Map.of("otherKey", "newValue");
		state.updateState(updates);
		assertThat(state.value("otherKey", String.class)).hasValue("newValue");
	}

}
