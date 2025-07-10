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
 * 异常处理工具类
 *
 * @author zhangshenghang
 */
public class ExceptionUtils {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionUtils.class);

	/**
	 * 安全执行操作，捕获并记录异常
	 * @param operation 要执行的操作
	 * @param defaultValue 异常时的默认值
	 * @param errorMessage 错误消息
	 * @return 操作结果或默认值
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
	 * 安全执行操作，捕获并记录异常
	 * @param operation 要执行的操作
	 * @param errorMessage 错误消息
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
	 * 包装异常为运行时异常
	 * @param exception 原始异常
	 * @param message 自定义消息
	 * @return 运行时异常
	 */
	public static RuntimeException wrapAsRuntimeException(Exception exception, String message) {
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		}
		return new RuntimeException(message, exception);
	}

	/**
	 * 获取异常的根本原因
	 * @param exception 异常
	 * @return 根本原因
	 */
	public static Throwable getRootCause(Throwable exception) {
		Throwable cause = exception.getCause();
		if (cause == null) {
			return exception;
		}
		return getRootCause(cause);
	}

	/**
	 * 获取简化的异常消息
	 * @param exception 异常
	 * @return 简化的异常消息
	 */
	public static String getSimpleMessage(Throwable exception) {
		Throwable rootCause = getRootCause(exception);
		String message = rootCause.getMessage();
		return message != null ? message : rootCause.getClass().getSimpleName();
	}

}
