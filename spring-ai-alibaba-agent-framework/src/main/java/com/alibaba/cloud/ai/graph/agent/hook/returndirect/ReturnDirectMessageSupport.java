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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.util.json.JsonParser;

import java.util.List;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

/**
 * Shared support for interpreting returnDirect tool responses.
 */
public final class ReturnDirectMessageSupport {

	private ReturnDirectMessageSupport() {
	}

	public static boolean isReturnDirect(ToolResponseMessage toolResponseMessage) {
		return toolResponseMessage != null
				&& FINISH_REASON.equals(toolResponseMessage.getMetadata().get(FINISH_REASON_METADATA_KEY));
	}

	public static AssistantMessage toAssistantMessage(ToolResponseMessage toolResponseMessage) {
		return AssistantMessage.builder()
				.content(toAssistantText(toolResponseMessage))
				.build();
	}

	static String toAssistantText(ToolResponseMessage toolResponseMessage) {
		List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
		if (responses.isEmpty()) {
			return "";
		}
		if (responses.size() == 1) {
			return Objects.requireNonNullElse(responses.get(0).responseData(), "");
		}
		return JsonParser.toJson(responses.stream()
				.map(ToolResponseMessage.ToolResponse::responseData)
				.toList());
	}

}
