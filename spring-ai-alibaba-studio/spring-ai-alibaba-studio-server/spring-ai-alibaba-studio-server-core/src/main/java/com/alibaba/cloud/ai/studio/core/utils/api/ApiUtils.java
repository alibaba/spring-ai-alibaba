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

package com.alibaba.cloud.ai.studio.core.utils.api;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * Utility class for API-related operations. Provides methods for generating user agent
 * strings and HTTP headers.
 *
 * @since 1.0.0.3
 */
public class ApiUtils {

	/** SDK identifier used in user agent string */
	private static final String SDK_FLAG = "agentscope";

	/**
	 * Generates a user agent string containing SDK version and system information.
	 * @return Formatted user agent string
	 */
	public static String userAgent() {
		return String.format("%s/%s; java/%s; platform/%s; processor/%s", SDK_FLAG, "1.0.0-M1",
				System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"));
	}

	/**
	 * Creates base HTTP headers with user agent information.
	 * @return MultiValueMap containing the base headers
	 */
	public static MultiValueMap<String, String> getBaseHeaders() {
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.USER_AGENT, userAgent());

		return headers;
	}

}
