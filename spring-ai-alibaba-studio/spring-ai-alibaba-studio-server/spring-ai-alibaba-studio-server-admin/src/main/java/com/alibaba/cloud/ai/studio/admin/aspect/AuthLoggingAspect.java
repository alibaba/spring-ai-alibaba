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

package com.alibaba.cloud.ai.studio.admin.aspect;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Authentication and logging aspect for web controllers. Provides request tracing,
 * monitoring, and logging functionality.
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
@Aspect
public class AuthLoggingAspect {

	/**
	 * Around advice for all controller methods. Intercepts and logs controller method
	 * executions.
	 */
	@Around("execution(* com.alibaba.cloud.ai.studio.admin.controller.*.*(..))")
	public Object consoleAround(ProceedingJoinPoint joinPoint) throws Throwable {
		return aroundService(joinPoint);
	}

	/**
	 * Core service method that handles request logging and monitoring. Tracks execution
	 * time, request context, and method parameters.
	 */
	public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		if (context == null) {
			context = new RequestContext();
			context.setRequestId(IdGenerator.uuid());
		}

		String className = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();
		Object[] filteredArgs = null;

		try {
			// TODO: Implement account login and workspace permission checks
			filteredArgs = filterNonSerializableObjects(joinPoint.getArgs());
			LogUtils.trace(context, methodName, SUCCESS, start, filteredArgs, null);
			Object result = joinPoint.proceed(joinPoint.getArgs());

			LogUtils.monitor(context, className, methodName, start, SUCCESS, filteredArgs, result);
			return result;
		}
		catch (Throwable e) {
			try {
				LogUtils.monitor(context, className, methodName, start, "exception", filteredArgs, null, e);
			}
			catch (Exception ex) {
				LogUtils.error(ex);
			}

			throw e;
		}
	}

	/**
	 * Filters out non-serializable objects from method arguments. Excludes
	 * HttpServletRequest, HttpServletResponse, and MultipartFile objects.
	 */
	private Object[] filterNonSerializableObjects(Object[] objects) {
		if (objects == null || objects.length == 0) {
			return objects;
		}

		List<Object> args = new ArrayList<>();
		for (Object object : objects) {
			if (object instanceof HttpServletRequest || object instanceof HttpServletResponse
					|| object instanceof MultipartFile[] || object instanceof MultipartFile) {
				continue;
			}

			args.add(object);
		}

		return args.toArray();
	}

}
