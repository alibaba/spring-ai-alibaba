package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.nacos.client.config.NacosConfigService;

public class NacosOptions {

	protected boolean modelSpecified;

	protected boolean modelConfigEncrypted;

	protected boolean promptSpecified;

	String promptKey;

	private NacosConfigService nacosConfigService;

	private String agentId;

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

	public boolean isModelSpecified() {
		return modelSpecified;
	}

	public void setModelSpecified(boolean modelSpecified) {
		this.modelSpecified = modelSpecified;
	}

	public boolean isPromptSpecified() {
		return promptSpecified;
	}

	public void setPromptSpecified(boolean promptSpecified) {
		this.promptSpecified = promptSpecified;
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
