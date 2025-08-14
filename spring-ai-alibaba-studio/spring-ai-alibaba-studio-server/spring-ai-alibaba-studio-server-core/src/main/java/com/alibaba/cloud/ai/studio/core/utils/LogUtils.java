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

package com.alibaba.cloud.ai.studio.core.utils;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.agent.AgentContext;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Utility class for logging operations. Provides methods for different log levels and
 * specialized logging for monitoring, tracing, and statistics.
 *
 * @since 1.0.0.3
 */
public class LogUtils {

	// Logger for general application logs
	private final static Logger logger = LoggerFactory.getLogger("application");

	// Logger for monitoring logs
	private final static Logger monitorLogger = LoggerFactory.getLogger("monitor-log");

	// Logger for statistics logs
	private final static Logger staticsLogger = LoggerFactory.getLogger("statics-log");

	// Logger for trace logs
	private final static Logger traceLogger = LoggerFactory.getLogger("trace-log");

	// Separator used in log messages
	public static final String SIMPLE_LOG_SPLIT = "@@@";

	// Status constants
	public static final String SUCCESS = "success";

	public static final String FAIL = "fail";

	/**
	 * Logs debug level message
	 * @param objects Parameters to be logged, concatenated with "@@@"
	 */
	public static void debug(Object... objects) {
		LogContent build = build(objects);
		logger.debug(build.logData);
	}

	/**
	 * Logs info level message with trace ID
	 */
	public static void info(Object... objects) {
		LogContent build = build(objects);
		logger.info("{}@@@{}", getTraceId(), build.logData);
	}

	/**
	 * Logs warning level message with trace ID
	 */
	public static void warn(Object... objects) {
		LogContent build = build(objects);
		logger.warn("{}@@@{}", getTraceId(), build.logData);
	}

	/**
	 * Logs error level message with trace ID and throwable
	 */
	public static void error(Object... objects) {
		LogContent build = build(objects);
		logger.error("{}@@@{}", getTraceId(), build.logData, build.getThrowable());
	}

	/**
	 * Logs monitoring information for a service action
	 */
	public static void monitor(String service, String action, Long start, String errorCode, Object input, Object output,
			Object... objects) {
		monitor(null, service, action, start, errorCode, input, output, objects);
	}

	/**
	 * Logs detailed monitoring information with request context
	 */
	public static void monitor(RequestContext context, String service, String action, Long starTime, String errorCode,
			Object input, Object output, Object... objects) {
		String status = StringUtils.isNotBlank(errorCode) && !errorCode.equalsIgnoreCase(SUCCESS) ? FAIL : SUCCESS;

		Long costTime = starTime == null || starTime == 0 ? 0 : System.currentTimeMillis() - starTime;
		StringBuilder builder = new StringBuilder();
		builder.append(getTraceId());

		builder.append(SIMPLE_LOG_SPLIT).append(service);
		builder.append(SIMPLE_LOG_SPLIT).append(action);
		builder.append(SIMPLE_LOG_SPLIT).append(status);
		builder.append(SIMPLE_LOG_SPLIT).append(costTime);
		builder.append(SIMPLE_LOG_SPLIT).append(errorCode);

		if (context != null) {
			builder.append(SIMPLE_LOG_SPLIT).append(context.getRequestId());
			builder.append(SIMPLE_LOG_SPLIT).append(context.getAccountId());

			if (context instanceof AgentContext agentContext) {
				builder.append(SIMPLE_LOG_SPLIT).append(agentContext.getConversationId());
				builder.append(SIMPLE_LOG_SPLIT).append(agentContext.getAppId());
			}
			else {
				builder.append(SIMPLE_LOG_SPLIT).append("");
				builder.append(SIMPLE_LOG_SPLIT).append("");
			}
		}
		else {
			builder.append(SIMPLE_LOG_SPLIT).append("");
			builder.append(SIMPLE_LOG_SPLIT).append("");
			builder.append(SIMPLE_LOG_SPLIT).append("");
		}

		Throwable throwable = null;
		if (Objects.nonNull(objects)) {
			for (Object obj : objects) {
				if (obj instanceof Throwable) {
					throwable = (Throwable) obj;
					continue;
				}

				String value;
				if (obj instanceof String) {
					value = String.valueOf(obj);
				}
				else {
					value = JsonUtils.toJson(obj);
				}

				builder.append(SIMPLE_LOG_SPLIT).append(value);
			}
		}

		String logStr = builder.toString();
		monitorLogger.info(logStr);
		if (Objects.nonNull(throwable)) {
			logger.error(logStr, throwable);
		}

		if (context != null) {
			trace(context, action, errorCode, starTime, input, output, objects);
		}
	}

