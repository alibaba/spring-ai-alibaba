/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.core.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultA2aServerExecutorProviderTest {

	@Test
	void defaultProviderOwnsAndClosesDistinctExecutors() {
		DefaultA2aServerExecutorProvider provider = new DefaultA2aServerExecutorProvider();

		assertThat(provider.getEventConsumerExecutor()).isNotSameAs(provider.getA2aServerExecutor());

		provider.close();

		assertThat(provider.getA2aServerExecutor().isShutdown()).isTrue();
		assertThat(provider.getEventConsumerExecutor().isShutdown()).isTrue();
	}

	@Test
	void customProvidersRemainSourceCompatibleThroughDefaultAccessor() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			A2aServerExecutorProvider provider = () -> executor;

			assertThat(provider.getEventConsumerExecutor()).isSameAs(executor);
		}
		finally {
			executor.shutdownNow();
		}
	}

}
