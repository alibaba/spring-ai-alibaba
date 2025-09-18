/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.manus.coordinator.tool;

import com.alibaba.cloud.ai.manus.tool.code.IpUtils;

public class EndPointUtils {

	public static String ENDPOINT_PREFIX = "/mcp";

	public static Integer SERVICE_PORT = 20881;

	public static String SERVICE_HOST = IpUtils.getLocalIp();

	public static String getUrl(String endpoint) {
		String builtEndpoint = buildMessageEndpoint(endpoint);
		return "http://" + SERVICE_HOST + ":" + SERVICE_PORT + builtEndpoint;
	}

	/**
	 * Build messageEndpoint, add default prefix /mcp
	 * @param endpoint Original endpoint address
	 * @return Endpoint address with prefix
	 */
	public static String buildMessageEndpoint(String endpoint) {
		if (endpoint == null || endpoint.trim().isEmpty()) {
			return ENDPOINT_PREFIX;
		}

		String trimmedEndpoint = endpoint.trim();

		// If endpoint already starts with /, directly concatenate /mcp
		if (trimmedEndpoint.startsWith("/")) {
			return ENDPOINT_PREFIX + trimmedEndpoint;
		}
		else {
			// If endpoint doesn't start with /, add /
			return ENDPOINT_PREFIX + "/" + trimmedEndpoint;
		}
	}

}
