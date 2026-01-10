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

import org.springframework.stereotype.Service;

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
 * In-memory implementation of ThreadService.
 * For production use, this should be backed by a database or distributed cache.
 */
@Service
public class ThreadServiceImpl implements ThreadService {

	private static final Logger log = LoggerFactory.getLogger(ThreadServiceImpl.class);

	// In-memory storage: key = "appName:userId:threadId", value = Thread
	private final Map<String, Thread> threads = new ConcurrentHashMap<>();

	// Storage for thread states: key = "appName:userId:threadId", value = state
	private final Map<String, Map<String, Object>> thradStates = new ConcurrentHashMap<>();

	@Override
	public Mono<Optional<Thread>> getThread(
			String appName, String userId, String threadId, Optional<Map<String, Object>> state) {
		return Mono.fromCallable(() -> {
			String key = buildKey(appName, userId, threadId);
			Thread thread = threads.get(key);

			return Optional.ofNullable(thread);
		});
	}

	@Override
	public Mono<ListThreadsResponse> listThreads(String appName, String userId) {
		return Mono.fromCallable(() -> {
			String prefix = buildKeyPrefix(appName, userId);

			List<Thread> userThreads = threads.entrySet().stream()
					.filter(entry -> entry.getKey().startsWith(prefix))
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());

			log.debug("Found {} threads for app={}, user={}", userThreads.size(), appName, userId);
			return ListThreadsResponse.of(userThreads);
		});
	}

	@Override
	public Mono<Thread> createThread(
			String appName, String userId, Map<String, Object> initialState, String threadId) {
		return Mono.fromCallable(() -> {
			// Generate thread ID if not provided
			String finalThreadId = (threadId == null || threadId.trim().isEmpty())
					? generateThreadId()
					: threadId;

			String key = buildKey(appName, userId, finalThreadId);

			// Check if thread already exists
			if (threads.containsKey(key)) {
				log.warn("Attempted to create duplicate thread: {}", finalThreadId);
				throw new IllegalStateException("Thread already exists: " + finalThreadId);
			}

			// Create new thread
			Thread newThread = Thread.builder(finalThreadId)
					.appName(appName)
					.userId(userId)
					.build();

			threads.put(key, newThread);

			// Store initial state if provided
			if (initialState != null && !initialState.isEmpty()) {
				thradStates.put(key, new ConcurrentHashMap<>(initialState));
			}

			log.info("Created thread: {} for app={}, user={}", finalThreadId, appName, userId);
			return newThread;
		});
	}

	@Override
	public Mono<Void> deleteThread(String appName, String userId, String threadId) {
		return Mono.fromRunnable(() -> {
			String key = buildKey(appName, userId, threadId);
			Thread removed = threads.remove(key);
			thradStates.remove(key);

			if (removed != null) {
				log.info("Deleted thread: {} for app={}, user={}", threadId, appName, userId);
			}
			else {
				log.warn("Attempted to delete non-existent thread: {}", threadId);
			}
		});
	}

	/**
	 * Gets the state for a thread.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID.
	 * @return The thread state, or empty map if not found.
	 */
	public Map<String, Object> getThreadState(String appName, String userId, String threadId) {
		String key = buildKey(appName, userId, threadId);
		return thradStates.getOrDefault(key, new ConcurrentHashMap<>());
	}

	/**
	 * Updates the state for a thread.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID.
	 * @param state The new state.
	 */
	public void updateThreadState(
			String appName, String userId, String threadId, Map<String, Object> state) {
		String key = buildKey(appName, userId, threadId);
		if (threads.containsKey(key)) {
			thradStates.put(key, new ConcurrentHashMap<>(state));
			// Update last update time
			Thread thread = threads.get(key);
			log.debug("Updated state for thread: {}", threadId);
		}
	}

	/**
	 * Builds a storage key for a thread.
	 */
	private String buildKey(String appName, String userId, String threadId) {
		return String.format("%s:%s:%s", appName, userId, threadId);
	}

	/**
	 * Builds a key prefix for filtering threads by app and user.
	 */
	private String buildKeyPrefix(String appName, String userId) {
		return String.format("%s:%s:", appName, userId);
	}

	/**
	 * Generates a unique thread ID.
	 */
	private String generateThreadId() {
		return UUID.randomUUID().toString();
	}
}

