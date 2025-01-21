/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.function.AgentFunctionCallbackWrapper;
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
