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
package com.alibaba.cloud.ai.graph.agent.hooks.returndirect;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.util.json.JsonParser;

import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

class ReturnDirectModelHookTest {

	@Test
	void rendersMixedParallelToolResponsesAsValidJson() throws Exception {
		var response = ToolResponseMessage.builder()
			.responses(List.of(
				new ToolResponseMessage.ToolResponse("1", "log", "[INFO] ready"),
				new ToolResponseMessage.ToolResponse("2", "lookup", "{\"id\": 1}"),
				new ToolResponseMessage.ToolResponse("3", "list", "[true]"),
				new ToolResponseMessage.ToolResponse("4", "raw", "{} {}"),
				new ToolResponseMessage.ToolResponse("5", "empty", null),
				new ToolResponseMessage.ToolResponse("6", "plain", "line\n\"quoted\"")))
			.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON))
			.build();

		var action = MessagesModelHook.beforeModelAction(new ReturnDirectModelHook());
		var updates = action.apply(new OverAllState(Map.of("messages", List.of(response))),
				RunnableConfig.builder().build()).join();
		var messages = (ReplaceAllWith<?>) updates.get("messages");
		var assistantMessage = (AssistantMessage) messages.newValues().get(1);
		var mapper = JsonParser.getObjectMapper();

		assertEquals("end", updates.get("jump_to"));
		assertEquals(mapper.readTree("""
				["[INFO] ready", {"id": 1}, [true], "{} {}", null, "line\\n\\"quoted\\""]
				"""), mapper.readTree(assistantMessage.getText()));
	}

}
