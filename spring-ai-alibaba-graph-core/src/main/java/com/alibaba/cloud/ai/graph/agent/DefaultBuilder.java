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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.ai.chat.client.ChatClient;

public class DefaultBuilder extends Builder {

	@Override
	public ReactAgent build() throws GraphStateException {

		if (chatClient == null) {
			if (model == null) {
				throw new IllegalArgumentException("Either chatClient or model must be provided");
			}
			ChatClient.Builder clientBuilder = ChatClient.builder(model);
			if (chatOptions != null) {
				clientBuilder.defaultOptions(chatOptions);
			}
			if (instruction != null) {
				clientBuilder.defaultSystem(instruction);
			}
			chatClient = clientBuilder.build();
		}

		LlmNode.Builder llmNodeBuilder = LlmNode.builder()
				.stream(true)
				.chatClient(chatClient)
				.messagesKey(this.inputKey);
		// For graph built from ReactAgent, the only legal key used inside must be
		// messages.
		// if (outputKey != null && !outputKey.isEmpty()) {
		// llmNodeBuilder.outputKey(outputKey);
		// }
		if (CollectionUtils.isNotEmpty(tools)) {
			llmNodeBuilder.toolCallbacks(tools);
		}
		LlmNode llmNode = llmNodeBuilder.build();

		ToolNode toolNode = null;
		if (resolver != null) {
			toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		}
		else if (tools != null) {
			toolNode = ToolNode.builder().toolCallbacks(tools).build();
		}
		else {
			toolNode = ToolNode.builder().build();
		}

		return new ReactAgent(llmNode, toolNode, this);
	}

}

