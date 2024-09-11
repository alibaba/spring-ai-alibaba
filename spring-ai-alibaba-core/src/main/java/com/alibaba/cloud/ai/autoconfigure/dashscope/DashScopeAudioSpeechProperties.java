/*
 * Copyright 2023-2024 the original author or authors.
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

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.speech.AudioSpeechModels;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * TongYi audio speech configuration properties.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeAudioSpeechProperties.CONFIG_PREFIX)
public class DashScopeAudioSpeechProperties extends DashScopeParentProperties {

	/**
	 * Spring Cloud Alibaba AI configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.audio.speech";

	/**
	 * Default TongYi Chat model.
	 */
	public static final String DEFAULT_AUDIO_MODEL_NAME = AudioSpeechModels.SAMBERT_ZHICHU_V1;

	/**
	 * Enable TongYiQWEN ai audio client.
	 */
	private boolean enabled = true;

	@NestedConfigurationProperty
	private DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder()
		.withModel(DEFAULT_AUDIO_MODEL_NAME)
		.build();

	public DashScopeAudioSpeechOptions getOptions() {

		return this.options;
	}

	public void setOptions(DashScopeAudioSpeechOptions options) {

		this.options = options;
	}

	public boolean isEnabled() {

		return this.enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}
