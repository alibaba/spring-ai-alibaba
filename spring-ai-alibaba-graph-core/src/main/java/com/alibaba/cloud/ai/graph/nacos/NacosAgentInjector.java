package com.alibaba.cloud.ai.graph.nacos;

import com.alibaba.nacos.client.config.NacosConfigService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class NacosAgentInjector {

	public static void injectPrompt(NacosConfigService nacosConfigService, ChatClient chatClient, String promptKey) {

		try {
			PromptVO promptVO = NacosPromptInjector.getPromptByKey(nacosConfigService, promptKey);
			if (promptVO != null) {
				NacosPromptInjector.replacePrompt(chatClient, promptVO);
			}
			NacosPromptInjector.registryPromptListener(nacosConfigService, chatClient, promptKey);
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void injectModel(NacosOptions nacosOptions, ChatModel chatModel, String agentId) {
		ModelVO modelVO = NacosModelInjector.getModelByAgentId(nacosOptions, agentId);
		if (modelVO != null) {
			try {
				NacosModelInjector.replaceModel(chatModel, modelVO);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		NacosModelInjector.injectModel(chatModel, nacosOptions, agentId);
	}


	public static ChatModel initModel(NacosOptions nacosOptions, String agentId) {
		ModelVO model = NacosModelInjector.getModelByAgentId(nacosOptions, agentId);
		if (model == null) {
			return null;
		}
		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey(model.getApiKey()).baseUrl(model.getBaseUrl())
				.build();

		OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder();
		if (model.getTemperature() != null) {
			chatOptionsBuilder.temperature(Double.parseDouble(model.getTemperature()));
		}
		if (model.getMaxTokens() != null) {
			chatOptionsBuilder.maxTokens(Integer.parseInt(model.getMaxTokens()));
		}
		OpenAiChatOptions openaiChatOptions = chatOptionsBuilder
				.model(model.getModel()).build();
		return OpenAiChatModel.builder().defaultOptions(openaiChatOptions).openAiApi(openAiApi)
				.build();
	}

}
