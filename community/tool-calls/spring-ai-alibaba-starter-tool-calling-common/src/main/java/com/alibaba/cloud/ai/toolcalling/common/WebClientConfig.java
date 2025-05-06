package com.alibaba.cloud.ai.toolcalling.common;

import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * WebClient configuration utilities
 */
public class WebClientConfig {

	public static final int DEFAULT_MEMORY_SIZE = 5;

	public static final int BYTE_SIZE = 1024;

	public static final int MAX_MEMORY_SIZE = DEFAULT_MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	private WebClientConfig() {
	}

	/**
	 * Create a default WebClient with standard configuration
	 * @param headers Custom headers to be added
	 * @return Configured WebClient instance
	 */
	public static WebClient createDefaultWebClient(Map<String, String> headers) {
		return CommonToolCallUtils.buildWebClient(headers, CommonToolCallConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS,
				CommonToolCallConstants.DEFAULT_RESPONSE_TIMEOUT_SECONDS, MAX_MEMORY_SIZE);
	}

}