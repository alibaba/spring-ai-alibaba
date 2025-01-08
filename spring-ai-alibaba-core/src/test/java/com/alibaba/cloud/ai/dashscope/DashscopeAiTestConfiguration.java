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
package com.alibaba.cloud.ai.dashscope;

import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DASHSCOPE_API_KEY;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

@SpringBootConfiguration
public class DashscopeAiTestConfiguration {

	@Bean
	public DashScopeImageApi dashscopeImageApi() {
		return newDashScopeImageApi(getApiKey());
	}

	@Bean
	public DashScopeApi dashscopeApi() {
		return newDashScopeApi(getApiKey());
	}

	@Bean
	public DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi() {
		return newDashScopeSpeechSynthesisApi(getApiKey());
	}

	@Bean
	public DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi() {
		return newDashScopeAudioTranscriptionApi(getApiKey());
	}

	@Bean
	public DashScopeApi dashscopeChatApi() {
		return newDashScopeChatApi(getApiKey());
	}

	private DashScopeApi newDashScopeChatApi(String apiKey) {
		return new DashScopeApi(DEFAULT_BASE_URL, apiKey, "");
	}

	private DashScopeApi newDashScopeApi(String apiKey) {
		return new DashScopeApi(apiKey);
	}

	private DashScopeSpeechSynthesisApi newDashScopeSpeechSynthesisApi(String apiKey) {
		return new DashScopeSpeechSynthesisApi(apiKey);
	}

	private DashScopeAudioTranscriptionApi newDashScopeAudioTranscriptionApi(String apiKey) {
		return new DashScopeAudioTranscriptionApi(apiKey);
	}

	private DashScopeImageApi newDashScopeImageApi(String apiKey) {
		return new DashScopeImageApi(apiKey);
	}

	private String getApiKey() {
		String apiKey = System.getenv(DASHSCOPE_API_KEY);
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key.  Put it in an environment variable under the name DASHSCOPE_API_KEY");
		}
		return apiKey;
	}

	@Bean
	public ChatModel dashscopeChatModel(DashScopeApi dashscopeChatApi, TestObservationRegistry observationRegistry) {
		return new DashScopeChatModel(dashscopeChatApi,
				DashScopeChatOptions.builder().withModel(DashScopeApi.DEFAULT_CHAT_MODEL).build(), null, List.of(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE, observationRegistry);
	}

	@Bean
	public EmbeddingModel dashscopeEmbeddingModel(DashScopeApi dashscopeApi) {
		return new DashScopeEmbeddingModel(dashscopeApi);
	}

	@Bean
	public DashScopeImageModel dashscopeImageModel(DashScopeImageApi dashscopeImageApi) {
		return new DashScopeImageModel(dashscopeImageApi);
	}

	@Bean
	public DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel(
			DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi) {
		return new DashScopeSpeechSynthesisModel(dashScopeSpeechSynthesisApi,
				DashScopeSpeechSynthesisOptions.builder().withModel("cosyvoice-v1").withVoice("longhua").build());
	}

	@Bean
	public DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel(
			DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi) {
		return new DashScopeAudioTranscriptionModel(dashScopeAudioTranscriptionApi);
	}

	@Bean
	public TestObservationRegistry observationRegistry() {
		return TestObservationRegistry.create();
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public SpeechSynthesizer speechSynthesizer() {
		return new SpeechSynthesizer();
	}

	@Bean
	@ConditionalOnMissingBean
	public Transcription transcription() {
		return new Transcription();
	}

	@Bean
	@ConditionalOnMissingBean
	public RerankModel dashscopeRerankModel(DashScopeApi dashscopeApi) {
		return new DashScopeRerankModel(dashscopeApi);
	}

}
