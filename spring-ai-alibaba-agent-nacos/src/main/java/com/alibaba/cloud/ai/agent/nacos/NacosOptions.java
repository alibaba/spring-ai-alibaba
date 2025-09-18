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

	private ObservationConfigration observationConfigration;

	private String agentName;

	private String mcpNamespace;

	public NacosOptions(Properties properties) throws NacosException {
		nacosConfigService = new NacosConfigService(properties);
		nacosAiMaintainerService = new NacosAiMaintainerServiceImpl(properties);
		mcpOperationService = new NacosMcpOperationService(properties);
		agentName = properties.getProperty("agentName");
		mcpNamespace = properties.getProperty("mcpNamespace", properties.getProperty("namespace"));

	}

}
