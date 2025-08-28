package com.alibaba.cloud.ai.graph.nacos;

import com.alibaba.nacos.client.config.NacosConfigService;

public class NacosOptions {

	NacosConfigService nacosConfigService;

	private String promptKey;

	private String agentId;

	private boolean modelConfigEncrypted;

	public NacosConfigService getNacosConfigService() {
		return nacosConfigService;
	}

	public void setNacosConfigService(NacosConfigService nacosConfigService) {
		this.nacosConfigService = nacosConfigService;
	}

	public boolean isModelConfigEncrypted() {
		return modelConfigEncrypted;
	}

	public void setModelConfigEncrypted(boolean modelConfigEncrypted) {
		this.modelConfigEncrypted = modelConfigEncrypted;
	}

	public String getPromptKey() {
		return promptKey;
	}

	public void setPromptKey(String promptKey) {
		this.promptKey = promptKey;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
}
