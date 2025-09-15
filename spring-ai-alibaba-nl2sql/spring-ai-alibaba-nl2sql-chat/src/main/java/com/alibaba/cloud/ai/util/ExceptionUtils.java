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

package com.alibaba.cloud.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Exception handling utility class
 *
 * @author zhangshenghang
 */
public class ExceptionUtils {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionUtils.class);

	/**
	 * Safely execute operation, catch and log exceptions
	 * @param operation operation to execute
	 * @param defaultValue default value when exception occurs
	 * @param errorMessage error message
	 * @return operation result or default value
	 */
	public static <T> T safeExecute(Supplier<T> operation, T defaultValue, String errorMessage) {
		try {
			return operation.get();
		}
		catch (Exception e) {
			logger.error("{}: {}", errorMessage, e.getMessage(), e);
			return defaultValue;
		}
	}

	/**
	 * Safely execute operation, catch and log exceptions
	 * @param operation operation to execute
	 * @param errorMessage error message
	 */
	public static void safeExecute(Runnable operation, String errorMessage) {
		try {
			operation.run();
		}
		catch (Exception e) {
			logger.error("{}: {}", errorMessage, e.getMessage(), e);
		}
	}

	/**
	 * Wrap exception as runtime exception
	 * @param exception original exception
	 * @param message custom message
	 * @return runtime exception
	 */
	public static RuntimeException wrapAsRuntimeException(Exception exception, String message) {
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		}
		return new RuntimeException(message, exception);
	}

	/**
	 * Get root cause of exception
	 * @param exception exception
	 * @return root cause
	 */
	public static Throwable getRootCause(Throwable exception) {
		Throwable cause = exception.getCause();
		if (cause == null) {
			return exception;
		}
		return getRootCause(cause);
	}

	/**
	 * Get simplified exception message
	 * @param exception exception
	 * @return simplified exception message
	 */
	public static String getSimpleMessage(Throwable exception) {
		Throwable rootCause = getRootCause(exception);
		String message = rootCause.getMessage();
		return message != null ? message : rootCause.getClass().getSimpleName();
	}

}
