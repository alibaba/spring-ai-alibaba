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
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * WebSocket handler for realtime bidirectional streaming: client sends audio chunks (binary PCM
 * 16kHz), then {"type":"end"} to signal completion. Server streams audio to ASR, streams
 * recognition results (stt_chunk, stt_output), runs Agent, streams TTS out (tts_chunk).
 */
@Component
public class VoiceAgentAudioWebSocketHandler extends AbstractWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(VoiceAgentAudioWebSocketHandler.class);

	private static final String AUDIO_SINK_KEY = "audioSink";

	private final RealtimeVoiceAgentStreamService realtimeStreamService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public VoiceAgentAudioWebSocketHandler(RealtimeVoiceAgentStreamService realtimeStreamService) {
		this.realtimeStreamService = realtimeStreamService;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		Sinks.Many<ByteBuffer> sink = Sinks.many().unicast().onBackpressureBuffer();
		session.getAttributes().put(AUDIO_SINK_KEY, sink);
		log.debug("WebSocket (audio) connected: {}", session.getId());
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
		Sinks.Many<ByteBuffer> sink = getAudioSink(session);
		if (sink != null) {
			ByteBuffer payload = message.getPayload();
			byte[] bytes = new byte[payload.remaining()];
			payload.get(bytes);
			sink.tryEmitNext(ByteBuffer.wrap(bytes));
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String payload = message.getPayload();
		if (payload == null || payload.isBlank()) {
			sendError(session, "Empty message");
			return;
		}
		String type = parseType(payload);
		if ("end".equals(type)) {
			Sinks.Many<ByteBuffer> sink = getAudioSink(session);
			if (sink != null) {
				session.getAttributes().remove(AUDIO_SINK_KEY);
				sink.tryEmitComplete();
				Flux<ByteBuffer> audioFlux = sink.asFlux();
				runRealtimeStream(session, audioFlux).doFinally(signalType -> {
					// New sink for next turn on same connection
					if (session.isOpen()) {
						Sinks.Many<ByteBuffer> nextSink = Sinks.many().unicast().onBackpressureBuffer();
						session.getAttributes().put(AUDIO_SINK_KEY, nextSink);
					}
				}).subscribe();
			}
			else {
				sendError(session, "No audio sink");
			}
		}
		else {
			sendError(session, "Expected {\"type\":\"end\"} to process audio");
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		session.getAttributes().remove(AUDIO_SINK_KEY);
		log.debug("WebSocket (audio) closed: {} status={}", session.getId(), status);
	}

	@SuppressWarnings("unchecked")
	private String parseType(String payload) {
		try {
			Map<String, Object> map = objectMapper.readValue(payload, Map.class);
			Object t = map.get("type");
			return t != null ? t.toString().trim() : null;
		}
		catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Sinks.Many<ByteBuffer> getAudioSink(WebSocketSession session) {
		return (Sinks.Many<ByteBuffer>) session.getAttributes().get(AUDIO_SINK_KEY);
	}

	private Mono<Void> runRealtimeStream(WebSocketSession session, Flux<ByteBuffer> audioFlux) {
		return Mono.fromCallable(() -> realtimeStreamService.streamWithAudioRealtime(audioFlux))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(flux -> flux.doOnNext(event -> sendEvent(session, event))
						.doOnError(e -> sendError(session, e.getMessage()))
						.then())
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
		if (event instanceof VoiceAgentEvent.SttChunkEvent e) {
			return objectMapper.writeValueAsString(Map.of("type", e.type(), "transcript", e.transcript(), "ts", e.ts()));
		}
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
