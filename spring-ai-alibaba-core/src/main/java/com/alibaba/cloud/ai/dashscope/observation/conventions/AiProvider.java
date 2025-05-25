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
package com.alibaba.cloud.ai.dashscope.observation.conventions;

/**
 * Extended collection of systems providing AI functionality. Based on the OpenTelemetry
 * Semantic Conventions for AI Systems.
 *
 * @author Lumian
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * Semantic Conventions</a>.
 * @see org.springframework.ai.observation.conventions.AiProvider
 */
public enum AiProvider {

	// @formatter:off

    // Please, keep the alphabetical sorting.
    DASHSCOPE("dashscope");

    private final String value;

    AiProvider(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    // @formatter:on

}
