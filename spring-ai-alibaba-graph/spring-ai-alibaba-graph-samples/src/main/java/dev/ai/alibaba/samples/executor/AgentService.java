package dev.ai.alibaba.samples.executor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;

public class AgentService {

	public final ToolService toolService;

	private final ChatClient chatClient;

	public AgentService(ChatClient.Builder chatClientBuilder, ToolService toolService) {
		var functions = toolService.agentFunctionsCallback().toArray(FunctionCallback[]::new);

		this.chatClient = chatClientBuilder.defaultSystem("You are a helpful AI Assistant answering questions.")
			.defaultFunctions(functions)
			.build();
		this.toolService = toolService;
	}

	public ChatResponse execute(String input) {
		return chatClient.prompt().user(input).call().chatResponse();
	}

}
