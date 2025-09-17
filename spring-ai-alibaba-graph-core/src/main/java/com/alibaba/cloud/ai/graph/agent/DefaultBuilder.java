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

