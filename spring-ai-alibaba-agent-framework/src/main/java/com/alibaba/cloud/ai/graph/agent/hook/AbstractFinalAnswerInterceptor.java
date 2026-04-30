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
 * Shared template for guard interceptors that enforce a synthetic final-answer turn by disabling
 * all tool exposure for the model.
 *
 * <p>
 * Guard hooks such as {@link AbstractToolCallGuardHook} inject an {@link AgentInstructionMessage}
 * when they decide the model should stop calling tools and answer directly. This interceptor runs
 * on the subsequent model call, detects that synthetic instruction via metadata, and strips tool
 * callbacks, tool descriptions, and internal tool execution settings from the request.
 * </p>
 *
 * <p>
 * In other words, the hook decides <em>when</em> to enter final-answer mode, while this interceptor
 * enforces <em>how</em> that next model turn is executed.
 * </p>
 */
public abstract class AbstractFinalAnswerInterceptor extends ModelInterceptor {

	/**
	 * Intercept the current model request.
	 * <p>
	 * If the last message is not the synthetic final-answer instruction, the request is passed through
	 * unchanged. If the final-answer instruction is present, the request is cloned with tool exposure
	 * removed so the model can only produce a direct answer for that turn.
	 * </p>
	 * @param request the current model request
	 * @param handler the downstream model call handler
	 * @return the downstream model response, using either the original or the tool-stripped request
	 */
	@Override
	public final ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		if (!shouldDisableTools(request)) {
			return handler.call(request);
		}
		return handler.call(disableToolExposure(request));
	}

	/**
	 * Determine whether the current model turn is the synthetic final-answer turn created by a guard hook.
	 * <p>
	 * The default implementation only activates when the last message is an {@link AgentInstructionMessage}
	 * whose metadata contains {@link #finalAnswerInstructionMetadataKey()} set to {@code true}.
	 * </p>
	 * @param request the current model request
	 * @return {@code true} if tools should be disabled for this request
	 */
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
	 * <p>
	 * Each guard type uses its own key so the matching interceptor only reacts to instructions
	 * created by that specific guard.
	 * </p>
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


