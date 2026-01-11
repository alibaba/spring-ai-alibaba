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
import com.alibaba.cloud.ai.agent.studio.service.ThreadService;

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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/** Controller handling thread-related API endpoints. */
@RestController
public class ThreadController {

	private static final Logger log = LoggerFactory.getLogger(ThreadController.class);

	// Thread constants
	private static final String EVAL_SESSION_ID_PREFIX = "SAA_EVAL_";

	private final ThreadService threadService;

	@Autowired
	public ThreadController(ThreadService threadService) {
		this.threadService = threadService;
	}

	/**
	 * Finds a thread by its identifiers or throws a ResponseStatusException if not found or if
	 * there's an app/user mismatch.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID.
	 * @return The found Thread object.
	 * @throws ResponseStatusException with HttpStatus.NOT_FOUND if the thread doesn't exist or
	 *     belongs to a different app/user.
	 */
	private Thread findThreadOrThrow(String appName, String userId, String threadId) {
		Optional<Thread> optionalThread =
				threadService.getThread(appName, userId, threadId, Optional.empty()).block();

		if (optionalThread == null || !optionalThread.isPresent()) {
			log.warn(
					"Thread not found for appName={}, userId={}, threadId={}", appName, userId, threadId);
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					String.format(
							"Thread not found: appName=%s, userId=%s, threadId=%s",
							appName, userId, threadId));
		}

		Thread thread = optionalThread.get();

		if (!Objects.equals(thread.appName(), appName) || !Objects.equals(thread.userId(), userId)) {
			log.warn(
					"Thread ID {} found but appName/userId mismatch (Expected: {}/{}, Found: {}/{}) -"
							+ " Treating as not found.",
					threadId,
					appName,
					userId,
					thread.appName(),
					thread.userId());

			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Thread found but belongs to a different app/user.");
		}
		log.debug("Found thread: {}", threadId);
		return thread;
	}

	/**
	 * Retrieves a specific thread by its ID.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID.
	 * @return The requested Thread object.
	 * @throws ResponseStatusException if the thread is not found.
	 */
	@GetMapping("/apps/{appName}/users/{userId}/threads/{threadId}")
	public Thread getThread(
			@PathVariable String appName, @PathVariable String userId, @PathVariable String threadId) {
		log.info("Request received for GET /apps/{}/users/{}/threads/{}", appName, userId, threadId);
		return findThreadOrThrow(appName, userId, threadId);
	}

	/**
	 * Lists all non-evaluation threads for a given app and user.
	 *
	 * @param appName The name of the application.
	 * @param userId The ID of the user.
	 * @return A list of threads, excluding those used for evaluation.
	 */
	@GetMapping("/apps/{appName}/users/{userId}/threads")
	public List<Thread> listThreads(@PathVariable String appName, @PathVariable String userId) {
		log.info("Request received for GET /apps/{}/users/{}/threads", appName, userId);

		ListThreadsResponse response = threadService.listThreads(appName, userId).block();

		if (response == null || response.threads() == null) {
			log.warn(
					"Received null response or null threads list for listThreads({}, {})", appName, userId);
			return Collections.emptyList();
		}

		List<Thread> filteredThreads =
				response.threads().stream()
						.filter(s -> !s.threadId().startsWith(EVAL_SESSION_ID_PREFIX))
						.collect(toList());
		log.info(
				"Found {} non-evaluation thread for app={}, user={}",
				filteredThreads.size(),
				appName,
				userId);
		return filteredThreads;
	}

	/**
	 * Creates a new thread with a specific ID provided by the client.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The desired thread ID.
	 * @param state Optional initial state for the thread.
	 * @return The newly created Thread object.
	 * @throws ResponseStatusException if a thread with the given ID already exists (BAD_REQUEST) or
	 *     if creation fails (INTERNAL_SERVER_ERROR).
	 */
	@PostMapping("/apps/{appName}/users/{userId}/threads/{threadId}")
	public Thread createThreadWithId(
			@PathVariable String appName,
			@PathVariable String userId,
			@PathVariable String threadId,
			@RequestBody(required = false) Map<String, Object> state) {
		log.info(
				"Request received for POST /apps/{}/users/{}/threads/{} with state: {}",
				appName,
				userId,
				threadId,
				state);

		try {
			findThreadOrThrow(appName, userId, threadId);

			log.warn("Attempted to create thread with existing ID: {}", threadId);
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Thread already exists: " + threadId);
		}
		catch (ResponseStatusException e) {

			if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
				throw e;
			}

			log.info("Thread {} not found, proceeding with creation.", threadId);
		}

		Map<String, Object> initialState = (state != null) ? state : Collections.emptyMap();
		try {
			Thread createdThread =
					threadService
							.createThread(appName, userId, new ConcurrentHashMap<>(initialState), threadId)
							.block();

			if (createdThread == null) {

				log.error(
						"Thread creation call completed without error but returned null thread for {}",
						threadId);
				throw new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create thread (null result)");
			}
			log.info("Thread created successfully with id: {}", createdThread.threadId());
			return createdThread;
		}
		catch (Exception e) {
			log.error("Error creating thread with id {}", threadId, e);

			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error creating thread", e);
		}
	}

	/**
	 * Creates a new thread where the ID is generated by the service.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param state Optional initial state for the thread.
	 * @return The newly created Thread object.
	 * @throws ResponseStatusException if creation fails (INTERNAL_SERVER_ERROR).
	 */
	@PostMapping("/apps/{appName}/users/{userId}/threads")
	public Thread createThread(
			@PathVariable String appName,
			@PathVariable String userId,
			@RequestBody(required = false) Map<String, Object> state) {

		log.info(
				"Request received for POST /apps/{}/users/{}/threads (service generates ID) with state:"
						+ " {}",
				appName,
				userId,
				state);

		Map<String, Object> initialState = (state != null && !state.isEmpty()) ? state : null;
		try {

			Thread createdThread =
					threadService
							.createThread(appName, userId, (initialState != null) ? new ConcurrentHashMap<>(initialState) : null, null)
							.block();

			if (createdThread == null) {
				log.error(
						"Thread creation call completed without error but returned null thread for user {}",
						userId);
				throw new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create thread (null result)");
			}
			log.info("Thread created successfully with generated id: {}", createdThread.threadId());
			return createdThread;
		}
		catch (Exception e) {
			log.error("Error creating thread for user {}", userId, e);
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error creating thread", e);
		}
	}

	/**
	 * Deletes a specific thread.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param threadId The thread ID to delete.
	 * @return A ResponseEntity with status NO_CONTENT on success.
	 * @throws ResponseStatusException if deletion fails (INTERNAL_SERVER_ERROR).
	 */
	@DeleteMapping("/apps/{appName}/users/{userId}/threads/{threadId}")
	public ResponseEntity<Void> deleteThread(
			@PathVariable String appName, @PathVariable String userId, @PathVariable String threadId) {
		log.info(
				"Request received for DELETE /apps/{}/users/{}/threads/{}", appName, userId, threadId);
		try {

			threadService.deleteThread(appName, userId, threadId).block();
			log.info("Thread deleted successfully: {}", threadId);
			return ResponseEntity.noContent().build();
		}
		catch (Exception e) {

			log.error("Error deleting thread {}", threadId, e);

			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting thread", e);
		}
	}
}
