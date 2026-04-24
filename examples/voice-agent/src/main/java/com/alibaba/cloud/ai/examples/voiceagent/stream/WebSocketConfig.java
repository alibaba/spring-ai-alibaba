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

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final VoiceAgentWebSocketHandler voiceAgentWebSocketHandler;

	private final VoiceAgentAudioWebSocketHandler voiceAgentAudioWebSocketHandler;

	public WebSocketConfig(VoiceAgentWebSocketHandler voiceAgentWebSocketHandler,
			VoiceAgentAudioWebSocketHandler voiceAgentAudioWebSocketHandler) {
		this.voiceAgentWebSocketHandler = voiceAgentWebSocketHandler;
		this.voiceAgentAudioWebSocketHandler = voiceAgentAudioWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(voiceAgentWebSocketHandler, "/voice/ws").setAllowedOrigins("*");
		registry.addHandler(voiceAgentAudioWebSocketHandler, "/voice/ws/audio").setAllowedOrigins("*");
	}
}
