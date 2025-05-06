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