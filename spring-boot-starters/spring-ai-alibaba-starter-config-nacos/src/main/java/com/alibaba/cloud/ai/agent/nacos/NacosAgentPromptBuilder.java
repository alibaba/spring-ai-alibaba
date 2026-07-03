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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.DefaultBuilder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

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
				this.chatOptions = buildObservationMetadataOptions(getMetadata(promptVO));
				observationMetadataAwareOptions = ObservationMetadataChatOptionsSupport
						.asObservationMetadataAware(this.chatOptions);
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

	private ChatOptions buildObservationMetadataOptions(Map<String, String> metadata) {
		ChatOptions sourceOptions = resolveSourceOptions();
		if (sourceOptions == null) {
			return ObservationMetadataChatOptionsSupport.metadataOnly(metadata);
		}
		return ObservationMetadataChatOptionsSupport.withObservationMetadata(sourceOptions, metadata);
	}

	private ChatOptions resolveSourceOptions() {
		if (this.chatOptions != null) {
			return this.chatOptions;
		}
		if (this.model != null) {
			return this.model.getOptions();
		}
		if (this.chatClient != null) {
			return getChatClientDefaultOptions(this.chatClient);
		}
		return null;
	}

	private static ChatOptions getChatClientDefaultOptions(ChatClient chatClient) {
		try {
			Object defaultChatClientRequest = getDefaultChatClientRequest(chatClient);
			if (defaultChatClientRequest == null) {
				return null;
			}
			ChatModel chatModel = getField(defaultChatClientRequest, "chatModel", ChatModel.class);
			ChatOptions modelOptions = chatModel != null ? chatModel.getOptions() : null;
			ChatOptions.Builder<?> optionsCustomizer = getField(defaultChatClientRequest, "optionsCustomizer",
					ChatOptions.Builder.class);
			if (optionsCustomizer == null) {
				return modelOptions;
			}
			if (modelOptions == null) {
				return optionsCustomizer.clone().build();
			}
			return modelOptions.mutate().combineWith(optionsCustomizer.clone()).build();
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	private static Object getDefaultChatClientRequest(ChatClient chatClient)
			throws NoSuchFieldException, IllegalAccessException {
		Field defaultRequestField = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
		defaultRequestField.setAccessible(true);
		return defaultRequestField.get(chatClient);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getField(Object target, String fieldName, Class<?> fieldType)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Object value = field.get(target);
		if (value == null || fieldType.isInstance(value)) {
			return (T) value;
		}
		return null;
	}
}
