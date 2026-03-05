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

package com.alibaba.cloud.ai.agent.studio.controller;

import com.alibaba.cloud.ai.agent.studio.dto.ListThreadsResponse;
import com.alibaba.cloud.ai.agent.studio.dto.Thread;
import com.alibaba.cloud.ai.agent.studio.loader.GraphLoader;
import com.alibaba.cloud.ai.agent.studio.service.ThreadService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static java.util.stream.Collectors.toList;

/**
 * REST Controller for Graph-specific thread operations. Uses the same ThreadService as Agent
 * threads but with appName = "graph:" + graphName to namespace graph threads independently.
 * Registered only when a {@link GraphLoader} bean exists.
 */
@RestController
public class GraphThreadController {

	private static final Logger log = LoggerFactory.getLogger(GraphThreadController.class);

	private static final String GRAPH_APP_PREFIX = "graph:";

	private static final String EVAL_SESSION_ID_PREFIX = "SAA_EVAL_";

	private final GraphLoader graphLoader;

	private final ThreadService threadService;

	@Autowired
	public GraphThreadController(GraphLoader graphLoader, ThreadService threadService) {
		this.graphLoader = graphLoader;
		this.threadService = threadService;
	}

	private String toAppName(String graphName) {
		return GRAPH_APP_PREFIX + graphName;
	}

	private void validateGraphExists(String graphName) {
		if (graphName == null || graphName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "graphName cannot be null or empty");
		}
		if (!graphLoader.listGraphs().contains(graphName)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Graph not found: " + graphName);
		}
	}

	private Thread findThreadOrThrow(String graphName, String userId, String threadId) {
		String appName = toAppName(graphName);
		Optional<Thread> optionalThread =
				threadService.getThread(appName, userId, threadId, Optional.empty()).block();

		if (optionalThread == null || !optionalThread.isPresent()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					String.format("Thread not found: graphName=%s, userId=%s, threadId=%s",
							graphName, userId, threadId));
		}

		Thread thread = optionalThread.get();
		if (!Objects.equals(thread.appName(), appName) || !Objects.equals(thread.userId(), userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Thread found but belongs to a different graph/user.");
		}
		return thread;
	}

	@GetMapping("/graphs/{graphName}/users/{userId}/threads/{threadId}")
	public Thread getThread(
			@PathVariable String graphName,
			@PathVariable String userId,
			@PathVariable String threadId) {
		validateGraphExists(graphName);
		return findThreadOrThrow(graphName, userId, threadId);
	}

	@GetMapping("/graphs/{graphName}/users/{userId}/threads")
	public List<Thread> listThreads(
			@PathVariable String graphName,
			@PathVariable String userId) {
		validateGraphExists(graphName);
		String appName = toAppName(graphName);
		ListThreadsResponse response = threadService.listThreads(appName, userId).block();

		if (response == null || response.threads() == null) {
			return Collections.emptyList();
		}

		return response.threads().stream()
				.filter(s -> !s.threadId().startsWith(EVAL_SESSION_ID_PREFIX))
				.collect(toList());
	}

	@PostMapping("/graphs/{graphName}/users/{userId}/threads/{threadId}")
	public Thread createThreadWithId(
			@PathVariable String graphName,
			@PathVariable String userId,
			@PathVariable String threadId,
			@RequestBody(required = false) Map<String, Object> state) {
		validateGraphExists(graphName);
		String appName = toAppName(graphName);

		try {
			findThreadOrThrow(graphName, userId, threadId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread already exists: " + threadId);
		}
		catch (ResponseStatusException e) {
			if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
				throw e;
			}
		}

		Map<String, Object> initialState = (state != null) ? state : Collections.emptyMap();
		Thread createdThread = threadService
				.createThread(appName, userId, new ConcurrentHashMap<>(initialState), threadId)
				.block();

		if (createdThread == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create thread");
		}
		return createdThread;
	}

	@PostMapping("/graphs/{graphName}/users/{userId}/threads")
	public Thread createThread(
			@PathVariable String graphName,
			@PathVariable String userId,
			@RequestBody(required = false) Map<String, Object> state) {
		validateGraphExists(graphName);
		String appName = toAppName(graphName);
		Map<String, Object> initialState = (state != null && !state.isEmpty()) ? state : null;

		Thread createdThread = threadService
				.createThread(appName, userId,
						(initialState != null) ? new ConcurrentHashMap<>(initialState) : null,
						null)
				.block();

		if (createdThread == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create thread");
		}
		return createdThread;
	}

	@DeleteMapping("/graphs/{graphName}/users/{userId}/threads/{threadId}")
	public ResponseEntity<Void> deleteThread(
			@PathVariable String graphName,
			@PathVariable String userId,
			@PathVariable String threadId) {
		validateGraphExists(graphName);
		String appName = toAppName(graphName);
		threadService.deleteThread(appName, userId, threadId).block();
		return ResponseEntity.noContent().build();
	}
}
