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

package com.alibaba.cloud.ai.mcp.dynamic.server.callback;

import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.dynamic.server.utils.SpringBeanUtils;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public class DynamicNacosToolCallbackV3 implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(DynamicNacosToolCallbackV3.class);

	private final ToolDefinition toolDefinition;

	private final WebClient webClient;

	private final NamingService namingService;

	public DynamicNacosToolCallbackV3(final ToolDefinition toolDefinition) {
		this.toolDefinition = toolDefinition;
		this.webClient = SpringBeanUtils.getInstance().getBean(WebClient.class);
		this.namingService = SpringBeanUtils.getInstance().getBean(NamingService.class);
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public String call(@NonNull final String input) {
		return call(input, new ToolContext(Maps.newHashMap()));
	}

	@Override
	public String call(@NonNull final String input, final ToolContext toolContext) {
		try {
			logger.info("Tool callback: {} input: {}, toolContext: {}", toolDefinition.name(), input,
					JacksonUtils.toJson(toolContext));
			DynamicNacosToolDefinitionV3 nacosToolDefinition = (DynamicNacosToolDefinitionV3) this.toolDefinition;
			logger.info("Tool callback toolDefinition: {}", JacksonUtils.toJson(nacosToolDefinition));
			Object remoteServerConfig = nacosToolDefinition.getRemoteServerConfig();
			String protocol = nacosToolDefinition.getProtocol();
			if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
				Map<String, Object> configMap = (Map<String, Object>) remoteServerConfig;
				Object serviceRef = configMap.get("serviceRef");
				Object exportPath = configMap.get("exportPath");
				if (serviceRef != null) {
					Map<String, Object> refMap = (Map<String, Object>) serviceRef;
					String serviceName = (String) refMap.get("serviceName");
					String groupName = (String) refMap.get("groupName");
					Instance instance = namingService.selectOneHealthyInstance(serviceName, groupName);
					logger.info("Tool callback instance: {}", JacksonUtils.toJson(instance));
//					String url = "http://" + instance.getIp() + ":" + instance.getPort() + exportPath;
//					logger.info("Tool callback url: {}", url);
					// if
					// (nacosToolDefinition.getRequestMethod().equalsIgnoreCase("POST")) {
					// return
					// webClient.post().uri(url).bodyValue(input).retrieve().bodyToMono(String.class).block();
					// }
//					return webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
					return null;
				}
			}

			return "";
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}