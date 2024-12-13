package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import dev.ai.alibaba.samples.executor.ToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;

public class IsAgentService {

	public final ToolService toolService;

	private final ChatClient chatClient;

	public IsAgentService(ChatClient.Builder chatClientBuilder, ToolService toolService) {
		var functions = toolService.agentFunctionsCallback().toArray(FunctionCallback[]::new);

		this.chatClient = chatClientBuilder.defaultSystem("You are a helpful AI Assistant answering questions.")
			.defaultFunctions(functions)
			.build();
		this.toolService = toolService;
	}

	public ChatResponse execute(String input) {
		return chatClient.prompt().user(input).call().chatResponse();
	}

	public ChatResponse executeByPrompt(String input, String prompt) {
		return chatClient.prompt(prompt).user(input).call().chatResponse();
	}

}
