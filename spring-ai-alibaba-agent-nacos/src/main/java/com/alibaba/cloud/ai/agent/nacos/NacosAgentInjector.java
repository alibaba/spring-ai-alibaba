package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
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
			NacosPromptInjector.registryPromptListener(nacosConfigService, chatClient, promptKey);
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void injectModel(NacosOptions nacosOptions, ChatClient chatClient, String agentId) {
		ModelVO modelVO = NacosModelInjector.getModelByAgentId(nacosOptions, agentId);
		if (modelVO != null) {
			try {
				ChatModel chatModel = NacosModelInjector.initModel(modelVO);
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

		return NacosModelInjector.initModel(modelVo);
	}

}
