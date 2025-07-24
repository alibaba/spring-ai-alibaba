/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.nacos;

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NacosMcpServiceDiscovery implements McpServiceDiscovery {

	@Autowired
	private NacosMcpOperationService nacosMcpOperationService;

	// 本地缓存：serviceName -> McpServerInfo
	private final Map<String, McpServerInfo> serviceCache = new ConcurrentHashMap<>();

	// 本地缓存：serviceName -> version/hash
	private final Map<String, String> serviceVersionCache = new ConcurrentHashMap<>();

	/**
	 * 获取并缓存指定serviceName的MCP服务信息
	 * @param serviceName 服务名
	 * @return McpServerInfo
	 */
	public McpServerInfo fetchAndCacheService(String serviceName) {
		try {
			McpServerDetailInfo detail = nacosMcpOperationService.getServerDetail(serviceName);
			if (detail == null)
				return null;
			String version = detail.getVersionDetail() != null ? detail.getVersionDetail().getVersion() : "";
			String cacheVersion = serviceVersionCache.get(serviceName);
			// 判断是否变更
			boolean changed = cacheVersion == null || !cacheVersion.equals(version);
			if (changed) {
				// 生成 embedding
				String name = detail.getName();
				String description = detail.getDescription();
				String protocol = detail.getProtocol();
				String endpoint = null;
				if (detail.getRemoteServerConfig() != null && detail.getRemoteServerConfig().getServiceRef() != null) {
					var ref = detail.getRemoteServerConfig().getServiceRef();
					String exportPath = detail.getRemoteServerConfig().getExportPath();
					endpoint = ref.getServiceName() + "@" + ref.getGroupName() + (exportPath != null ? exportPath : "");
				}
				List<String> tags = new ArrayList<>();
				if (description != null && !description.isEmpty()) {
					tags.addAll(Arrays.asList(description.split("[ ,;|]")));
				}
				McpServerInfo info = new McpServerInfo(name, description, protocol, version, endpoint, true, tags);
				serviceCache.put(serviceName, info);
				serviceVersionCache.put(serviceName, version);
				return info;
			}
			else {
				return serviceCache.get(serviceName);
			}
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public McpServerInfo getService(String serviceName) {
		return fetchAndCacheService(serviceName);
	}

	@Override
	public List<McpServerInfo> getAllServices() {
		// 这里需要实现获取所有服务的逻辑
		// 由于当前实现是基于单个服务获取，这里返回缓存中的所有服务
		return new ArrayList<>(serviceCache.values());
	}

	@Override
	public List<McpServerInfo> searchServices(String query, int limit) {
		// 简单的关键词搜索实现
		return serviceCache.values()
			.stream()
			.filter(s -> s.getName().contains(query)
					|| (s.getDescription() != null && s.getDescription().contains(query))
					|| (s.getTags() != null && s.getTags().stream().anyMatch(tag -> tag.contains(query))))
			.limit(limit)
			.toList();
	}

	@Override
	public boolean refreshService(String serviceName) {
		try {
			// 清除缓存，强制重新获取
			serviceCache.remove(serviceName);
			serviceVersionCache.remove(serviceName);
			return fetchAndCacheService(serviceName) != null;
		}
		catch (Exception e) {
			return false;
		}
	}

}
