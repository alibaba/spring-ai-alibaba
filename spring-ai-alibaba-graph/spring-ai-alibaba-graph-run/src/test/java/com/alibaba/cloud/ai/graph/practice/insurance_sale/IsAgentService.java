package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import dev.ai.alibaba.samples.executor.ToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.List;
import java.util.Map;

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

	private ToolResponseMessage buildToolResponseMessage(IsExecutor.Step intermediateStep) {
		var toolCall = intermediateStep.action().toolCall();
		var response = new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
				intermediateStep.observation());
		return new ToolResponseMessage(List.of(response), Map.of());
	}

	public ChatResponse execute(String input, List<IsExecutor.Step> intermediateSteps) {
		var messages = intermediateSteps.stream()
			.map(this::buildToolResponseMessage)
			.toArray(ToolResponseMessage[]::new);
		return chatClient.prompt().user(input).messages(messages).call().chatResponse();
	}

}
