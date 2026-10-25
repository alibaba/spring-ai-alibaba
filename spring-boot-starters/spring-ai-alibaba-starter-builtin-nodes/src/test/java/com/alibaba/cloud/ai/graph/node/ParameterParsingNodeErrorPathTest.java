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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Error-path tests that do not require model credentials, unlike
 * {@link ParameterParsingNodeTest} which runs against a live DashScope endpoint.
 */
public class ParameterParsingNodeErrorPathTest {

	private ParameterParsingNode nodeWith(ChatClient chatClient) {
		return ParameterParsingNode.builder()
			.inputText("My name is Kanbe Kotori and I am 20 years old.")
			.chatClient(chatClient)
			.parameters(List.of(ParameterParsingNode.param("name", "String", "The name of the person")))
			.successKey("success")
			.dataKey("data")
			.reasonKey("reason")
			.build();
	}

	@Test
	public void testApplyReturnsFailureWhenExceptionMessageIsNull() throws Exception {
		// A bare NullPointerException carries a null message; the failure result must
		// still be buildable (Map.of rejects null values) instead of throwing.
		ChatClient chatClient = mock(ChatClient.class);
		when(chatClient.prompt()).thenThrow(new NullPointerException());

		ParameterParsingNode node = nodeWith(chatClient);
		Map<String, Object> result = node.apply(new OverAllState());

		assertNotNull(result);
		assertEquals(Boolean.FALSE, result.get("success"));
		assertNotNull(result.get("reason"));
		assertFalse(result.containsKey("data"));
	}

	@Test
	public void testApplyReturnsFailureWhenInputIsEmpty() throws Exception {
		ChatClient chatClient = mock(ChatClient.class);

		ParameterParsingNode node = ParameterParsingNode.builder()
			.inputText("")
			.chatClient(chatClient)
			.parameters(List.of(ParameterParsingNode.param("name", "String", "The name of the person")))
			.successKey("success")
			.dataKey("data")
			.reasonKey("reason")
			.build();

		Map<String, Object> result = node.apply(new OverAllState());

		assertNotNull(result);
		assertEquals(Boolean.FALSE, result.get("success"));
		assertEquals("inputText is empty.", result.get("reason"));
		assertFalse(result.containsKey("data"));
	}

}
