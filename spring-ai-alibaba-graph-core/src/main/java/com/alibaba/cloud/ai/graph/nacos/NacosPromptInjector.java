package com.alibaba.cloud.ai.graph.nacos;

import java.lang.reflect.Field;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.util.ReflectionUtils;

public class NacosPromptInjector {

	public static PromptVO getPrompt(NacosConfigService nacosConfigService, String agentId) {
		try {
			String config = nacosConfigService.getConfig(String.format("prompt-%s.json", agentId), "nacos-ai-agent", 3000L);
			String promptKey = (String) JSON.parseObject(config).get("promptKey");
			String promptConfig = nacosConfigService.getConfig(String.format("prompt-%s.json", promptKey), "nacos-ai-meta", 3000L);
			return JSON.parseObject(promptConfig, PromptVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static void injectPrompt(ChatClient chatClient, NacosConfigService nacosConfigService, String agentId, PromptVO promptVO) {

		try {

			// register agent prompt listener
			nacosConfigService.addListener(String.format("prompt-%s.json", agentId), "nacos-ai-agent", new AbstractListener() {

				String currentPromptKey;

				@Override
				public void receiveConfigInfo(String configInfo) {
					String newPromptKey = (String) JSON.parseObject(configInfo).get("promptKey");
					if (newPromptKey.equals(currentPromptKey)) {
						return;
					}
					try {
						registryPromptListener(nacosConfigService, chatClient, newPromptKey);
					}
					catch (NacosException e) {
						throw new RuntimeException(e);
					}
				}
			});

			registryPromptListener(nacosConfigService,chatClient,promptVO.getPromptKey());

		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	private static void registryPromptListener(NacosConfigService nacosConfigService, ChatClient chatClient, String promptKey) throws NacosException {
		try {
			nacosConfigService.addListener(String.format("prompt-%s.json", promptKey), "nacos-ai-meta", new AbstractListener() {

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
			});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static void replacePrompt(ChatClient chatClient, PromptVO promptVO) throws Exception {
		Field defaultChatClientRequest = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
		ReflectionUtils.makeAccessible(defaultChatClientRequest);
		DefaultChatClient.DefaultChatClientRequestSpec object = (DefaultChatClient.DefaultChatClientRequestSpec) defaultChatClientRequest.get(chatClient);
		Field systemText = object.getClass().getDeclaredField("systemText");
		ReflectionUtils.makeAccessible(systemText);
		ReflectionUtils.setField(systemText, object, promptVO.getTemplate());

		Field chatOptionsFeild = object.getClass().getDeclaredField("chatOptions");
		chatOptionsFeild.setAccessible(true);
		ChatOptions chatOptions = (ChatOptions) chatOptionsFeild.get(object);
		Field metadataFiled = chatOptions.getClass().getDeclaredField("metadata");
		metadataFiled.setAccessible(true);
		Map<String, String> metadata = (Map<String, String>) metadataFiled.get(chatOptions);
		metadata.put("promptKey", promptVO.getPromptKey());
		metadata.put("promptVersion", promptVO.getVersion());
	}

}
