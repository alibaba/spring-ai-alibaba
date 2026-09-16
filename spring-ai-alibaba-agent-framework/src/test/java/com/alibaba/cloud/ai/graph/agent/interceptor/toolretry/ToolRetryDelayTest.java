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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolretry;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRetryDelayTest {

	@Test
	void jitterRemainsWithinExistingRange() throws Exception {
		ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder().initialDelay(1000).build();
		Method method = ToolRetryInterceptor.class.getDeclaredMethod("calculateDelay", int.class);
		method.setAccessible(true);
		for (int i = 0; i < 100; i++) {
			assertThat((long) method.invoke(interceptor, 0)).isBetween(750L, 1249L);
		}
	}

	@Test
	void disablingJitterPreservesBackoffAndCap() throws Exception {
		ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder()
			.initialDelay(1000).maxDelay(3000).jitter(false).build();
		Method method = ToolRetryInterceptor.class.getDeclaredMethod("calculateDelay", int.class);
		method.setAccessible(true);
		assertThat((long) method.invoke(interceptor, 0)).isEqualTo(1000);
		assertThat((long) method.invoke(interceptor, 1)).isEqualTo(2000);
		assertThat((long) method.invoke(interceptor, 2)).isEqualTo(3000);
	}

}
