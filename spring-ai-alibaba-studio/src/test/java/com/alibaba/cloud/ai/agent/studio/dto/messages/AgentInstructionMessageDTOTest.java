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
package com.alibaba.cloud.ai.agent.studio.dto.messages;

import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression coverage for the bug where AgentInstructionMessage broke Studio's
 * MessageDTO conversion.
 *
 * Background:
 *   - InstructionAgentHook (added by default to every ReactAgent that has an
 *     instruction) wraps the agent's instruction in an AgentInstructionMessage
 *     and pushes it into the state's messages list before each agent run.
 *   - AgentInstructionMessage extends AbstractMessage directly — it is NOT a
 *     UserMessage / AssistantMessage / ToolResponseMessage.
 *   - Studio's MessageDTOFactory.fromMessage previously ran an instanceof chain
 *     that didn't match AgentInstructionMessage and fell through to a default
 *     branch that threw IllegalArgumentException.
 *   - Symptom: any agent run that flowed through Studio (GraphRunResponse /
 *     AgentRunResponse) crashed the SSE stream with
 *       "Unsupported message type: com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage".
 *
 * Fix: MessageDTOFactory.fromMessage now returns null for AgentInstructionMessage
 * so that the instruction is silently hidden from the rendered transcript.
 */
class AgentInstructionMessageDTOTest {

	@Test
	void agentInstructionMessage_convertsToNullDTO() {
		AgentInstructionMessage msg = AgentInstructionMessage.builder()
				.text("你是一个专业翻译，能够准确翻译多种语言。")
				.build();

		MessageDTO dto = assertDoesNotThrow(
				() -> MessageDTO.MessageDTOFactory.fromMessage(msg),
				"AgentInstructionMessage must not crash MessageDTOFactory.fromMessage"
		);

		assertNull(dto, "AgentInstructionMessage should be hidden from UI (return null)");
	}

}
