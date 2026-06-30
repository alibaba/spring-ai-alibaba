/*
 * Copyright 2026-2027 the original author or authors.
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
package com.alibaba.cloud.ai.agent.nacos;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

final class ObservationMetadataChatOptionsSupport {

	private ObservationMetadataChatOptionsSupport() {
	}

	static ChatOptions metadataOnly(Map<String, String> observationMetadata) {
		return ObservationMetadataChatOptions.metadataOnly(sharedObservationMetadata(observationMetadata));
	}

	static ChatOptions withObservationMetadata(ChatOptions chatOptions, Map<String, String> observationMetadata) {
		Map<String, String> sharedMetadata = sharedObservationMetadata(observationMetadata);
		if (chatOptions instanceof ObservationMetadataAwareOptions observationMetadataAwareOptions) {
			observationMetadataAwareOptions.setObservationMetadata(sharedMetadata);
			return chatOptions;
		}
		if (chatOptions instanceof OpenAiChatOptions openAiChatOptions) {
			return ObservationMetadataOpenAiChatOptions.from(openAiChatOptions, sharedMetadata);
		}
		if (chatOptions instanceof DashScopeChatOptions dashScopeChatOptions) {
			return ObservationMetadataDashScopeChatOptions.from(dashScopeChatOptions, sharedMetadata);
		}
		return ObservationMetadataChatOptions.from(chatOptions, sharedMetadata);
	}

	static ObservationMetadataAwareOptions asObservationMetadataAware(ChatOptions chatOptions) {
		if (chatOptions instanceof ObservationMetadataAwareOptions observationMetadataAwareOptions) {
			return observationMetadataAwareOptions;
		}
		throw new IllegalStateException("ChatOptions does not carry Nacos observation metadata.");
	}

	private static Map<String, String> sharedObservationMetadata(Map<String, String> observationMetadata) {
		Map<String, String> sharedMetadata = new HashMap<>();
		if (observationMetadata != null) {
			sharedMetadata.putAll(observationMetadata);
		}
		return sharedMetadata;
	}

}
