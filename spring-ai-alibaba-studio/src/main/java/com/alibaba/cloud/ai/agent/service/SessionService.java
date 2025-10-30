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

package com.alibaba.cloud.ai.agent.service;

import com.alibaba.cloud.ai.agent.dto.ListSessionsResponse;
import com.alibaba.cloud.ai.agent.dto.Session;

import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

/**
 * Service interface for managing user sessions.
 */
public interface SessionService {

	/**
	 * Retrieves a session by its identifiers.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID.
	 * @param state Optional state filter.
	 * @return A Mono containing an Optional Session.
	 */
	Mono<Optional<Session>> getSession(
			String appName, String userId, String sessionId, Optional<Map<String, Object>> state);

	/**
	 * Lists all sessions for a given app and user.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @return A Mono containing the list of sessions response.
	 */
	Mono<ListSessionsResponse> listSessions(String appName, String userId);

	/**
	 * Creates a new session.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param initialState The initial state for the session.
	 * @param sessionId The session ID (optional, can be null for auto-generation).
	 * @return A Mono containing the created Session.
	 */
	Mono<Session> createSession(
			String appName, String userId, Map<String, Object> initialState, String sessionId);

	/**
	 * Deletes a session.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID to delete.
	 * @return A Mono that completes when the deletion is done.
	 */
	Mono<Void> deleteSession(String appName, String userId, String sessionId);
}

