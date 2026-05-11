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
package com.alibaba.cloud.ai.examples.voiceagent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionOptions;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for realtime bidirectional streaming (ASR + TTS).
 *
 * <p>Creates {@link DashScopeAudioTranscriptionModel} when spring-ai-alibaba-dashscope is on the
 * classpath and DASHSCOPE_API_KEY is set.
 */
@Configuration
@ConditionalOnClass(DashScopeAudioTranscriptionModel.class)
public class RealtimeVoiceConfig {

	@Bean
	@ConditionalOnMissingBean(DashScopeAudioTranscriptionModel.class)
	@ConditionalOnProperty(name = "spring.ai.dashscope.api-key")
	public DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel(
			@Value("${spring.ai.dashscope.api-key:}") String apiKey) {
		DashScopeAudioTranscriptionApi api = DashScopeAudioTranscriptionApi.builder()
				.apiKey(new SimpleApiKey(apiKey))
				.build();
		DashScopeAudioTranscriptionOptions defaultOptions = DashScopeAudioTranscriptionOptions.builder()
				.build();
		return DashScopeAudioTranscriptionModel.builder()
				.audioTranscriptionApi(api)
				.defaultOptions(defaultOptions)
				.build();
	}
}
