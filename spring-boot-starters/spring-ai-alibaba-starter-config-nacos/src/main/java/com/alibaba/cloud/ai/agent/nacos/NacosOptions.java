/*
 * Copyright 2024-2026 the original author or authors.
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

import java.time.Duration;
import java.util.Properties;

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import com.alibaba.nacos.maintainer.client.ai.NacosAiMaintainerServiceImpl;

import org.springframework.boot.convert.DurationStyle;

public class NacosOptions {

	private static final Duration DEFAULT_MCP_GATEWAY_TOOL_TIMEOUT = Duration.ofSeconds(30);

	protected boolean encrypted;

	protected boolean modelEncrypted;

	protected boolean agentBaseEncrypted;

	protected boolean promptEncrypted;

	protected boolean mcpServersEncrypted;

	String promptKey;

	private NacosConfigService nacosConfigService;

	private AiMaintainerService nacosAiMaintainerService;

	NacosMcpOperationService mcpOperationService;

	private ObservationConfiguration observationConfiguration;

	private String agentName;

	private String mcpNamespace;

	private Duration mcpGatewayToolTimeout = DEFAULT_MCP_GATEWAY_TOOL_TIMEOUT;

	private void encryptParamInit(Properties properties) {
		encrypted = Boolean.parseBoolean(properties.getProperty("encrypted", "false"));
		String defaultEncrypted = String.valueOf(encrypted);
		modelEncrypted = Boolean.parseBoolean(properties.getProperty("modelEncrypted", defaultEncrypted));
		promptEncrypted = Boolean.parseBoolean(properties.getProperty("promptEncrypted", defaultEncrypted));
		mcpServersEncrypted = Boolean.parseBoolean(properties.getProperty("mcpServersEncrypted", defaultEncrypted));
		agentBaseEncrypted = Boolean.parseBoolean(properties.getProperty("agentBaseEncrypted", defaultEncrypted));
		properties.remove("modelEncrypted");
		properties.remove("promptEncrypted");
		properties.remove("mcpServersEncrypted");
		properties.remove("agentBaseEncrypted");
		properties.remove("encrypted");
	}

	public NacosOptions(Properties properties) throws NacosException {

		encryptParamInit(properties);
		agentName = properties.getProperty("agentName");
		mcpNamespace = properties.getProperty("mcpNamespace", properties.getProperty("namespace"));
		mcpGatewayToolTimeout = parseMcpGatewayToolTimeout(properties);
		String rawLabels = properties.getProperty("nacos.app.conn.labels", "");
		if (StringUtils.isBlank(rawLabels)) {
			rawLabels = "AgentName=" + agentName;
		}
		else {
			rawLabels += ",AgentName=" + agentName;
		}
		properties.put("nacos.app.conn.labels", rawLabels);

		nacosConfigService = new NacosConfigService(properties);
		nacosAiMaintainerService = new NacosAiMaintainerServiceImpl(properties);
		mcpOperationService = new NacosMcpOperationService(properties);

	}

	private Duration parseMcpGatewayToolTimeout(Properties properties) {
		String timeout = properties.getProperty("mcpGatewayToolTimeout");
		if (!StringUtils.isBlank(properties.getProperty("mcp-gateway-tool-timeout"))) {
			timeout = properties.getProperty("mcp-gateway-tool-timeout");
		}
		if (StringUtils.isBlank(timeout)) {
			return DEFAULT_MCP_GATEWAY_TOOL_TIMEOUT;
		}
		properties.remove("mcpGatewayToolTimeout");
		properties.remove("mcp-gateway-tool-timeout");
		return DurationStyle.detectAndParse(timeout);
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public boolean isModelEncrypted() {
		return modelEncrypted;
	}

	public void setModelEncrypted(boolean modelEncrypted) {
		this.modelEncrypted = modelEncrypted;
	}

	public boolean isAgentBaseEncrypted() {
		return agentBaseEncrypted;
	}

	public void setAgentBaseEncrypted(boolean agentBaseEncrypted) {
		this.agentBaseEncrypted = agentBaseEncrypted;
	}

	public boolean isPromptEncrypted() {
		return promptEncrypted;
	}

	public void setPromptEncrypted(boolean promptEncrypted) {
		this.promptEncrypted = promptEncrypted;
	}

	public boolean isMcpServersEncrypted() {
		return mcpServersEncrypted;
	}

	public void setMcpServersEncrypted(boolean mcpServersEncrypted) {
		this.mcpServersEncrypted = mcpServersEncrypted;
	}

	public String getPromptKey() {
		return promptKey;
	}

	public void setPromptKey(String promptKey) {
		this.promptKey = promptKey;
	}

	public NacosConfigService getNacosConfigService() {
		return nacosConfigService;
	}

	public void setNacosConfigService(NacosConfigService nacosConfigService) {
		this.nacosConfigService = nacosConfigService;
	}

	public AiMaintainerService getNacosAiMaintainerService() {
		return nacosAiMaintainerService;
	}

	public void setNacosAiMaintainerService(AiMaintainerService nacosAiMaintainerService) {
		this.nacosAiMaintainerService = nacosAiMaintainerService;
	}

	public NacosMcpOperationService getMcpOperationService() {
		return mcpOperationService;
	}

	public void setMcpOperationService(NacosMcpOperationService mcpOperationService) {
		this.mcpOperationService = mcpOperationService;
	}

	public ObservationConfiguration getObservationConfiguration() {
		return observationConfiguration;
	}

	public void setObservationConfiguration(ObservationConfiguration observationConfiguration) {
		this.observationConfiguration = observationConfiguration;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getMcpNamespace() {
		return mcpNamespace;
	}

	public void setMcpNamespace(String mcpNamespace) {
		this.mcpNamespace = mcpNamespace;
	}

	public Duration getMcpGatewayToolTimeout() {
		return mcpGatewayToolTimeout;
	}

	public void setMcpGatewayToolTimeout(Duration mcpGatewayToolTimeout) {
		this.mcpGatewayToolTimeout = mcpGatewayToolTimeout;
	}

}
