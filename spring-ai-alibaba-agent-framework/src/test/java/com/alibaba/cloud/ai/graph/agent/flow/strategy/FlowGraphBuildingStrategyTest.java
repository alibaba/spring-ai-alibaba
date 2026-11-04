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
package com.alibaba.cloud.ai.graph.agent.flow.strategy;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowGraphBuildingStrategyTest {

	@Test
	void messagesStrategyAllowsRepeatedUserMessages() {
		FlowGraphBuilder.FlowGraphConfig config = FlowGraphBuilder.FlowGraphConfig.builder().name("test");

		Map<String, KeyStrategy> keyStrategies = new SequentialGraphBuildingStrategy()
			.generateKeyStrategyFactory(config)
			.apply();

		KeyStrategy messagesStrategy = keyStrategies.get("messages");
		List<Object> oldMessages = List.of(new UserMessage("continue"), new AssistantMessage("ok"));
		List<Object> newMessages = List.of(new UserMessage("continue"));

		Object merged = messagesStrategy.apply(oldMessages, newMessages);

		assertEquals(3, ((List<?>) merged).size());
	}

}
