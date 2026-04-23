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
package com.alibaba.cloud.ai.graph.agent.hooks.hip;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HitlMetadataKeys;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HumanInTheLoopDynamicApprovalTest {

	@Test
	public void testDynamicApprovalByToolName() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", "dynamic_tool", "{}");
		AssistantMessage assistantMessage = AssistantMessage.builder()
				.content("Plan")
				.toolCalls(List.of(toolCall))
				.build();

		OverAllState state = OverAllStateBuilder.builder()
				.withData(Map.of("messages", List.of(assistantMessage)))
				.build();

		RunnableConfig config = RunnableConfig.builder()
				.addMetadata(HitlMetadataKeys.HITL_APPROVAL_TOOL_NAMES_KEY, List.of("dynamic_tool"))
				.build();

		HumanInTheLoopHook hook = HumanInTheLoopHook.builder().build();

		Optional<InterruptionMetadata> interruption = hook.interrupt("HITL", state, config);
		Assertions.assertTrue(interruption.isPresent(), "Expected interruption for dynamic approval tool");
		Assertions.assertEquals(1, interruption.get().toolFeedbacks().size());
		Assertions.assertEquals("dynamic_tool", interruption.get().toolFeedbacks().get(0).getName());
		Assertions.assertEquals("call-1", interruption.get().toolFeedbacks().get(0).getId());
	}
}
