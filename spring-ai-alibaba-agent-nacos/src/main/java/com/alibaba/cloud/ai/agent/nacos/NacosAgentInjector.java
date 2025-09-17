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

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public class NacosAgentInjector {

	public static void injectPrompt(NacosConfigService nacosConfigService, ChatClient chatClient, String promptKey) {

		try {
			PromptVO promptVO = NacosPromptInjector.getPromptByKey(nacosConfigService, promptKey);
			if (promptVO != null) {
				NacosPromptInjector.replacePrompt(chatClient, promptVO);
			}
			NacosPromptInjector.registerPromptListener(nacosConfigService, chatClient, promptKey);
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * load prompt by agent id.
	 *
	 * @param nacosConfigService
	 * @param agentName
	 * @return
	 */
	public static AgentVO loadAgentVO(NacosConfigService nacosConfigService, String agentName) {
		try {
			String config = nacosConfigService.getConfig("agent-base.json", "ai-agent-" + agentName,
					3000L);
			return JSON.parseObject(config, AgentVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static void injectPromptByAgentName(NacosConfigService nacosConfigService, ChatClient chatClient,String agentName,AgentVO agentVO) {

		try {
			if (agentVO == null) {
				return;
			}
			PromptVO promptVO = NacosPromptInjector.loadPromptByAgentId(nacosConfigService, agentVO);
			if (promptVO != null) {
				NacosPromptInjector.replacePrompt(chatClient, promptVO);
			}
			NacosPromptInjector.registryPromptByAgentId(chatClient, nacosConfigService, agentName, promptVO);
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void injectModel(NacosOptions nacosOptions, ChatClient chatClient, String agentId) {
		ModelVO modelVO = NacosModelInjector.getModelByAgentId(nacosOptions, agentId);
		if (modelVO != null) {
			try {
				ChatModel chatModel = NacosModelInjector.initModel(nacosOptions, modelVO);
				NacosModelInjector.replaceModel(chatClient, chatModel);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		NacosModelInjector.registerModelListener(chatClient, nacosOptions, agentId);
	}


	public static ChatModel initModel(NacosOptions nacosOptions, String agentId) {
		ModelVO modelVo = NacosModelInjector.getModelByAgentId(nacosOptions, agentId);
		if (modelVo == null) {
			return null;
		}

		return NacosModelInjector.initModel(nacosOptions, modelVo);
	}

}
