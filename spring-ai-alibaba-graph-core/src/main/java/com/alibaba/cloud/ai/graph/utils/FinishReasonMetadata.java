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

package com.alibaba.cloud.ai.graph.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.FinishReasonCategory;

/**
 * Carries model finish reasons on the assistant messages stored in graph state.
 */
public final class FinishReasonMetadata {

	public static final String FINISH_REASON = "finishReason";

	public static final String CATEGORY = "finishReasonCategory";

	private FinishReasonMetadata() {
	}

	public static AssistantMessage apply(AssistantMessage message, ChatGenerationMetadata generation) {
		String rawReason = generation.getFinishReason();
		FinishReasonCategory category = generation.getFinishReasonCategory();
		if (rawReason == null && category == FinishReasonCategory.UNSPECIFIED) {
			return message;
		}

		Map<String, Object> metadata = new HashMap<>(message.getMetadata());
		if (rawReason != null) {
			metadata.put(FINISH_REASON, rawReason);
		}
		metadata.put(CATEGORY, category.name());
		var parts = category == FinishReasonCategory.FILTERED
				? message.getParts().stream().filter(part -> !(part instanceof ToolCallPart)).toList()
				: message.getParts();
		return AssistantMessage.builder().parts(parts).properties(metadata).build();
	}

}