	/**
	 * Logs trace information for request tracking
	 */
	public static void trace(RequestContext context, String action, String resultCode, Long startTime, Object input,
			Object output, Object... objects) {
		Long costTime = startTime == null || startTime == 0 ? 0 : System.currentTimeMillis() - startTime;

		StringBuilder trace = new StringBuilder();
		trace.append("trace").append(SIMPLE_LOG_SPLIT);
		trace.append(context.getRequestId()).append(SIMPLE_LOG_SPLIT);
		trace.append(context.getWorkspaceId()).append(SIMPLE_LOG_SPLIT);
		trace.append(context.getAccountId()).append(SIMPLE_LOG_SPLIT);

		trace.append(action).append(SIMPLE_LOG_SPLIT);
		trace.append(resultCode).append(SIMPLE_LOG_SPLIT);
		trace.append(costTime).append(SIMPLE_LOG_SPLIT);
		trace.append(JsonUtils.toJson(input)).append(SIMPLE_LOG_SPLIT);
		trace.append(JsonUtils.toJson(output)).append(SIMPLE_LOG_SPLIT);
		trace.append(context.getSource()).append(SIMPLE_LOG_SPLIT);

		if (context instanceof AgentContext agentContext) {
			trace.append(agentContext.getConversationId()).append(SIMPLE_LOG_SPLIT);
			trace.append(agentContext.getAppId()).append(SIMPLE_LOG_SPLIT);
		}
		else {
			trace.append("").append(SIMPLE_LOG_SPLIT);
			trace.append("").append(SIMPLE_LOG_SPLIT);
		}

		for (Object obj : objects) {
			String value;
			if (obj instanceof String) {
				value = String.valueOf(obj);
			}
			else if (obj instanceof Throwable err) {
				value = err.getMessage();
			}
			else {
				value = JsonUtils.toJson(obj);
			}

			trace.append(value).append(SIMPLE_LOG_SPLIT);
		}

		traceLogger.info(trace.toString());
	}

	/**
	 * Logs statistics for agent context
	 */
	public static void statistics(AgentContext context, boolean success) {
		StringBuilder builder = new StringBuilder();

		builder.append(context.getRequestId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getAccountId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getWorkspaceId());

		builder.append(SIMPLE_LOG_SPLIT).append(context.getAppId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getModel());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getConversationId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getSource());

		// static success or error
		builder.append(SIMPLE_LOG_SPLIT).append(success);
		builder.append(SIMPLE_LOG_SPLIT).append(context.getStartTime());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getEndTime());
		builder.append(SIMPLE_LOG_SPLIT).append(0);
		builder.append(SIMPLE_LOG_SPLIT).append(0);
		staticsLogger.error(builder.toString());
	}

	/**
	 * Logs statistics for workflow context
	 */
	public static void statistics(WorkflowContext context, boolean success) {
		StringBuilder builder = new StringBuilder();

		builder.append(context.getRequestId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getAccountId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getWorkspaceId());

		builder.append(SIMPLE_LOG_SPLIT).append(context.getAppId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getTaskResult());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getConversationId());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getSource());

		// static success or error
		builder.append(SIMPLE_LOG_SPLIT).append(success);
		builder.append(SIMPLE_LOG_SPLIT).append(context.getStartTime());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getEndTime());
		builder.append(SIMPLE_LOG_SPLIT).append(context.getTaskStatus());
		builder.append(SIMPLE_LOG_SPLIT).append(0);
		staticsLogger.error(builder.toString());
	}

	/**
	 * Builds log content from objects
	 * @return LogContent containing formatted log data and throwable if present
	 */
	private static LogContent build(Object... objects) {
		if (objects == null || objects.length == 0) {
			return null;
		}

		try {
			StringBuilder builder = new StringBuilder();

			Throwable throwable = null;
			for (Object obj : objects) {
				if (obj instanceof Throwable) {
					throwable = (Throwable) obj;
					continue;
				}

				String value;
				if (obj instanceof String) {
					value = String.valueOf(obj);
				}
				else {
					value = JsonUtils.toJson(obj);
				}

				builder.append(SIMPLE_LOG_SPLIT).append(value);
			}

			LogContent logContent = new LogContent();
			logContent.setLogData(builder.toString());
			logContent.setThrowable(throwable);

			return logContent;
		}
		catch (Exception exception) {
			logger.error("build log error, ", exception);
			return null;
		}
	}

	/**
	 * Internal class to hold log content and throwable
	 */
	@Data
	static class LogContent {

		String logData;

		Throwable throwable;

	}

	/**
	 * Generates a unique trace ID
	 */
	private static String getTraceId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
