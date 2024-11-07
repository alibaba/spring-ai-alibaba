package dev.ai.alibaba.samples.executor;

import dev.ai.alibaba.samples.executor.function.AgentFunctionCallbackWrapper;
import lombok.NonNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ToolService {

	private final List<FunctionCallback> agentFunctionCallbackWrappers;

	private final ApplicationContext applicationContext;

	public ToolService(ApplicationContext applicationContext) {

		this.applicationContext = applicationContext;
		var AgentFunctionCallbackMap = applicationContext.getBeansOfType(AgentFunctionCallbackWrapper.class);

		agentFunctionCallbackWrappers = AgentFunctionCallbackMap.values()
			.stream()
			.map(FunctionCallback.class::cast)
			.toList();
	}

	public List<FunctionCallback> agentFunctionsCallback() {
		return agentFunctionCallbackWrappers;
	}

	public <I, O> Optional<AgentFunctionCallbackWrapper<I, O>> agentFunction(@NonNull String name) {
		return (applicationContext.containsBean(name))
				? Optional.of(applicationContext.getBean(name, AgentFunctionCallbackWrapper.class)) : Optional.empty();
	}

	public <O> Optional<O> getFunctionResult(@NonNull ToolResponseMessage.ToolResponse response) {
		return agentFunction(response.name()).map(functionCallback -> (O) functionCallback.convertResponse(response));
	}

	public ToolResponseMessage buildToolResponseMessage(@NonNull ToolResponseMessage.ToolResponse response) {
		return new ToolResponseMessage(List.of(response), Map.of());
	}

	public CompletableFuture<ToolResponseMessage.ToolResponse> executeFunction(AssistantMessage.ToolCall toolCall,
			Map<String, Object> toolContextMap) {
		CompletableFuture<ToolResponseMessage.ToolResponse> result = new CompletableFuture<>();

		String functionName = toolCall.name();
		String functionArguments = toolCall.arguments();

		var functionCallback = agentFunction(functionName);

		if (functionCallback.isPresent()) {
			var functionResponse = functionCallback.get().call(functionArguments, new ToolContext(toolContextMap));

			result.complete(new ToolResponseMessage.ToolResponse(toolCall.id(), functionName, functionResponse));
		}
		else {

			result.completeExceptionally(
					new IllegalStateException("No function callback found for function name: " + functionName));
		}

		return result;

	}

	public CompletableFuture<ToolResponseMessage.ToolResponse> executeFunction(AssistantMessage.ToolCall toolCall) {
		return executeFunction(toolCall, Map.of());
	}

}
