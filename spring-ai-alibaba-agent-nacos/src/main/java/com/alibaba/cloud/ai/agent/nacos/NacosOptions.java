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

import java.util.Properties;

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import com.alibaba.nacos.maintainer.client.ai.NacosAiMaintainerServiceImpl;
import lombok.Data;

@Data
public class NacosOptions {

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

}
