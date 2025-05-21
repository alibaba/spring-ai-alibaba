package com.alibaba.cloud.ai.util;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

	@Value("${spring.ai.openai.api-key}")
	private String openAiApiKey;

	@Value("${spring.ai.openai.base-url}")
	private String baseUrl;

	@Value("${spring.ai.openai.model}")
	private String model;

	@Bean
	public ChatModel chatModel() {
		OpenAiApi openAiApi = OpenAiApi.builder().apiKey(openAiApiKey).baseUrl(baseUrl).build();
		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder().model(model).temperature(0.7).build();
		return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
	}

	@Bean
	public ChatClient chatClient(@Qualifier("chatModel") ChatModel chatModel) {
		return ChatClient.create(chatModel);
	}

}