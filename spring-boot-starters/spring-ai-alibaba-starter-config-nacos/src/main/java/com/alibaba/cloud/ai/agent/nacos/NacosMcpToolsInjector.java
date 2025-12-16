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

import java.util.Collections;
import java.util.List;

import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.ToolCallback;

public class NacosMcpToolsInjector {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpToolsInjector.class);

	public static McpServersVO getMcpServersVO(NacosOptions nacosOptions) {
		try {
			String dataId = (nacosOptions.isMcpServersEncrypted() ? "cipher-kms-aes-256-" : "") + "mcp-servers.json";
			String config = nacosOptions.getNacosConfigService()
					.getConfig(dataId, "ai-agent-" + nacosOptions.getAgentName(), 3000L);
			return JSON.parseObject(config, McpServersVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<ToolCallback> convert(NacosOptions nacosOptions, McpServersVO mcpServersVO) {

		NacosMcpGatewayToolsInitializer nacosMcpGatewayToolsInitializer = new NacosMcpGatewayToolsInitializer(
				nacosOptions.mcpOperationService, mcpServersVO.getMcpServers());
		return Collections.unmodifiableList(nacosMcpGatewayToolsInitializer.initializeTools());
	}

}
