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

package com.alibaba.cloud.ai.agent.nacos;

import java.util.List;

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.DefaultBuilder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.nacos.common.utils.StringUtils;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

public class NacosReactAgentBuilder extends DefaultBuilder {

	private NacosOptions nacosOptions;

	public NacosReactAgentBuilder nacosOptions(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions; return this;
	}

	@Override
	public Builder model(ChatModel model) {
		super.model(model); nacosOptions.modelSpecified = true; return this;
	}

	public Builder instruction(String instruction) {
		super.instruction(instruction); nacosOptions.promptSpecified = true; return this;
	}

	@Override
	public ReactAgent build() throws GraphStateException {
		if (super.name == null) {
			this.name = nacosOptions.getAgentName();
		} if (model == null && StringUtils.isNotBlank(this.name)) {
			this.model = NacosAgentInjector.initModel(nacosOptions, this.name);
		} if (chatClient == null) {
			ChatClient.Builder clientBuilder = null;

			ObservationConfigration observationConfigration = nacosOptions.getObservationConfigration();
			if (observationConfigration == null) {
				clientBuilder = ChatClient.builder(model);
			}
			else {
				clientBuilder = ChatClient.builder(model, observationConfigration.getObservationRegistry() == null ? ObservationRegistry.NOOP : observationConfigration.getObservationRegistry(), nacosOptions.getObservationConfigration()
						.getChatClientObservationConvention());
			}

			if (chatOptions != null) {
				clientBuilder.defaultOptions(chatOptions);
			} if (instruction != null) {
				clientBuilder.defaultSystem(instruction);
			}
			chatClient = clientBuilder.build();
		}

		if (!nacosOptions.modelSpecified) {
			NacosAgentInjector.injectModel(nacosOptions, chatClient, this.name);
		}

		if (!nacosOptions.promptSpecified) {
			if (nacosOptions.promptKey != null) {
				NacosAgentInjector.injectPrompt(nacosOptions.getNacosConfigService(), chatClient, nacosOptions.promptKey);
			}
			else {
				AgentVO agentVO = NacosAgentInjector.loadAgentVO(nacosOptions.getNacosConfigService(), this.name);
				this.description = agentVO.getDescription();
				NacosAgentInjector.injectPromptByAgentName(nacosOptions.getNacosConfigService(), chatClient, nacosOptions.getAgentName(), agentVO);
			}
		}

		List<ToolCallback> toolCallbacks = NacosMcpToolsInjector.loadMcpTools(nacosOptions, this.name);

		this.tools = toolCallbacks;

		LlmNode.Builder llmNodeBuilder = LlmNode.builder().stream(true).chatClient(chatClient)
				.messagesKey(this.inputKey); if (outputKey != null && !outputKey.isEmpty()) {
			llmNodeBuilder.outputKey(outputKey);
		}

		if (CollectionUtils.isNotEmpty(tools)) {
			llmNodeBuilder.toolCallbacks(tools);
		} LlmNode llmNode = llmNodeBuilder.build();

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
		NacosMcpToolsInjector.registry(llmNode, toolNode, nacosOptions, this.name);

		return new ReactAgent(llmNode, toolNode, this);
	}
}
