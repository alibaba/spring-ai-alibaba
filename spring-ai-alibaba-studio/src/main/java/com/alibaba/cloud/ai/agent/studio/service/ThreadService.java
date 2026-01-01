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

package com.alibaba.cloud.ai.agent.studio.service;

import com.alibaba.cloud.ai.agent.studio.dto.ListThreadsResponse;
import com.alibaba.cloud.ai.agent.studio.dto.Thread;

import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

/**
 * Service interface for managing user threads.
 */
public interface ThreadService {

	/**
	 * Retrieves a thread by its identifiers.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID.
	 * @param state Optional state filter.
	 * @return A Mono containing an Optional Session.
	 */
	Mono<Optional<Thread>> getThread(
			String appName, String userId, String threadId, Optional<Map<String, Object>> state);

	/**
	 * Lists all threads for a given app and user.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @return A Mono containing the list of threads response.
	 */
	Mono<ListThreadsResponse> listThreads(String appName, String userId);

	/**
	 * Creates a new thread.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param initialState The initial state for the thread.
	 * @param threadId The thread ID (optional, can be null for auto-generation).
	 * @return A Mono containing the created Session.
	 */
	Mono<Thread> createThread(
			String appName, String userId, Map<String, Object> initialState, String threadId);

	/**
	 * Deletes a thread.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID to delete.
	 * @return A Mono that completes when the deletion is done.
	 */
	Mono<Void> deleteThread(String appName, String userId, String threadId);
}

