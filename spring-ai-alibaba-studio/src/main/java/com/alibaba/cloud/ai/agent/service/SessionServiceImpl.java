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

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * In-memory implementation of SessionService.
 * For production use, this should be backed by a database or distributed cache.
 */
@Service
public class SessionServiceImpl implements SessionService {

	private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);

	// In-memory storage: key = "appName:userId:sessionId", value = Session
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	// Storage for session states: key = "appName:userId:sessionId", value = state
	private final Map<String, Map<String, Object>> sessionStates = new ConcurrentHashMap<>();

	@Override
	public Mono<Optional<Session>> getSession(
			String appName, String userId, String sessionId, Optional<Map<String, Object>> state) {
		return Mono.fromCallable(() -> {
			String key = buildKey(appName, userId, sessionId);
			Session session = sessions.get(key);

			if (session != null) {
				// Update last access time
				session.lastUpdateTime(Instant.now());
				log.debug("Retrieved session: {}", sessionId);
			}
			else {
				log.debug("Session not found: {}", sessionId);
			}

			return Optional.ofNullable(session);
		});
	}

	@Override
	public Mono<ListSessionsResponse> listSessions(String appName, String userId) {
		return Mono.fromCallable(() -> {
			String prefix = buildKeyPrefix(appName, userId);

			List<Session> userSessions = sessions.entrySet().stream()
					.filter(entry -> entry.getKey().startsWith(prefix))
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());

			log.debug("Found {} sessions for app={}, user={}", userSessions.size(), appName, userId);
			return ListSessionsResponse.of(userSessions);
		});
	}

	@Override
	public Mono<Session> createSession(
			String appName, String userId, Map<String, Object> initialState, String sessionId) {
		return Mono.fromCallable(() -> {
			// Generate session ID if not provided
			String finalSessionId = (sessionId == null || sessionId.trim().isEmpty())
					? generateSessionId()
					: sessionId;

			String key = buildKey(appName, userId, finalSessionId);

			// Check if session already exists
			if (sessions.containsKey(key)) {
				log.warn("Attempted to create duplicate session: {}", finalSessionId);
				throw new IllegalStateException("Session already exists: " + finalSessionId);
			}

			// Create new session
			Session newSession = Session.builder(finalSessionId)
					.appName(appName)
					.userId(userId)
					.lastUpdateTime(Instant.now())
					.build();

			sessions.put(key, newSession);

			// Store initial state if provided
			if (initialState != null && !initialState.isEmpty()) {
				sessionStates.put(key, new ConcurrentHashMap<>(initialState));
			}

			log.info("Created session: {} for app={}, user={}", finalSessionId, appName, userId);
			return newSession;
		});
	}

	@Override
	public Mono<Void> deleteSession(String appName, String userId, String sessionId) {
		return Mono.fromRunnable(() -> {
			String key = buildKey(appName, userId, sessionId);
			Session removed = sessions.remove(key);
			sessionStates.remove(key);

			if (removed != null) {
				log.info("Deleted session: {} for app={}, user={}", sessionId, appName, userId);
			}
			else {
				log.warn("Attempted to delete non-existent session: {}", sessionId);
			}
		});
	}

	/**
	 * Gets the state for a session.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID.
	 * @return The session state, or empty map if not found.
	 */
	public Map<String, Object> getSessionState(String appName, String userId, String sessionId) {
		String key = buildKey(appName, userId, sessionId);
		return sessionStates.getOrDefault(key, new ConcurrentHashMap<>());
	}

	/**
	 * Updates the state for a session.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID.
	 * @param state The new state.
	 */
	public void updateSessionState(
			String appName, String userId, String sessionId, Map<String, Object> state) {
		String key = buildKey(appName, userId, sessionId);
		if (sessions.containsKey(key)) {
			sessionStates.put(key, new ConcurrentHashMap<>(state));
			// Update last update time
			Session session = sessions.get(key);
			if (session != null) {
				session.lastUpdateTime(Instant.now());
			}
			log.debug("Updated state for session: {}", sessionId);
		}
	}

	/**
	 * Builds a storage key for a session.
	 */
	private String buildKey(String appName, String userId, String sessionId) {
		return String.format("%s:%s:%s", appName, userId, sessionId);
	}

	/**
	 * Builds a key prefix for filtering sessions by app and user.
	 */
	private String buildKeyPrefix(String appName, String userId) {
		return String.format("%s:%s:", appName, userId);
	}

	/**
	 * Generates a unique session ID.
	 */
	private String generateSessionId() {
		return UUID.randomUUID().toString();
	}
}

