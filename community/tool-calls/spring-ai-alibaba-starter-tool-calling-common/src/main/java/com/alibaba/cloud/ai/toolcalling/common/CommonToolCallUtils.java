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

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * @author vlsmb
 */
public final class CommonToolCallUtils {

	private CommonToolCallUtils() {
	}

	public static class MultiValueMapBuilder<K, V> {

		private MultiValueMap<K, V> params;

		private MultiValueMapBuilder() {
			params = new LinkedMultiValueMap<>();
		}

		public MultiValueMapBuilder<K, V> add(K key, V value) {
			params.add(key, value);
			return this;
		}

		public MultiValueMap<K, V> build() {
			return params;
		}

	}

	/**
	 * MultiValueMap builder
	 * @author vlsmb
	 * @return builder
	 * @param <K> Key Type
	 * @param <V> Value Type
	 */
	public static <K, V> MultiValueMapBuilder<K, V> multiValueMapBuilder() {
		return new MultiValueMapBuilder<>();
	}

	/**
	 * Common error handling method.
	 * @author inlines10
	 * @param serviceName Service name
	 * @param operation Operation to execute
	 * @param logger Logger instance
	 * @return Operation result
	 */
	public static <T> T handleServiceError(String serviceName, Supplier<T> operation, Logger logger) {
		try {
			return operation.get();
		}
		catch (Exception e) {
			logger.error("Failed to invoke {} service due to: {}", serviceName, e.getMessage());
			return null;
		}
	}

	/**
	 * Common parameter validation method.
	 * @author inlines10
	 * @param params Parameters to validate
	 * @return if params is invalid, return true
	 */
	public static boolean isInvalidateRequestParams(Object... params) {
		return !Arrays.stream(params)
			.allMatch(param -> param != null && (!(param instanceof String) || StringUtils.hasText((String) param)));
	}

	/**
	 * Handle service response with error handling and logging
	 * @author inlines10
	 * @param responseData Raw response data
	 * @param parser Function to parse the response
	 * @param logger Logger instance
	 * @return Parsed response or null if handling fails
	 */
	public static <T> T handleResponse(String responseData, Function<String, T> parser, Logger logger) {
		if (responseData == null) {
			logger.error("Response data is null");
			return null;
		}
		try {
			return parser.apply(responseData);
		}
		catch (Exception e) {
			logger.error("Failed to handle response: {}", e.getMessage());
			return null;
		}
	}

	private static final String URL_REGEX = "^(https?://)?"
			+ "((([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})|(([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})" + // 域名或
			// IP
			"(?!(\\.0\\.0\\.0|\\.0\\.0|\\.0|\\.1|\\.2|\\.3|\\.4|\\.5|\\.6|\\.7|\\.8|\\.9))" + // 排除
			// 127.0.0.1
			"))" + "(\\:[0-9]{1,5})?" + "(/.*)?$";

	private static final Pattern pattern = Pattern.compile(URL_REGEX);

	/**
	 * @author yuluo
	 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
	 */
	public static boolean isValidUrl(String target) {

		return pattern.matcher(target).matches();
	}

}
