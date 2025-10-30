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

package com.alibaba.cloud.ai.agent.controller;

import com.alibaba.cloud.ai.agent.dto.ListSessionsResponse;
import com.alibaba.cloud.ai.agent.dto.Session;
import com.alibaba.cloud.ai.agent.service.SessionService;

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

/** Controller handling session-related API endpoints. */
@RestController
public class SessionController {

	private static final Logger log = LoggerFactory.getLogger(SessionController.class);

	// Session constants
	private static final String EVAL_SESSION_ID_PREFIX = "ADK_EVAL_";

	private final SessionService sessionService;

	@Autowired
	public SessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	/**
	 * Finds a session by its identifiers or throws a ResponseStatusException if not found or if
	 * there's an app/user mismatch.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID.
	 * @return The found Session object.
	 * @throws ResponseStatusException with HttpStatus.NOT_FOUND if the session doesn't exist or
	 *     belongs to a different app/user.
	 */
	private Session findSessionOrThrow(String appName, String userId, String sessionId) {
		Optional<Session> maybeSession =
				sessionService.getSession(appName, userId, sessionId, Optional.empty()).block();

		if (maybeSession == null || !maybeSession.isPresent()) {
			log.warn(
					"Session not found for appName={}, userId={}, sessionId={}", appName, userId, sessionId);
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					String.format(
							"Session not found: appName=%s, userId=%s, sessionId=%s",
							appName, userId, sessionId));
		}

		Session session = maybeSession.get();

		if (!Objects.equals(session.appName(), appName) || !Objects.equals(session.userId(), userId)) {
			log.warn(
					"Session ID {} found but appName/userId mismatch (Expected: {}/{}, Found: {}/{}) -"
							+ " Treating as not found.",
					sessionId,
					appName,
					userId,
					session.appName(),
					session.userId());

			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Session found but belongs to a different app/user.");
		}
		log.debug("Found session: {}", sessionId);
		return session;
	}

	/**
	 * Retrieves a specific session by its ID.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID.
	 * @return The requested Session object.
	 * @throws ResponseStatusException if the session is not found.
	 */
	@GetMapping("/apps/{appName}/users/{userId}/sessions/{sessionId}")
	public Session getSession(
			@PathVariable String appName, @PathVariable String userId, @PathVariable String sessionId) {
		log.info("Request received for GET /apps/{}/users/{}/sessions/{}", appName, userId, sessionId);
		return findSessionOrThrow(appName, userId, sessionId);
	}

	/**
	 * Lists all non-evaluation sessions for a given app and user.
	 *
	 * @param appName The name of the application.
	 * @param userId The ID of the user.
	 * @return A list of sessions, excluding those used for evaluation.
	 */
	@GetMapping("/apps/{appName}/users/{userId}/sessions")
	public List<Session> listSessions(@PathVariable String appName, @PathVariable String userId) {
		log.info("Request received for GET /apps/{}/users/{}/sessions", appName, userId);

		ListSessionsResponse response = sessionService.listSessions(appName, userId).block();

		if (response == null || response.sessions() == null) {
			log.warn(
					"Received null response or null sessions list for listSessions({}, {})", appName, userId);
			return Collections.emptyList();
		}

		List<Session> filteredSessions =
				response.sessions().stream()
						.filter(s -> !s.id().startsWith(EVAL_SESSION_ID_PREFIX))
						.collect(toList());
		log.info(
				"Found {} non-evaluation sessions for app={}, user={}",
				filteredSessions.size(),
				appName,
				userId);
		return filteredSessions;
	}

	/**
	 * Creates a new session with a specific ID provided by the client.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The desired session ID.
	 * @param state Optional initial state for the session.
	 * @return The newly created Session object.
	 * @throws ResponseStatusException if a session with the given ID already exists (BAD_REQUEST) or
	 *     if creation fails (INTERNAL_SERVER_ERROR).
	 */
	@PostMapping("/apps/{appName}/users/{userId}/sessions/{sessionId}")
	public Session createSessionWithId(
			@PathVariable String appName,
			@PathVariable String userId,
			@PathVariable String sessionId,
			@RequestBody(required = false) Map<String, Object> state) {
		log.info(
				"Request received for POST /apps/{}/users/{}/sessions/{} with state: {}",
				appName,
				userId,
				sessionId,
				state);

		try {
			findSessionOrThrow(appName, userId, sessionId);

			log.warn("Attempted to create session with existing ID: {}", sessionId);
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Session already exists: " + sessionId);
		}
		catch (ResponseStatusException e) {

			if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
				throw e;
			}

			log.info("Session {} not found, proceeding with creation.", sessionId);
		}

		Map<String, Object> initialState = (state != null) ? state : Collections.emptyMap();
		try {
			Session createdSession =
					sessionService
							.createSession(appName, userId, new ConcurrentHashMap<>(initialState), sessionId)
							.block();

			if (createdSession == null) {

				log.error(
						"Session creation call completed without error but returned null session for {}",
						sessionId);
				throw new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create session (null result)");
			}
			log.info("Session created successfully with id: {}", createdSession.id());
			return createdSession;
		}
		catch (Exception e) {
			log.error("Error creating session with id {}", sessionId, e);

			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error creating session", e);
		}
	}

	/**
	 * Creates a new session where the ID is generated by the service.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param state Optional initial state for the session.
	 * @return The newly created Session object.
	 * @throws ResponseStatusException if creation fails (INTERNAL_SERVER_ERROR).
	 */
	@PostMapping("/apps/{appName}/users/{userId}/sessions")
	public Session createSession(
			@PathVariable String appName,
			@PathVariable String userId,
			@RequestBody(required = false) Map<String, Object> state) {

		log.info(
				"Request received for POST /apps/{}/users/{}/sessions (service generates ID) with state:"
						+ " {}",
				appName,
				userId,
				state);

		Map<String, Object> initialState = (state != null) ? state : Collections.emptyMap();
		try {

			Session createdSession =
					sessionService
							.createSession(appName, userId, new ConcurrentHashMap<>(initialState), null)
							.block();

			if (createdSession == null) {
				log.error(
						"Session creation call completed without error but returned null session for user {}",
						userId);
				throw new ResponseStatusException(
						HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create session (null result)");
			}
			log.info("Session created successfully with generated id: {}", createdSession.id());
			return createdSession;
		}
		catch (Exception e) {
			log.error("Error creating session for user {}", userId, e);
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error creating session", e);
		}
	}

	/**
	 * Deletes a specific session.
	 *
	 * @param appName The application name.
	 * @param userId The user ID.
	 * @param sessionId The session ID to delete.
	 * @return A ResponseEntity with status NO_CONTENT on success.
	 * @throws ResponseStatusException if deletion fails (INTERNAL_SERVER_ERROR).
	 */
	@DeleteMapping("/apps/{appName}/users/{userId}/sessions/{sessionId}")
	public ResponseEntity<Void> deleteSession(
			@PathVariable String appName, @PathVariable String userId, @PathVariable String sessionId) {
		log.info(
				"Request received for DELETE /apps/{}/users/{}/sessions/{}", appName, userId, sessionId);
		try {

			sessionService.deleteSession(appName, userId, sessionId).block();
			log.info("Session deleted successfully: {}", sessionId);
			return ResponseEntity.noContent().build();
		}
		catch (Exception e) {

			log.error("Error deleting session {}", sessionId, e);

			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting session", e);
		}
	}
}
