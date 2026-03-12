/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.voiceagent.stream;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * WebSocket handler for streaming voice agent. Receives text input, emits JSON events.
 *
 * <p>Protocol: Client sends {"text":"..."}. Server sends events: agent_chunk, tool_call,
 * tool_result, agent_end, tts_chunk.
 */
@Component
public class VoiceAgentWebSocketHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(VoiceAgentWebSocketHandler.class);

	private final VoiceAgentStreamService streamService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public VoiceAgentWebSocketHandler(VoiceAgentStreamService streamService) {
		this.streamService = streamService;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		log.debug("WebSocket connected: {}", session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String payload = message.getPayload();
		if (payload == null || payload.isBlank()) {
			sendError(session, "Empty message");
			return;
		}
		ParsedInput input = parseInput(payload);
		if (input == null || input.text() == null || input.text().isBlank()) {
			sendError(session, "Missing or empty 'text' field");
			return;
		}
		if (input.ttsOnly()) {
			runTtsStream(session, input.text()).subscribe();
		}
		else {
			runStream(session, input.text()).subscribe();
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		log.debug("WebSocket closed: {} status={}", session.getId(), status);
	}

	private ParsedInput parseInput(String payload) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(payload, Map.class);
			Object t = map.get("text");
			Object ttsOnly = map.get("ttsOnly");
			boolean tts = ttsOnly instanceof Boolean b && b || "true".equals(String.valueOf(ttsOnly));
			return new ParsedInput(t != null ? t.toString().trim() : null, tts);
		}
		catch (Exception e) {
			return null;
		}
	}

	private record ParsedInput(String text, boolean ttsOnly) {
	}

	private Mono<Void> runStream(WebSocketSession session, String text) {
		return Mono.fromCallable(() -> streamService.streamWithText(text))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(flux -> flux.doOnNext(event -> sendEvent(session, event))
						.doOnError(e -> sendError(session, e.getMessage()))
						.then())
				.onErrorResume(e -> {
					sendError(session, e.getMessage());
					return Mono.empty();
				});
	}

	private Mono<Void> runTtsStream(WebSocketSession session, String text) {
		return streamService.streamTts(text)
				.map(VoiceAgentEvent.TtsChunkEvent::create)
				.doOnNext(event -> sendEvent(session, event))
				.doOnError(e -> sendError(session, e.getMessage()))
				.then()
				.onErrorResume(e -> {
					sendError(session, e.getMessage());
					return Mono.empty();
				});
	}

	private void sendEvent(WebSocketSession session, VoiceAgentEvent event) {
		if (!session.isOpen()) {
			return;
		}
		try {
			String json = toJson(event);
			session.sendMessage(new TextMessage(json));
		}
		catch (IOException e) {
			log.warn("Failed to send event: {}", e.getMessage());
		}
	}

	private void sendError(WebSocketSession session, String message) {
		if (!session.isOpen()) {
			return;
		}
		try {
			String json = objectMapper.writeValueAsString(Map.of("type", "error", "message", message));
			session.sendMessage(new TextMessage(json));
		}
		catch (IOException e) {
			log.warn("Failed to send error: {}", e.getMessage());
		}
	}

	private String toJson(VoiceAgentEvent event) throws IOException {
		if (event instanceof VoiceAgentEvent.SttOutputEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "transcript", e.transcript(), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.AgentChunkEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "text", e.text(), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.AgentEndEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.ToolCallEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "id", e.id(), "name", e.name(), "args", e.args(), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.ToolResultEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "toolCallId", e.toolCallId(), "name", e.name(), "result", e.result(), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.TtsChunkEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "audio", Base64.getEncoder().encodeToString(e.audio()), "ts", e.ts()));
		}
		if (event instanceof VoiceAgentEvent.TtsEndEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "ts", e.ts()));
		}
		throw new IllegalArgumentException("Unknown event: " + event);
	}
}
