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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.agent.extension.interceptor.AssistantMessageSanitizerInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link DefaultBuilder} registers the {@link AssistantMessageSanitizerInterceptor}
 * by default and honors the {@code assistantMessageSanitizerEnabled} opt-out (issue #4561).
 * <p>
 * The build path is exercised via {@link DefaultBuilder#registerDefaultModelInterceptors()}
 * directly so the test does not require a real {@code ChatModel}. The method operates on the
 * protected {@code modelInterceptors} list shared with the runtime build path.
 */
class DefaultBuilderSanitizerRegistrationTest {

	private static long sanitizerCount(DefaultBuilder builder) {
		if (builder.modelInterceptors == null) {
			return 0;
		}
		return builder.modelInterceptors.stream()
				.filter(i -> i instanceof AssistantMessageSanitizerInterceptor)
				.count();
	}

	@Test
	void registersSanitizerByDefault() {
		DefaultBuilder builder = new DefaultBuilder();

		builder.registerDefaultModelInterceptors();

		assertEquals(1, sanitizerCount(builder), "sanitizer should be registered by default");
		// Registered last so it is innermost (closest to the model call) in the chain.
		ModelInterceptor last = builder.modelInterceptors.get(builder.modelInterceptors.size() - 1);
		assertTrue(last instanceof AssistantMessageSanitizerInterceptor);
	}

	@Test
	void optOutDisablesSanitizer() {
		DefaultBuilder builder = new DefaultBuilder();
		builder.assistantMessageSanitizerEnabled(false);

		builder.registerDefaultModelInterceptors();

		assertEquals(0, sanitizerCount(builder), "sanitizer should not be registered when disabled");
	}

	@Test
	void doesNotRegisterDuplicateSanitizer() {
		DefaultBuilder builder = new DefaultBuilder();
		builder.interceptors(AssistantMessageSanitizerInterceptor.builder().build());
		// Mirror the runtime ordering: user interceptors are separated by type first.
		builder.separateInterceptorsByType();

		builder.registerDefaultModelInterceptors();

		assertEquals(1, sanitizerCount(builder), "an explicitly configured sanitizer must not be duplicated");
	}
}
