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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.*;
import com.alibaba.cloud.ai.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat Controller
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	@Autowired
	private AgentService agentService;

	@Autowired
	private ChatSessionService chatSessionService;

	@Autowired
	private ChatMessageService chatMessageService;

	@Autowired(required = false)
	private Nl2SqlService nl2SqlService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Get session list for an agent
	 */
	@GetMapping("/agent/{id}/sessions")
	public ResponseEntity<List<ChatSession>> getAgentSessions(@PathVariable(value = "id") Integer id) {
		List<ChatSession> sessions = chatSessionService.findByAgentId(id);
		return ResponseEntity.ok(sessions);
	}

	/**
	 * Create a new session
	 */
	@PostMapping("/agent/{id}/sessions")
	public ResponseEntity<ChatSession> createSession(@PathVariable(value = "id") Integer id,
			@RequestBody(required = false) Map<String, Object> request) {
		String title = request != null ? (String) request.get("title") : null;
		Long userId = request != null ? (Long) request.get("userId") : null;

		ChatSession session = chatSessionService.createSession(id, title, userId);
		return ResponseEntity.ok(session);
	}

	/**
	 * Clear all sessions for an agent
	 */
	@DeleteMapping("/agent/{id}/sessions")
	public ResponseEntity<ApiResponse> clearAgentSessions(@PathVariable(value = "id") Integer id) {
		chatSessionService.clearSessionsByAgentId(id);
		return ResponseEntity.ok(ApiResponse.success("会话已清空"));
	}

	/**
	 * Get message list for a session
	 */
	@GetMapping("/sessions/{sessionId}/messages")
	public ResponseEntity<List<ChatMessage>> getSessionMessages(@PathVariable(value = "sessionId") String sessionId) {
		List<ChatMessage> messages = chatMessageService.findBySessionId(sessionId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * Agent chat interface
	 */
	@PostMapping("/agent/{id}/chat")
	public ResponseEntity<ChatResponse> chat(@PathVariable(value = "id") Integer id, @RequestBody ChatRequest request) {
		try {
			// Verify that the agent exists
			Agent agent = agentService.findById(id.longValue());
			if (agent == null) {
				return ResponseEntity.notFound().build();
			}

			String sessionId = request.getSessionId();
			String userMessage = request.getMessage();

			// Create a new session if no sessionId is provided
			if (sessionId == null || sessionId.trim().isEmpty()) {
				ChatSession newSession = chatSessionService.createSession(id, "新对话", null);
				sessionId = newSession.getId();
			}
			else {
				// Update session activity time
				chatSessionService.updateSessionTime(sessionId);
			}

			// Save user message
			chatMessageService.saveUserMessage(sessionId, userMessage);

			// Call the NL2SQL service to process the user message
			ChatResponse response = new ChatResponse(sessionId, "", "text");

			if (nl2SqlService != null) {
				try {
					// Use the NL2SQL service to generate SQL
					String sql = nl2SqlService.nl2sql(userMessage);

					// Create a response
					response.setMessage("我为您生成了以下SQL查询：");
					response.setMessageType("sql");
					response.setSql(sql);

					// Save assistant message containing SQL information
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("sql", sql);
					metadata.put("originalQuery", userMessage);

					String metadataJson = objectMapper.writeValueAsString(metadata);
					chatMessageService.saveAssistantMessage(sessionId, response.getMessage(), "sql", metadataJson);

				}
				catch (IllegalArgumentException e) {
					// Handle cases where the intent is unclear or chitchat is refused
					response.setMessage("抱歉，" + e.getMessage());
					response.setMessageType("error");
					response.setError(e.getMessage());

					chatMessageService.saveAssistantMessage(sessionId, response.getMessage(), "error", null);
				}
				catch (Exception e) {
					log.error("NL2SQL processing error for agent {}: {}", id, e.getMessage(), e);
					response.setMessage("抱歉，处理您的请求时出现了错误，请稍后重试。");
					response.setMessageType("error");
					response.setError("系统内部错误");

					chatMessageService.saveAssistantMessage(sessionId, response.getMessage(), "error", null);
				}
			}
			else {
				// NL2SQL服务不可用
				response.setMessage("抱歉，NL2SQL服务当前不可用，请稍后重试。");
				response.setMessageType("error");
				response.setError("服务不可用");

				chatMessageService.saveAssistantMessage(sessionId, response.getMessage(), "error", null);
			}

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Chat processing error for agent {}: {}", id, e.getMessage(), e);
			return ResponseEntity.internalServerError()
				.body(new ChatResponse(request.getSessionId(), "系统错误，请稍后重试", "error"));
		}
	}

	/**
	 * Save message to session
	 */
	@PostMapping("/sessions/{sessionId}/messages")
	public ResponseEntity<ChatMessage> saveMessage(@PathVariable(value = "sessionId") String sessionId,
			@RequestBody ChatMessage message) {
		try {
			// Set session ID
			message.setSessionId(sessionId);

			// Save message
			ChatMessage savedMessage = chatMessageService.saveMessage(message);

			// Update session activity time
			chatSessionService.updateSessionTime(sessionId);

			return ResponseEntity.ok(savedMessage);
		}
		catch (Exception e) {
			log.error("Save message error for session {}: {}", sessionId, e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 置顶/取消置顶会话
	 */
	@PutMapping("/sessions/{sessionId}/pin")
	public ResponseEntity<ApiResponse> pinSession(@PathVariable(value = "sessionId") String sessionId,
			@RequestBody Map<String, Object> request) {
		try {
			Boolean isPinned = (Boolean) request.get("isPinned");
			if (isPinned == null) {
				return ResponseEntity.badRequest().body(ApiResponse.error("isPinned参数不能为空"));
			}

			chatSessionService.pinSession(sessionId, isPinned);
			String message = isPinned ? "会话已置顶" : "会话已取消置顶";
			return ResponseEntity.ok(ApiResponse.success(message));
		}
		catch (Exception e) {
			log.error("Pin session error for session {}: {}", sessionId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiResponse.error("操作失败"));
		}
	}

	/**
	 * Rename session
	 */
	@PutMapping("/sessions/{sessionId}/rename")
	public ResponseEntity<ApiResponse> renameSession(@PathVariable(value = "sessionId") String sessionId,
			@RequestBody Map<String, Object> request) {
		try {
			String newTitle = (String) request.get("title");
			if (newTitle == null || newTitle.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(ApiResponse.error("标题不能为空"));
			}

			chatSessionService.renameSession(sessionId, newTitle.trim());
			return ResponseEntity.ok(ApiResponse.success("会话已重命名"));
		}
		catch (Exception e) {
			log.error("Rename session error for session {}: {}", sessionId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiResponse.error("重命名失败"));
		}
	}

	/**
	 * Delete a single session
	 */
	@DeleteMapping("/sessions/{sessionId}")
	public ResponseEntity<ApiResponse> deleteSession(@PathVariable(value = "sessionId") String sessionId) {
		try {
			chatSessionService.deleteSession(sessionId);
			return ResponseEntity.ok(ApiResponse.success("会话已删除"));
		}
		catch (Exception e) {
			log.error("Delete session error for session {}: {}", sessionId, e.getMessage(), e);
			return ResponseEntity.internalServerError().body(ApiResponse.error("删除失败"));
		}
	}

}
