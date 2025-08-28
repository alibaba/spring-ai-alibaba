package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.DefaultBuilder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class NacosReactAgentBuilder extends DefaultBuilder {

	private NacosOptions nacosOptions;

	public NacosReactAgentBuilder nacosOptions(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions;
		return this;
	}

	@Override
	public Builder model(ChatModel model) {
		super.model(model);
		nacosOptions.modelSpecified = true;
		return this;
	}

	public Builder instruction(String instruction) {
		super.instruction(instruction);
		nacosOptions.promptSpecified = true;
		return this;
	}

	@Override
	public ReactAgent build() throws GraphStateException {

		if (model == null) {
			this.model = NacosAgentInjector.initModel(nacosOptions, this.name);
		}

		if (chatClient == null) {
			ChatClient.Builder clientBuilder = ChatClient.builder(model);
			if (chatOptions != null) {
				clientBuilder.defaultOptions(chatOptions);
			}
			if (instruction != null) {
				clientBuilder.defaultSystem(instruction);
			}
			chatClient = clientBuilder.build();
		}

		if (!nacosOptions.modelSpecified) {
			NacosAgentInjector.injectModel(nacosOptions, chatClient, this.name);
		}
		if (!nacosOptions.promptSpecified) {
			NacosAgentInjector.injectPrompt(nacosOptions.getNacosConfigService(), chatClient, nacosOptions.promptKey);
		}

		LlmNode.Builder llmNodeBuilder = LlmNode.builder().chatClient(chatClient).messagesKey("messages");
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
