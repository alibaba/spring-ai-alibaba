/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.StringUtils;

public class ToolNode implements NodeAction {
	public static final String TOOL_RESPONSE_KEY = "tool_response";

	private String llmResponseKey;
	private String outputKey;

	private AssistantMessage assistantMessage;
	private ToolCallingManager toolCallingManager;
	private ToolCallbackResolver toolCallbackResolver;

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (!StringUtils.hasLength(llmResponseKey)) {
			this.llmResponseKey = LlmNode.LLM_RESPONSE_KEY;
		}

		this.assistantMessage = (AssistantMessage) state.value(this.llmResponseKey).orElseThrow(() -> new IllegalStateException("No LLM response found"));
		ToolResponseMessage toolResponseMessage = executeFunction(assistantMessage);

		String outputKey = StringUtils.hasLength(this.outputKey) ? this.outputKey : TOOL_RESPONSE_KEY;
		return Map.of(outputKey, toolResponseMessage);
	}

	private ToolResponseMessage executeFunction(AssistantMessage assistantMessage) {
		// execute the tool function
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
			String toolName = toolCall.name();
			String toolArgs = toolCall.arguments();

			FunctionCallback toolCallback = toolCallbackResolver.resolve(toolName);
			String toolResult = toolCallback.call(toolArgs, new ToolContext(Map.of()));
			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
		}
		return new ToolResponseMessage(toolResponses, Map.of());
	}

	public static class ToolNodeMessage {
		private AssistantMessage assistantMessage;
		private Prompt requestPrompt;

		ToolNodeMessage(AssistantMessage assistantMessage, Prompt requestPrompt) {
			this.assistantMessage = assistantMessage;
			this.requestPrompt = requestPrompt;
		}

		AssistantMessage getAssistantMessage() {
			return assistantMessage;
		}

		void setAssistantMessage(AssistantMessage assistantMessage) {
			this.assistantMessage = assistantMessage;
		}

		Prompt getRequestPrompt() {
			return requestPrompt;
		}

		void setRequestPrompt(Prompt requestPrompt) {
			this.requestPrompt = requestPrompt;
		}
	}

}
