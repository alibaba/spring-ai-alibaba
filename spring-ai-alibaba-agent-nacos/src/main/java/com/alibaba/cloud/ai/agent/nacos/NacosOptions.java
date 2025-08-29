package com.alibaba.cloud.ai.agent.nacos;

import java.util.Properties;

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import com.alibaba.nacos.maintainer.client.ai.NacosAiMaintainerServiceImpl;
import lombok.Data;

@Data
public class NacosOptions {

	protected boolean modelSpecified;

	protected boolean modelConfigEncrypted;

	protected boolean promptSpecified;

	String promptKey;

	private NacosConfigService nacosConfigService;

	private AiMaintainerService nacosAiMaintainerService;

	NacosMcpOperationService mcpOperationService;

	private String agentId;

	private String mcpNamespace;

	public NacosOptions(Properties properties) throws NacosException {
		nacosConfigService = new NacosConfigService(properties);
		nacosAiMaintainerService = new NacosAiMaintainerServiceImpl(properties);
		mcpOperationService = new NacosMcpOperationService(properties);
		agentId = properties.getProperty("agentId");
		mcpNamespace = properties.getProperty("mcpNamespace", properties.getProperty("namespace"));

	}
}
