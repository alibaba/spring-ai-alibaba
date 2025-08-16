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

package com.alibaba.cloud.ai.studio.admin.utils;

import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling HTTP request streams and headers.
 *
 * @since 1.0.0.3
 */
public class HttpStreamUtils {

	/**
	 * Extracts the request body as a string from the HTTP request.
	 * @param request The HTTP request
	 * @return The request body as a string, or null if extraction fails
	 */
	public static String getBodyFromRequest(HttpServletRequest request) {
		try {
			return StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			LogUtils.error("failed to get body from http request, err: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Extracts all headers from the HTTP request into a map.
	 * @param request The HTTP request
	 * @return Map containing all request headers
	 */
	public static Map<String, String> getHeadersFromRequest(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}

		return map;
	}

}
