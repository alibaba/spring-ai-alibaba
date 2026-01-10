/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.agent.nacos;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.utils.ChatOptionsProxy;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.DefaultBuilder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

public class NacosAgentPromptBuilder extends DefaultBuilder {

	private NacosOptions nacosOptions;

	public NacosAgentPromptBuilder nacosOptions(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions; return this;
	}

	protected static Map<String, String> getMetadata(PromptVO prompt) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("promptKey", prompt.getPromptKey());
		metadata.put("promptVersion", prompt.getVersion());
		return metadata;
	}

	protected void registryPrompt(NacosOptions nacosOptions, ReactAgent reactAgent, ObservationMetadataAwareOptions observationMetadataAwareOptions, String promptKey) {

		Listener listener = new AbstractListener() {
			@Override
			public void receiveConfigInfo(String configInfo) {
				PromptVO promptVO = JSON.parseObject(configInfo, PromptVO.class);
				try {
					Map<String, String> metadata = getMetadata(promptVO);
					observationMetadataAwareOptions.getObservationMetadata().putAll(metadata);
					reactAgent.setInstruction(promptVO.getTemplate());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		try {
			nacosOptions.getNacosConfigService()
					.addListener(String.format("prompt-%s.json", promptKey), "nacos-ai-meta", listener);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ReactAgent build() {
		PromptVO promptVO = null;
		ObservationMetadataAwareOptions observationMetadataAwareOptions = null;
		if (nacosOptions.getPromptKey() != null) {
			promptVO = NacosPromptInjector.getPromptByKey(nacosOptions, nacosOptions.getPromptKey());
			if (promptVO != null) {
				this.instruction = promptVO.getTemplate();
				if (this.chatOptions == null) {
					this.chatOptions = new OpenAiChatOptions();
				}
				if (!(this.chatOptions instanceof ObservationMetadataAwareOptions)) {
					this.chatOptions = (ChatOptions) ChatOptionsProxy.createProxy(this.chatOptions, getMetadata(promptVO));
				}

				observationMetadataAwareOptions = (ObservationMetadataAwareOptions) this.chatOptions;
				observationMetadataAwareOptions.getObservationMetadata().putAll(getMetadata(promptVO));
			}
		}
		if (this.observationRegistry == null && nacosOptions.getObservationConfiguration() != null) {
			this.observationRegistry = nacosOptions.getObservationConfiguration().getObservationRegistry();
		}
		if (this.customObservationConvention == null && nacosOptions.getObservationConfiguration() != null) {
			this.customObservationConvention = nacosOptions.getObservationConfiguration()
					.getChatClientObservationConvention();
		}

		ReactAgent reactAgent = super.build();
		if (promptVO != null && observationMetadataAwareOptions != null) {
			registryPrompt(nacosOptions, reactAgent, observationMetadataAwareOptions, promptVO.getPromptKey());
		}
		return reactAgent;
	}
}
