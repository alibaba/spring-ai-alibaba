/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.dashscope.api;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ChatCompletionFunction;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.Role;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ToolCall;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class to support Streaming function calling. It can merge the streamed
 * ChatCompletionChunk in case of function calling message.
 *
 * @author Ken
 */
public class DashScopeAiStreamFunctionCallingHelper {

	private Boolean incrementalOutput = false;

	public DashScopeAiStreamFunctionCallingHelper() {
	}

	public DashScopeAiStreamFunctionCallingHelper(Boolean incrementalOutput) {
		this.incrementalOutput = incrementalOutput;
	}

	/**
	 * Merge the previous and current ChatCompletionChunk into a single one.
	 * @param previous the previous ChatCompletionChunk
	 * @param current the current ChatCompletionChunk
	 * @return the merged ChatCompletionChunk
	 */
	public ChatCompletionChunk merge(ChatCompletionChunk previous, ChatCompletionChunk current) {
		if (previous == null) {
			return current;
		}

		String id = (current.requestId() != null ? current.requestId() : previous.requestId());
		TokenUsage usage = (current.usage() != null ? current.usage() : previous.usage());

		Choice previousChoice0 = previous.output() == null ? null : previous.output().choices().get(0);
		Choice currentChoice0 = current.output() == null ? null : current.output().choices().get(0);

		// compatibility of incremental_output false for streaming function call
		if (!incrementalOutput && isStreamingToolFunctionCall(current)) {
			if (!isStreamingToolFunctionCallFinish(current)) {
				return new ChatCompletionChunk(id, new ChatCompletionOutput(null, List.of(new Choice(null, null))),
						usage);
			}
			else {
				return new ChatCompletionChunk(id, new ChatCompletionOutput(null, List.of(currentChoice0)), usage);
			}
		}

		Choice choice = merge(previousChoice0, currentChoice0);
		List<Choice> chunkChoices = choice == null ? List.of() : List.of(choice);
		return new ChatCompletionChunk(id, new ChatCompletionOutput(null, chunkChoices), usage);
	}

	private Choice merge(Choice previous, Choice current) {
		if (previous == null) {
			return current;
		}

		ChatCompletionFinishReason finishReason = (current.finishReason() != null ? current.finishReason()
				: previous.finishReason());

		ChatCompletionMessage message = merge(previous.message(), current.message());
		return new Choice(finishReason, message);
	}

	private ChatCompletionMessage merge(ChatCompletionMessage previous, ChatCompletionMessage current) {

		String content = (current.content() != null ? current.content()
				: (previous.content() != null) ? previous.content() : "");
		Role role = (current.role() != null ? current.role() : previous.role());
		role = (role != null ? role : Role.ASSISTANT); // default to ASSISTANT (if null
		String name = (StringUtils.hasText(current.name()) ? current.name() : previous.name());
		String toolCallId = (StringUtils.hasText(current.toolCallId()) ? current.toolCallId() : previous.toolCallId());
		String reasoningContent = (current.reasoningContent() != null ? current.reasoningContent()
				: previous.reasoningContent());

		List<ToolCall> toolCalls = new ArrayList<>();
		ToolCall lastPreviousTooCall = null;
		if (previous.toolCalls() != null) {
			lastPreviousTooCall = previous.toolCalls().get(previous.toolCalls().size() - 1);
			if (previous.toolCalls().size() > 1) {
				toolCalls.addAll(previous.toolCalls().subList(0, previous.toolCalls().size() - 1));
			}
		}
		if (current.toolCalls() != null) {
			if (current.toolCalls().size() > 1) {
				throw new IllegalStateException("Currently only one tool call is supported per message!");
			}
			var currentToolCall = current.toolCalls().iterator().next();
			if (StringUtils.hasText(currentToolCall.id())) {
				if (lastPreviousTooCall != null) {
					toolCalls.add(lastPreviousTooCall);
				}
				toolCalls.add(currentToolCall);
			}
			else {
				toolCalls.add(merge(lastPreviousTooCall, currentToolCall));
			}
		}
		else {
			if (lastPreviousTooCall != null) {
				toolCalls.add(lastPreviousTooCall);
			}
		}
		return new ChatCompletionMessage(content, role, name, toolCallId, toolCalls, reasoningContent);
	}

	private ToolCall merge(ToolCall previous, ToolCall current) {
		if (previous == null) {
			return current;
		}
		String id = (StringUtils.hasText(current.id()) ? current.id() : previous.id());
		String type = (StringUtils.hasText(current.type()) ? current.type() : previous.type());
		ChatCompletionFunction function = merge(previous.function(), current.function());
		return new ToolCall(id, type, function);
	}

	private ChatCompletionFunction merge(ChatCompletionFunction previous, ChatCompletionFunction current) {
		if (previous == null) {
			return current;
		}
		String name = (StringUtils.hasText(current.name()) ? current.name() : previous.name());
		StringBuilder arguments = new StringBuilder();
		if (previous.arguments() != null) {
			arguments.append(previous.arguments());
		}
		if (current.arguments() != null) {
			arguments.append(current.arguments());
		}
		return new ChatCompletionFunction(name, arguments.toString());
	}

	/**
	 * @param chatCompletion the ChatCompletionChunk to check
	 * @return true if the ChatCompletionChunk is a streaming tool function call.
	 */
	public boolean isStreamingToolFunctionCall(ChatCompletionChunk chatCompletion) {

		if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.output().choices())) {
			return false;
		}

		var choice = chatCompletion.output().choices().get(0);
		if (choice == null || choice.message() == null) {
			return false;
		}
		return !CollectionUtils.isEmpty(choice.message().toolCalls());
	}

	/**
	 * @param chatCompletion the ChatCompletionChunk to check
	 * @return true if the ChatCompletionChunk is a streaming tool function call and it is
	 * the last one.
	 */
	public boolean isStreamingToolFunctionCallFinish(ChatCompletionChunk chatCompletion) {

		if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.output().choices())) {
			return false;
		}

		var choice = chatCompletion.output().choices().get(0);
		if (choice == null || choice.message() == null) {
			return false;
		}
		return choice.finishReason() == ChatCompletionFinishReason.TOOL_CALLS;
	}

	/**
	 * Convert the ChatCompletionChunk into a ChatCompletion. The Usage is set to null.
	 * @param chunk the ChatCompletionChunk to convert
	 * @return the ChatCompletion
	 */
	public ChatCompletion chunkToChatCompletion(ChatCompletionChunk chunk) {
		return new ChatCompletion(chunk.requestId(), chunk.output(), chunk.usage());
	}

}
