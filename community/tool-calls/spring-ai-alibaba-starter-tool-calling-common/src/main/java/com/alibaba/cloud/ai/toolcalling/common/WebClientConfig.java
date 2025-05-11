/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
