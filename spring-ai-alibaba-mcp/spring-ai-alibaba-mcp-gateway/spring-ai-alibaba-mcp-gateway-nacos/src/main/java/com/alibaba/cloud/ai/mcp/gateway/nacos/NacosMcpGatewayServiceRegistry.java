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

package com.alibaba.cloud.ai.mcp.gateway.nacos;

import com.alibaba.cloud.ai.mcp.gateway.core.McpEndpointInfo;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayServiceRegistry;
import com.alibaba.cloud.ai.mcp.gateway.core.McpServiceDetail;
import com.alibaba.cloud.ai.mcp.gateway.core.McpServiceDetail.McpToolMeta;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nacos MCP Gateway 服务注册实现
 */
public class NacosMcpGatewayServiceRegistry implements McpGatewayServiceRegistry {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayServiceRegistry.class);

	private final NacosMcpOperationService nacosMcpOperationService;

	private final List<ServiceChangeListener> listeners = new CopyOnWriteArrayList<>();

	public NacosMcpGatewayServiceRegistry(NacosMcpOperationService nacosMcpOperationService) {
		this.nacosMcpOperationService = nacosMcpOperationService;
	}

	@Override
	public McpServiceDetail getServiceDetail(String serviceName) {
		try {
			McpServerDetailInfo serverDetailInfo = nacosMcpOperationService.getServerDetail(serviceName);
			if (serverDetailInfo == null) {
				return null;
			}
			return convertToMcpServiceDetail(serverDetailInfo);
		}
		catch (Exception e) {
			logger.error("Failed to get service detail for: {}", serviceName, e);
			return null;
		}
	}

	@Override
	public List<String> getServiceNames() {
		// 这里需要根据实际配置返回服务名称列表
		// 可以通过配置文件或其他方式获取
		return new ArrayList<>();
	}

	@Override
	public McpEndpointInfo selectEndpoint(McpServiceDetail.McpServiceRef serviceRef) {
		try {
			// 转换为 Nacos 的 McpServiceRef
			com.alibaba.nacos.api.ai.model.mcp.McpServiceRef nacosServiceRef = new com.alibaba.nacos.api.ai.model.mcp.McpServiceRef();
			nacosServiceRef.setServiceName(serviceRef.getServiceName());
			nacosServiceRef.setNamespaceId(serviceRef.getNamespaceId());
			nacosServiceRef.setGroupName(serviceRef.getGroupName());

			com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo nacosEndpointInfo = nacosMcpOperationService
				.selectEndpoint(nacosServiceRef);
			if (nacosEndpointInfo == null) {
				return null;
			}
			return convertToMcpEndpointInfo(nacosEndpointInfo, serviceRef);
		}
		catch (NacosException e) {
			logger.error("Failed to select endpoint for service: {}", serviceRef.getServiceName(), e);
			return null;
		}
	}

	@Override
	public void registerServiceChangeListener(ServiceChangeListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeServiceChangeListener(ServiceChangeListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * 通知服务变更监听器
	 * @param serviceName 服务名称
	 * @param oldDetail 旧的服务详情
	 * @param newDetail 新的服务详情
	 */
	protected void notifyServiceChangeListeners(String serviceName, McpServiceDetail oldDetail,
			McpServiceDetail newDetail) {
		for (ServiceChangeListener listener : listeners) {
			try {
				listener.onServiceChanged(serviceName, oldDetail, newDetail);
			}
			catch (Exception e) {
				logger.error("Error notifying service change listener", e);
			}
		}
	}

	/**
	 * 转换 Nacos 服务详情为抽象服务详情
	 * @param serverDetailInfo Nacos 服务详情
	 * @return 抽象服务详情
	 */
	private McpServiceDetail convertToMcpServiceDetail(McpServerDetailInfo serverDetailInfo) {
		McpServiceDetail.McpServiceRef serviceRef = convertToMcpServiceRef(
				serverDetailInfo.getRemoteServerConfig().getServiceRef());
		McpServiceDetail.McpToolSpecification toolSpec = convertToMcpToolSpecification(serverDetailInfo.getToolSpec());
		McpServiceDetail.McpVersionDetail versionDetail = new McpServiceDetail.McpVersionDetail(
				serverDetailInfo.getVersionDetail().getVersion());

		return new McpServiceDetail(serverDetailInfo.getName(), serverDetailInfo.getProtocol(), serviceRef, toolSpec,
				versionDetail);
	}

	/**
	 * 转换 Nacos 服务引用为抽象服务引用
	 * @param nacosServiceRef Nacos 服务引用
	 * @return 抽象服务引用
	 */
	private McpServiceDetail.McpServiceRef convertToMcpServiceRef(McpServiceRef nacosServiceRef) {
		return new McpServiceDetail.McpServiceRef(nacosServiceRef.getServiceName(), nacosServiceRef.getNamespaceId(),
				nacosServiceRef.getGroupName());
	}

	/**
	 * 转换 Nacos 工具规格为抽象工具规格
	 * @param nacosToolSpec Nacos 工具规格
	 * @return 抽象工具规格
	 */
	private McpServiceDetail.McpToolSpecification convertToMcpToolSpecification(
			com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification nacosToolSpec) {
		if (nacosToolSpec == null) {
			return null;
		}

		List<McpServiceDetail.McpTool> tools = new ArrayList<>();
		if (nacosToolSpec.getTools() != null) {
			for (com.alibaba.nacos.api.ai.model.mcp.McpTool nacosTool : nacosToolSpec.getTools()) {
				McpServiceDetail.McpTool tool = new McpServiceDetail.McpTool(nacosTool.getName(),
						nacosTool.getDescription(), nacosTool.getInputSchema());
				tools.add(tool);
			}
		}

		Map<String, McpToolMeta> toolsMeta = new java.util.HashMap<>();
		if (nacosToolSpec.getToolsMeta() != null) {
			for (Map.Entry<String, com.alibaba.nacos.api.ai.model.mcp.McpToolMeta> entry : nacosToolSpec.getToolsMeta()
				.entrySet()) {
				com.alibaba.nacos.api.ai.model.mcp.McpToolMeta nacosMeta = entry.getValue();
				McpServiceDetail.McpToolMeta meta = new McpServiceDetail.McpToolMeta(nacosMeta.isEnabled(),
						nacosMeta.getTemplates());
				toolsMeta.put(entry.getKey(), meta);
			}
		}

		return new McpServiceDetail.McpToolSpecification(tools, toolsMeta);
	}

	/**
	 * 转换 Nacos 端点信息为抽象端点信息
	 * @param nacosEndpointInfo Nacos 端点信息
	 * @param serviceRef 服务引用信息
	 * @return 抽象端点信息
	 */
	private McpEndpointInfo convertToMcpEndpointInfo(
			com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo nacosEndpointInfo,
			McpServiceDetail.McpServiceRef serviceRef) {
		// Nacos 的 McpEndpointInfo 只有 address 和 port 方法
		// 其他字段需要从 serviceRef 中获取或使用默认值
		return new McpEndpointInfo(nacosEndpointInfo.getAddress(), nacosEndpointInfo.getPort(),
				serviceRef.getServiceName(), // serviceName - 从 serviceRef 中获取
				serviceRef.getNamespaceId(), // namespaceId - 从 serviceRef 中获取
				serviceRef.getGroupName(), // groupName - 从 serviceRef 中获取
				true); // healthy - 默认认为健康
	}

}
