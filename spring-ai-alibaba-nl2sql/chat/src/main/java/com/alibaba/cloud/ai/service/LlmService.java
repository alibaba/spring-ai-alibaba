package com.alibaba.cloud.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

	private final ChatClient chatClient;

	public LlmService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public String call(String prompt) {
		return chatClient.prompt().user(prompt).call().content();
	}

	public String callWithSystemPrompt(String system, String user) {
		return chatClient.prompt().system(system).user(user).call().content();
	}

}