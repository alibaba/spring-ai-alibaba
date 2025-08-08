package com.alibaba.cloud.ai.example.manus.coordinator.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.IpUtils;

public class EndPointUtils {

	public static String ENDPOINT_PREFIX = "/mcp";

	public static Integer SERVICE_PORT = 20881;

	public static String SERVICE_HOST = IpUtils.getLocalIp();

	public static String getUrl(String endpoint) {
		String builtEndpoint = buildMessageEndpoint(endpoint);
		return "http://" + SERVICE_HOST + ":" + SERVICE_PORT + builtEndpoint;
	}

	/**
	 * 构建messageEndpoint，增加默认前缀/mcp
	 * @param endpoint 原始端点地址
	 * @return 带前缀的端点地址
	 */
	public static String buildMessageEndpoint(String endpoint) {
		if (endpoint == null || endpoint.trim().isEmpty()) {
			return ENDPOINT_PREFIX;
		}

		String trimmedEndpoint = endpoint.trim();

		// 如果endpoint已经以/开头，则直接拼接/mcp
		if (trimmedEndpoint.startsWith("/")) {
			return ENDPOINT_PREFIX + trimmedEndpoint;
		}
		else {
			// 如果endpoint不以/开头，则添加/
			return ENDPOINT_PREFIX + "/" + trimmedEndpoint;
		}
	}

}
