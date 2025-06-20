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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;

import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.model.SpringAIModels;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@AutoConfiguration(after = { RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(DashScopeApi.class)
@ConditionalOnDashScopeEnabled
@ConditionalOnProperty(name = SpringAIModelProperties.AUDIO_SPEECH_MODEL, havingValue = SpringAIModels.OPENAI,
		matchIfMissing = true)
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeAudioSpeechSynthesisProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class DashScopeAudioSpeechAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel(RetryTemplate retryTemplate,
			DashScopeConnectionProperties commonProperties, DashScopeAudioSpeechSynthesisProperties speechProperties) {

		var dashScopeSpeechSynthesisApi = dashScopeSpeechSynthesisApi(commonProperties, speechProperties);

		return new DashScopeSpeechSynthesisModel(dashScopeSpeechSynthesisApi, speechProperties.getOptions(),
				retryTemplate);
	}

	private DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi(DashScopeConnectionProperties commonProperties,
			DashScopeAudioSpeechSynthesisProperties speechSynthesisProperties) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, speechSynthesisProperties,
				"audio.synthesis");

		return new DashScopeSpeechSynthesisApi(resolved.apiKey(), resolved.workspaceId());
	}

}
