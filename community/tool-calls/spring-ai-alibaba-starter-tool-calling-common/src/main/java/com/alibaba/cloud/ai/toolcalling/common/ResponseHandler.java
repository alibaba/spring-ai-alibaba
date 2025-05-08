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

import org.slf4j.Logger;

import java.util.function.Function;

/**
 * Utility class for handling service responses
 */
public class ResponseHandler {

	private ResponseHandler() {
	}

	/**
	 * Handle service response with error handling and logging
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

}