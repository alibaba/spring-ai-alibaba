/*
 * Copyright 2024-2025 the original author or authors.
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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ParameterParsingNodeTest {

	private ChatClient chatClient;

	@BeforeEach
	public void setUp() {
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
		chatClient = ChatClient.builder(chatModel).build();
	}

	private OverAllState createState(Map<String, Object> map) {
		OverAllState state = new OverAllState();
		state.updateState(map);
		return state;
	}

	@Test
	public void testSuccess() throws Exception {
		ParameterParsingNode node = ParameterParsingNode.builder()
			.inputText("")
			.inputTextKey("input")
			.chatClient(chatClient)
			.parameters(List.of(ParameterParsingNode.param("name", "String", "The name of the person"),
					ParameterParsingNode.param("age", "Number", "The age of the person")))
			.successKey("success")
			.dataKey("data")
			.reasonKey("reason")
			.instruction("Parse the input text into a JSON object with the following keys: name, age")
			.build();
		OverAllState state = createState(Map.of("input", "My name is Kanbe Kotori and I am 20 years old."));
		Map<String, Object> result = node.apply(state);
		System.out.println(result);
		assertNotNull(result);
		assertEquals(Map.of("success", true, "data", Map.of("name", "Kanbe Kotori", "age", 20), "reason", "success"),
				result);
	}

	@Test
	public void testFail() throws Exception {
		ParameterParsingNode node = ParameterParsingNode.builder()
			.inputText("")
			.inputTextKey("input")
			.chatClient(chatClient)
			.parameters(List.of(ParameterParsingNode.param("name", "String", "The name of the person"),
					ParameterParsingNode.param("age", "Number", "The age of the person")))
			.successKey("success")
			.dataKey("data")
			.reasonKey("reason")
			.instruction("Parse the input text into a JSON object with the following keys: name, age")
			.build();
		OverAllState state = createState(Map.of());
		Map<String, Object> result = node.apply(state);
		System.out.println(result);
		assertNotNull(result);
		assertTrue(result.containsKey("success"));
		assertTrue(result.containsKey("reason"));
		assertFalse(result.containsKey("data"));
		assertFalse((Boolean) result.get("success"));
	}

}
