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

package com.alibaba.cloud.ai.studio.admin.service.client;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression tests for the spring-ai 1.1.2 {@code outputSchema} copy failure: wrapping a
 * provider ChatOptions into the metadata-aware subclass must not throw
 * {@code FatalBeanException} and must preserve scalar model params.
 *
 * @since 1.0.0.3
 */
class ObservationMetadataChatOptionsTest {

    @Test
    void deepSeekOptionsCanBeWrappedWithoutThrowing() {
        DeepSeekChatOptions base = DeepSeekChatOptions.builder()
            .model("deepseek-chat")
            .temperature(0.3)
            .maxTokens(1024)
            .build();

        DeepSeekObservationMetadataChatOptions wrapped =
                DeepSeekObservationMetadataChatOptions.fromDeepSeekOptions(base);

        assertThatCode(() -> wrapped.copy()).doesNotThrowAnyException();
        assertThat(wrapped.getModel()).isEqualTo("deepseek-chat");
        assertThat(wrapped.getTemperature()).isEqualTo(0.3);
        assertThat(wrapped.getMaxTokens()).isEqualTo(1024);
    }

    @Test
    void deepSeekWrapPreservesObservationMetadata() {
        DeepSeekChatOptions base = DeepSeekChatOptions.builder().model("deepseek-chat").build();

        DeepSeekObservationMetadataChatOptions wrapped =
                DeepSeekObservationMetadataChatOptions.fromDeepSeekOptions(base);
        wrapped.setObservationMetadata(Map.of("studioSource", "test"));

        assertThat(wrapped.getObservationMetadata()).containsEntry("studioSource", "test");
    }

    @Test
    void openAiOptionsCanBeWrappedWithoutThrowing() {
        OpenAiChatOptions base = OpenAiChatOptions.builder().model("gpt-4o").temperature(0.5).build();

        OpenAiObservationMetadataChatOptions wrapped =
                OpenAiObservationMetadataChatOptions.fromOpenAiOptions(base);

        assertThatCode(() -> wrapped.copy()).doesNotThrowAnyException();
        assertThat(wrapped.getModel()).isEqualTo("gpt-4o");
    }

    // Note: add an equivalent case for DashScopeObservationMetadataChatOptions once its
    // builder API is confirmed; the same fromXxxOptions -> copySafely path is exercised.

}
