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

import java.util.Map;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import org.junit.jupiter.api.Test;

import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ObservationMetadataOpenAiChatOptionsTest {

	@Test
	void mutateBuildPreservesObservationMetadata() {
		OpenAiChatOptions options = ObservationMetadataOpenAiChatOptions.from(OpenAiChatOptions.builder()
				.model("configured-model")
				.temperature(0.3)
				.build(), Map.of("promptKey", "prompt-a", "promptVersion", "v1"));

		OpenAiChatOptions mutatedOptions = options.mutate()
				.temperature(0.7)
				.build();

		ObservationMetadataAwareOptions observationOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				mutatedOptions);
		assertEquals("configured-model", mutatedOptions.getModel());
		assertEquals(0.7, mutatedOptions.getTemperature());
		assertEquals(Map.of("promptKey", "prompt-a", "promptVersion", "v1"),
				observationOptions.getObservationMetadata());
	}

	@Test
	void clonedBuilderSeesRefreshedObservationMetadata() {
		OpenAiChatOptions options = ObservationMetadataOpenAiChatOptions.from(OpenAiChatOptions.builder()
				.model("configured-model")
				.build(), Map.of("promptKey", "prompt-a", "promptVersion", "v1"));
		ObservationMetadataAwareOptions holderOptions = assertInstanceOf(ObservationMetadataAwareOptions.class,
				options);
		OpenAiChatOptions.Builder requestBuilder = options.mutate();

		holderOptions.getObservationMetadata().put("promptVersion", "v2");
		OpenAiChatOptions requestOptions = requestBuilder.build();

		ObservationMetadataAwareOptions requestObservationOptions = assertInstanceOf(
				ObservationMetadataAwareOptions.class, requestOptions);
		assertEquals(Map.of("promptKey", "prompt-a", "promptVersion", "v2"),
				requestObservationOptions.getObservationMetadata());
	}

}
