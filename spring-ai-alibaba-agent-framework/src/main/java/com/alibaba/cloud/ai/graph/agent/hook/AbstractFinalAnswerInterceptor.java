/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.Map;

/**
 * Shared template for guard interceptors that enter a final-answer turn by disabling
 * all tool exposure for the model.
 */
public abstract class AbstractFinalAnswerInterceptor extends ModelInterceptor {

	@Override
	public final ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		if (!shouldDisableTools(request)) {
			return handler.call(request);
		}
		return handler.call(disableToolExposure(request));
	}

	protected boolean shouldDisableTools(ModelRequest request) {
		List<Message> messages = request.getMessages();
		if (messages == null || messages.isEmpty()) {
			return false;
		}

		Message lastMessage = messages.get(messages.size() - 1);
		if (!(lastMessage instanceof AgentInstructionMessage instructionMessage)) {
			return false;
		}

		return Boolean.TRUE.equals(instructionMessage.getMetadata().get(finalAnswerInstructionMetadataKey()));
	}

	/**
	 * Metadata key that marks the synthetic final-answer instruction injected by a guard
	 * hook.
	 * @return the metadata key used to detect final-answer mode
	 */
	protected abstract String finalAnswerInstructionMetadataKey();

	private ModelRequest disableToolExposure(ModelRequest request) {
		ToolCallingChatOptions options = request.getOptions();
		if (options != null) {
			options = options.copy();
			options.setToolCallbacks(List.of());
			options.setInternalToolExecutionEnabled(false);
		}

		return ModelRequest.builder(request)
				.options(options)
				.tools(List.of())
				.dynamicToolCallbacks(List.of())
				.toolDescriptions(Map.of())
				.build();
	}

}


