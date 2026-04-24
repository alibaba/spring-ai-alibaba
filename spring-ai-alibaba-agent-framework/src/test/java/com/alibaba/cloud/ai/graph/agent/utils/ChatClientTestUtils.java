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
package com.alibaba.cloud.ai.graph.agent.utils;

import org.springframework.ai.chat.client.ChatClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test utilities for stubbing {@link ChatClient} mock in unit tests.
 * Use when building agents with a mock ChatClient so that the builder's
 * {@code chatClient.mutate().defaultOptions(...).build()} chain does not NPE.
 */
public final class ChatClientTestUtils {

	private ChatClientTestUtils() {
	}

	/**
	 * Stub a mock ChatClient so that {@code mutate().defaultOptions(...).build()}
	 * returns the same client. Call this in {@code @BeforeEach} when the test uses
	 * a mock ChatClient with ReactAgent/DefaultBuilder.
	 * @param chatClient the mock ChatClient to stub (must be a Mockito mock)
	 */
	@SuppressWarnings("unchecked")
	public static void stubChatClientMutateChain(ChatClient chatClient) {
		ChatClient.Builder builderMock = mock(ChatClient.Builder.class);
		when(chatClient.mutate()).thenReturn(builderMock);
		when(builderMock.defaultOptions(any())).thenReturn(builderMock);
		when(builderMock.build()).thenReturn(chatClient);
	}
}
