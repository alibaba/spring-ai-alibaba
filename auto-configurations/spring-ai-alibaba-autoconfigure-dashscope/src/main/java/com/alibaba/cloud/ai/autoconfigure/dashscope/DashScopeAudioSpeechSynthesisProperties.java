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

package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author kevinlin09
 */

@ConfigurationProperties(DashScopeAudioSpeechSynthesisProperties.CONFIG_PREFIX)
public class DashScopeAudioSpeechSynthesisProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.audio.synthesis";

	private final String DEFAULT_MODEL = DashScopeAudioSpeechApi.TTSModel.SAM_BERT_ZHICHU_V1.getValue();

	private static final Float SPEED = 1.0f;

	private static final String DEFAULT_VOICE = "longhua";

	private final DashScopeAudioSpeechApi.ResponseFormat DEFAULT_RESPONSE_FORMAT = DashScopeAudioSpeechApi.ResponseFormat.MP3;

	public DashScopeAudioSpeechOptions getOptions() {
		return this.options;
	}

	public void setOptions(DashScopeAudioSpeechOptions options) {
		this.options = options;
	}

	@NestedConfigurationProperty
	private DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder()
		.model(DEFAULT_MODEL)
		.voice(DEFAULT_VOICE)
		.speed(SPEED)
		.responseFormat(DEFAULT_RESPONSE_FORMAT)
		.build();

	public DashScopeAudioSpeechSynthesisProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

}
