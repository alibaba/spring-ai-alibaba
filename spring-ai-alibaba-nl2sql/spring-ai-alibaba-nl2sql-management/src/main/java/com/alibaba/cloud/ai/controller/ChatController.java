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
 * 聊天控制器
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
	 * 获取智能体的会话列表
	 */
	@GetMapping("/agent/{id}/sessions")
	public ResponseEntity<List<ChatSession>> getAgentSessions(@PathVariable Integer id) {
		List<ChatSession> sessions = chatSessionService.findByAgentId(id);
		return ResponseEntity.ok(sessions);
	}

	/**
	 * 创建新的会话
	 */
	@PostMapping("/agent/{id}/sessions")
	public ResponseEntity<ChatSession> createSession(@PathVariable Integer id,
			@RequestBody(required = false) Map<String, Object> request) {
		String title = request != null ? (String) request.get("title") : null;
		Long userId = request != null ? (Long) request.get("userId") : null;

		ChatSession session = chatSessionService.createSession(id, title, userId);
		return ResponseEntity.ok(session);
	}

	/**
	 * 清空智能体的所有会话
	 */
	@DeleteMapping("/agent/{id}/sessions")
	public ResponseEntity<ApiResponse> clearAgentSessions(@PathVariable Integer id) {
		chatSessionService.clearSessionsByAgentId(id);
		return ResponseEntity.ok(ApiResponse.success("会话已清空"));
	}

	/**
	 * 获取会话的消息列表
	 */
	@GetMapping("/sessions/{sessionId}/messages")
	public ResponseEntity<List<ChatMessage>> getSessionMessages(@PathVariable String sessionId) {
		List<ChatMessage> messages = chatMessageService.findBySessionId(sessionId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * 智能体聊天接口
	 */
	@PostMapping("/agent/{id}/chat")
	public ResponseEntity<ChatResponse> chat(@PathVariable Integer id, @RequestBody ChatRequest request) {
		try {
			// 验证智能体是否存在
			Agent agent = agentService.findById(id.longValue());
			if (agent == null) {
				return ResponseEntity.notFound().build();
			}

			String sessionId = request.getSessionId();
			String userMessage = request.getMessage();

			// 如果没有提供sessionId，创建新会话
			if (sessionId == null || sessionId.trim().isEmpty()) {
				ChatSession newSession = chatSessionService.createSession(id, "新对话", null);
				sessionId = newSession.getId();
			}
			else {
				// 更新会话活动时间
				chatSessionService.updateSessionTime(sessionId);
			}

			// 保存用户消息
			chatMessageService.saveUserMessage(sessionId, userMessage);

			// 调用NL2SQL服务处理用户消息
			ChatResponse response = new ChatResponse(sessionId, "", "text");

			if (nl2SqlService != null) {
				try {
					// 使用NL2SQL服务生成SQL
					String sql = nl2SqlService.apply(userMessage);

					// 创建响应
					response.setMessage("我为您生成了以下SQL查询：");
					response.setMessageType("sql");
					response.setSql(sql);

					// 保存助手消息，包含SQL信息
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("sql", sql);
					metadata.put("originalQuery", userMessage);

					String metadataJson = objectMapper.writeValueAsString(metadata);
					chatMessageService.saveAssistantMessage(sessionId, response.getMessage(), "sql", metadataJson);

				}
				catch (IllegalArgumentException e) {
					// 处理意图不明确或闲聊拒绝的情况
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
	 * 保存消息到会话
	 */
	@PostMapping("/sessions/{sessionId}/messages")
	public ResponseEntity<ChatMessage> saveMessage(@PathVariable String sessionId, @RequestBody ChatMessage message) {
		try {
			// 设置会话ID
			message.setSessionId(sessionId);

			// 保存消息
			ChatMessage savedMessage = chatMessageService.saveMessage(message);

			// 更新会话活动时间
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
	public ResponseEntity<ApiResponse> pinSession(@PathVariable String sessionId,
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
	 * 重命名会话
	 */
	@PutMapping("/sessions/{sessionId}/rename")
	public ResponseEntity<ApiResponse> renameSession(@PathVariable String sessionId,
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
	 * 删除单个会话
	 */
	@DeleteMapping("/sessions/{sessionId}")
	public ResponseEntity<ApiResponse> deleteSession(@PathVariable String sessionId) {
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
