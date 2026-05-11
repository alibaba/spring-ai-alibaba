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

import java.util.Map;

/**
 * Voice agent streaming events.
 */
public sealed interface VoiceAgentEvent permits VoiceAgentEvent.SttChunkEvent,
		VoiceAgentEvent.SttOutputEvent, VoiceAgentEvent.AgentChunkEvent, VoiceAgentEvent.AgentEndEvent,
		VoiceAgentEvent.ToolCallEvent, VoiceAgentEvent.ToolResultEvent, VoiceAgentEvent.TtsChunkEvent,
		VoiceAgentEvent.TtsEndEvent {

	String type();

	long ts();

	record SttChunkEvent(String type, String transcript, long ts) implements VoiceAgentEvent {
		public static SttChunkEvent create(String transcript) {
			return new SttChunkEvent("stt_chunk", transcript, System.currentTimeMillis());
		}
	}

	record SttOutputEvent(String type, String transcript, long ts) implements VoiceAgentEvent {
		public static SttOutputEvent create(String transcript) {
			return new SttOutputEvent("stt_output", transcript, System.currentTimeMillis());
		}
	}

	record AgentChunkEvent(String type, String text, long ts) implements VoiceAgentEvent {
		public static AgentChunkEvent create(String text) {
			return new AgentChunkEvent("agent_chunk", text, System.currentTimeMillis());
		}
	}

	record AgentEndEvent(String type, long ts) implements VoiceAgentEvent {
		public static AgentEndEvent create() {
			return new AgentEndEvent("agent_end", System.currentTimeMillis());
		}
	}

	record ToolCallEvent(String type, String id, String name, Map<String, Object> args, long ts)
			implements VoiceAgentEvent {
		public static ToolCallEvent create(String id, String name, Map<String, Object> args) {
			return new ToolCallEvent("tool_call", id, name, args != null ? args : Map.of(),
					System.currentTimeMillis());
		}
	}

	record ToolResultEvent(String type, String toolCallId, String name, String result, long ts)
			implements VoiceAgentEvent {
		public static ToolResultEvent create(String toolCallId, String name, String result) {
			return new ToolResultEvent("tool_result", toolCallId, name, result,
					System.currentTimeMillis());
		}
	}

	record TtsChunkEvent(String type, byte[] audio, long ts) implements VoiceAgentEvent {
		public static TtsChunkEvent create(byte[] audio) {
			return new TtsChunkEvent("tts_chunk", audio, System.currentTimeMillis());
		}
	}

	record TtsEndEvent(String type, long ts) implements VoiceAgentEvent {
		public static TtsEndEvent create() {
			return new TtsEndEvent("tts_end", System.currentTimeMillis());
		}
	}
}
