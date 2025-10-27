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
package com.alibaba.cloud.ai.graph.stream;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.GraphFluxGenerator;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Map;

public class LLmNodeAction implements NodeAction {

	private ChatModel chatModel;

	private String nodeId;

	public LLmNodeAction(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	public LLmNodeAction(ChatModel chatModel, String nodeId) {
		this.chatModel = chatModel;
		this.nodeId = nodeId;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		// Create prompt with user message
		UserMessage message = new UserMessage((String) state.value(OverAllState.DEFAULT_INPUT_KEY).get());
		Flux<ChatResponse> stream = chatModel.stream(new Prompt(message));
		return Map.of("messages", GraphFluxGenerator
				.builder()
				.startingNode(nodeId)
				.outKey("messages")
				.build(stream));
	}

}
