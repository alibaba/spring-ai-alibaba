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

package com.alibaba.cloud.ai.agent.nacos;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.common.utils.StringUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.util.ReflectionUtils;

public class NacosPromptInjector {

	static Map<String, Listener> promptKeyListener = new HashMap<>();


	/**
	 * load prompt by agent id.
	 *
	 * @param nacosConfigService
	 * @param AgentVO
	 * @return
	 */
	public static PromptVO loadPromptByAgentId(NacosConfigService nacosConfigService, AgentVO agentVO) {
		try {
			return getPromptByKey(nacosConfigService, agentVO.getPromptKey());
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * load promot by prompt key.
	 *
	 * @param nacosConfigService
	 * @param promptKey
	 * @return
	 * @throws NacosException
	 */
	public static PromptVO getPromptByKey(NacosConfigService nacosConfigService, String promptKey)
			throws NacosException {
		String promptConfig = nacosConfigService.getConfig(String.format("prompt-%s.json", promptKey), "nacos-ai-meta",
				3000L);
		PromptVO promptVO = JSON.parseObject(promptConfig, PromptVO.class);
		promptVO.setPromptKey(promptKey);
		return promptVO;
	}

	/**
	 * replace prompt info by key and registry.
	 *
	 * @param nacosConfigService
	 * @param chatClient
	 * @param promptKey
	 * @throws Exception
	 */
	public static void injectPromptByKey(NacosConfigService nacosConfigService, ChatClient chatClient, String promptKey)
			throws Exception {
		PromptVO promptVO = NacosPromptInjector.getPromptByKey(nacosConfigService, promptKey);
		if (promptVO != null) {
			NacosPromptInjector.replacePrompt(chatClient, promptVO);
		}
		NacosPromptInjector.registerPromptListener(nacosConfigService, chatClient, promptKey);
	}

	public static void registryPromptByAgentId(ChatClient chatClient, NacosConfigService nacosConfigService,
			String agentId, PromptVO promptVO) {

		try {

			// register agent prompt listener
			nacosConfigService.addListener("agent-base.json", "ai-agent-" + agentId,
					new AbstractListener() {

						String currentPromptKey;

						@Override
						public void receiveConfigInfo(String configInfo) {
							if (StringUtils.isBlank(configInfo)) {
								return;
							}
							String newPromptKey = (String) JSON.parseObject(configInfo).get("promptKey");
							if (StringUtils.isBlank(newPromptKey) || newPromptKey.equals(currentPromptKey)) {
								return;
							}
							try {
								injectPromptByKey(nacosConfigService, chatClient, newPromptKey);
							}
							catch (Exception e) {
								throw new RuntimeException(e);
							}
							if (promptKeyListener.containsKey(currentPromptKey)) {
								Listener listener = promptKeyListener.remove(currentPromptKey);
								nacosConfigService.removeListener(String.format("prompt-%s.json", currentPromptKey),
										"nacos-ai-meta", listener);
							}
							currentPromptKey = newPromptKey;
						}
					});
			if (promptVO != null && promptVO.getPromptKey() != null) {
				registerPromptListener(nacosConfigService, chatClient, promptVO.getPromptKey());
			}

		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * register prompt listener
	 *
	 * @param nacosConfigService
	 * @param chatClient
	 * @param promptKey
	 * @throws NacosException
	 */
	public static void registerPromptListener(NacosConfigService nacosConfigService, ChatClient chatClient,
			String promptKey) throws NacosException {
		try {

			Listener listener = new AbstractListener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					PromptVO promptVO = JSON.parseObject(configInfo, PromptVO.class);
					try {
						replacePrompt(chatClient, promptVO);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};

			nacosConfigService.addListener(String.format("prompt-%s.json", promptKey), "nacos-ai-meta", listener);
			promptKeyListener.put(promptKey, listener);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}


	public static void replacePrompt(ChatClient chatClient, PromptVO promptVO) throws Exception {
		Field defaultChatClientRequest = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
		ReflectionUtils.makeAccessible(defaultChatClientRequest);
		DefaultChatClient.DefaultChatClientRequestSpec object = (DefaultChatClient.DefaultChatClientRequestSpec) defaultChatClientRequest.get(
				chatClient);
		Field systemText = object.getClass().getDeclaredField("systemText");
		ReflectionUtils.makeAccessible(systemText);
		ReflectionUtils.setField(systemText, object, promptVO.getTemplate());

		Field chatOptionsFeild = object.getClass().getDeclaredField("chatOptions");
		chatOptionsFeild.setAccessible(true);
		ChatOptions chatOptions = (ChatOptions) chatOptionsFeild.get(object);
		Field metadataFiled = chatOptions.getClass().getDeclaredField("metadata");
		metadataFiled.setAccessible(true);
		Map<String, String> metadata = (Map<String, String>) metadataFiled.get(chatOptions);
		if (metadata == null) {
			metadata = new HashMap<>();
		}
		metadata.put("promptKey", promptVO.getPromptKey());
		metadata.put("promptVersion", promptVO.getVersion());
		metadataFiled.set(chatOptions, metadata);

	}

}
