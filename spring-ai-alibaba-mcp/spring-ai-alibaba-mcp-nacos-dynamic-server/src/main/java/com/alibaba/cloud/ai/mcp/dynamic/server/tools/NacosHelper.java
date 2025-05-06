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

package com.alibaba.cloud.ai.mcp.dynamic.server.tools;

import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.exception.NacosException;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;

public class NacosHelper {

	private static final Logger logger = LoggerFactory.getLogger(NacosHelper.class);

	private NacosHelper() {
	}

	public static int compareVersion(String v1, String v2) {
		String[] arr1 = v1.split("\\.");
		String[] arr2 = v2.split("\\.");
		int len = Math.max(arr1.length, arr2.length);
		for (int i = 0; i < len; i++) {
			int n1 = i < arr1.length ? Integer.parseInt(arr1[i]) : 0;
			int n2 = i < arr2.length ? Integer.parseInt(arr2[i]) : 0;
			if (n1 != n2)
				return n1 - n2;
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public static String fetchNacosVersion(WebClient webClient, String serverAddr) {
		if (!serverAddr.startsWith("http://") && !serverAddr.startsWith("https://")) {
			serverAddr = "http://" + serverAddr;
		}
		try {
			String serverInfo = webClient.get()
				.uri(serverAddr + "/nacos/v1/console/server/state")
				.retrieve()
				.bodyToMono(String.class)
				.block();
			logger.info("Nacos server info: {}", serverInfo);
			Map<String, Object> serverInfoMap = JacksonUtils.toObj(serverInfo, Map.class);
			Object versionObj = serverInfoMap.get("version");
			return versionObj != null ? versionObj.toString() : null;
		}
		catch (WebClientResponseException webClientResponseException) {
			logger.error("Failed to get nacos server version", webClientResponseException);
			return "3.0.0";
		}
		catch (Exception e) {
			logger.warn("Failed to get or parse nacos server version", e);
			return null;
		}
	}

	public static List<String> listAllServices(NamingService namingService, String serviceGroup) throws NacosException {
		List<String> allServices = new ArrayList<>();
		int pageNo = 1, pageSize = 100, totalCount = 0;
		do {
			ListView<String> services = namingService.getServicesOfServer(pageNo, pageSize, serviceGroup);
			if (pageNo == 1)
				totalCount = services.getCount();
			allServices.addAll(services.getData());
			int startIndex = (pageNo - 1) * pageSize;
			if (startIndex + services.getData().size() >= totalCount)
				break;
			pageNo++;
		}
		while (true);
		return allServices;
	}

	public static boolean hasHealthyEnabledInstance(List<Instance> instances) {
		return instances != null
				&& instances.stream().anyMatch(instance -> instance.isHealthy() && instance.isEnabled());
	}

}