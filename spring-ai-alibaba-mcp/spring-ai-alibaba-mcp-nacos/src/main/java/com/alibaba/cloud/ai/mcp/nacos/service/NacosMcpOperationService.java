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

package com.alibaba.cloud.ai.mcp.nacos.service;

import com.alibaba.cloud.ai.mcp.nacos.service.model.NacosMcpServerEndpoint;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointSpec;
import com.alibaba.nacos.api.ai.model.mcp.McpServerBasicInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerFactory;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Sunrisea
 */
public class NacosMcpOperationService {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpOperationService.class);

	private final AiMaintainerService aiMaintainerService;

	private final NamingService namingService;

	private final String namespace;

	private final Map<String, List<NacosMcpSubscriber>> subscribers;

	public NacosMcpOperationService(Properties nacosProperties) throws NacosException {
		this.aiMaintainerService = AiMaintainerFactory.createAiMaintainerService(nacosProperties);
		this.namingService = NacosFactory.createNamingService(nacosProperties);
		this.namespace = nacosProperties.getProperty(PropertyKeyConst.NAMESPACE, "public");
		this.subscribers = new ConcurrentHashMap<>();
		ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, r -> {
			Thread t = new Thread(r);
			t.setName("nacos-mcp-operation-service");
			t.setDaemon(true);
			return t;
		});

		executorService.scheduleWithFixedDelay(this::getServerChange, 30, 30, TimeUnit.SECONDS);
	}

	private void getServerChange() {
		for (Map.Entry<String, List<NacosMcpSubscriber>> entry : subscribers.entrySet()) {
			String mcpNameAndVersion = entry.getKey();
			List<NacosMcpSubscriber> nacosMcpSubscribers = entry.getValue();
			try {
				McpServerDetailInfo mcpServerDetailInfo = this.getServerDetail(mcpNameAndVersion);
				if (mcpServerDetailInfo == null) {
					continue;
				}
				for (NacosMcpSubscriber nacosMcpSubscriber : nacosMcpSubscribers) {
					nacosMcpSubscriber.receive(mcpServerDetailInfo);
				}
			}
			catch (Exception e) {
				logger.error("getServerChange error", e);
			}
		}
	}

	public NacosMcpServerEndpoint getServerEndpoint(String mcpNameAndVersion) throws NacosException {
		if (mcpNameAndVersion == null) {
			throw new IllegalArgumentException("mcpNameAndVersion must not be null");
		}
		McpServerDetailInfo mcpServerDetailInfo = this.getServerDetail(mcpNameAndVersion);
		if (mcpServerDetailInfo == null) {
			return null;
		}
		List<McpEndpointInfo> mcpEndpointInfoList = mcpServerDetailInfo.getBackendEndpoints() == null
				? new ArrayList<>() : mcpServerDetailInfo.getBackendEndpoints();
		String exportPath = mcpServerDetailInfo.getRemoteServerConfig().getExportPath();
		String protocol = mcpServerDetailInfo.getProtocol();
		String realVersion = mcpServerDetailInfo.getVersionDetail().getVersion();
		return new NacosMcpServerEndpoint(mcpEndpointInfoList, exportPath, protocol, realVersion);
	}

	public NacosMcpServerEndpoint getServerEndpoint(String mcpName, String version) throws NacosException {
		Assert.notNull(mcpName, "serviceName cannot be null");
		Assert.notNull(version, "version cannot be null");
		McpServerDetailInfo mcpServerDetailInfo = this.getServerDetail(mcpName, version);
		if (mcpServerDetailInfo == null) {
			return null;
		}
		List<McpEndpointInfo> mcpEndpointInfoList = mcpServerDetailInfo.getBackendEndpoints() == null
				? new ArrayList<>() : mcpServerDetailInfo.getBackendEndpoints();
		String exportPath = mcpServerDetailInfo.getRemoteServerConfig().getExportPath();
		String protocol = mcpServerDetailInfo.getProtocol();
		String realVersion = mcpServerDetailInfo.getVersionDetail().getVersion();
		return new NacosMcpServerEndpoint(mcpEndpointInfoList, exportPath, protocol, realVersion);
	}

	public McpServerDetailInfo getServerDetail(String mcpNameAndVersion) throws NacosException {
		if (mcpNameAndVersion == null) {
			throw new IllegalArgumentException("mcpNameAndVersion must not be null");
		}
		String[] nameAndVersion = mcpNameAndVersion.strip().split("::");
		String version = null;
		String mcpName = mcpNameAndVersion;
		if (nameAndVersion.length > 2) {
			throw new NacosException(NacosException.INVALID_PARAM, "mcpName is invalid");
		}
		if (nameAndVersion.length == 2) {
			version = nameAndVersion[1];
			mcpName = nameAndVersion[0];
		}
		return aiMaintainerService.getMcpServerDetail(this.namespace, mcpName, version);
	}

	public McpServerDetailInfo getServerDetail(String mcpName, String version) throws NacosException {
		if (mcpName == null || version == null) {
			throw new IllegalArgumentException("mcpName must not be null");
		}
		return aiMaintainerService.getMcpServerDetail(this.namespace, mcpName, version);
	}

	public void subscribeNacosMcpServer(String mcpNameAndVersion, NacosMcpSubscriber nacosMcpSubscriber) {
		if (mcpNameAndVersion == null || nacosMcpSubscriber == null) {
			throw new IllegalArgumentException("mcpNameAndVersion and nacosMcpSubscriber must not be null");
		}
		this.subscribers.computeIfAbsent(mcpNameAndVersion, k -> new ArrayList<>()).add(nacosMcpSubscriber);
	}

	public McpEndpointInfo selectEndpoint(McpServiceRef mcpServiceRef) throws NacosException {
		if (mcpServiceRef == null) {
			throw new IllegalArgumentException("mcpServiceRef must not be null");
		}
		Instance instance = namingService.selectOneHealthyInstance(mcpServiceRef.getServiceName(),
				mcpServiceRef.getGroupName());
		McpEndpointInfo mcpEndpointInfo = new McpEndpointInfo();
		mcpEndpointInfo.setAddress(instance.getIp());
		mcpEndpointInfo.setPort(instance.getPort());
		return mcpEndpointInfo;
	}

	public boolean createMcpServer(String mcpName, McpServerBasicInfo serverSpec, McpToolSpecification toolSpec,
			McpEndpointSpec endpointSpec) throws NacosException {
		endpointSpec.getData().put("namespaceId", this.namespace);
		return aiMaintainerService.createMcpServer(this.namespace, mcpName, serverSpec, toolSpec, endpointSpec);
	}

	public void registerService(String serviceName, String groupName, Instance instance) throws NacosException {
		this.namingService.registerInstance(serviceName, groupName, instance);
	}

}
