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
package com.alibaba.cloud.ai.graph.agent.hook.returndirect;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

class ReturnDirectMessageSupportTest {

	@Test
	void shouldRecognizeReturnDirectMetadata() {
		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "demo", "\"done\"")))
				.metadata(Map.of(ReturnDirectConstants.FINISH_REASON_METADATA_KEY, FINISH_REASON))
				.build();

		assertTrue(ReturnDirectMessageSupport.isReturnDirect(toolResponseMessage));
	}

	@Test
	void shouldIgnoreNonReturnDirectMetadata() {
		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "demo", "\"done\"")))
				.build();

		assertFalse(ReturnDirectMessageSupport.isReturnDirect(toolResponseMessage));
	}

	@Test
	void shouldReturnSingleResponseDataAsAssistantText() {
		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "demo", "\"RAW_RESULT=42\"")))
				.build();

		assertEquals("\"RAW_RESULT=42\"", ReturnDirectMessageSupport.toAssistantMessage(toolResponseMessage).getText());
	}

	@Test
	void shouldSerializeMultipleResponsesWithSharedJsonStrategy() {
		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
				.responses(List.of(
						new ToolResponseMessage.ToolResponse("call-1", "demo-1", "\"alpha\""),
						new ToolResponseMessage.ToolResponse("call-2", "demo-2", "{\"value\":2}"),
						new ToolResponseMessage.ToolResponse("call-3", "demo-3", null)))
				.build();

		assertEquals("[\"\\\"alpha\\\"\",{\"value\":2},null]",
				ReturnDirectMessageSupport.toAssistantMessage(toolResponseMessage).getText());
	}

	@Test
	void shouldReturnEmptyStringWhenNoResponsesExist() {
		ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
				.responses(List.of())
				.build();

		assertEquals("", ReturnDirectMessageSupport.toAssistantMessage(toolResponseMessage).getText());
	}

}
