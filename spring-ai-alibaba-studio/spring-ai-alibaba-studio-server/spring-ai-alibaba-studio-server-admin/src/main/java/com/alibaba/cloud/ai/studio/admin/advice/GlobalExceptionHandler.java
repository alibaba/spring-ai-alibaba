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

package com.alibaba.cloud.ai.studio.admin.advice;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.admin.utils.HttpStreamUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;

/**
 * Global exception handler for the application. Handles various types of exceptions and
 * converts them into standardized error responses.
 *
 * @since 1.0.0.3
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles all exceptions and converts them to appropriate error responses. Maps
	 * different exception types to corresponding error codes and messages.
	 * @param request HTTP request
	 * @param response HTTP response
	 * @param ex The exception to handle
	 * @throws Exception If error response cannot be written
	 */
	@ExceptionHandler(value = Exception.class)
	public void exceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws Exception {
		long start = System.currentTimeMillis();
		Error error;
		if (ex instanceof AsyncRequestTimeoutException) {
			error = ErrorCode.REQUEST_TIMEOUT.toError();
		}
		else if (ex instanceof BizException be) {
			error = be.getError();
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			error = ErrorCode.INVALID_JSON.toError();
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			error = ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.toError();
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			error = ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.toError();
		}
		else if (ex instanceof HttpRequestMethodNotSupportedException) {
			error = ErrorCode.REQUEST_METHOD_NOT_SUPPORTED.toError();
		}
		else if (ex instanceof MethodArgumentNotValidException me) {
			String message = buildValidationErrorMessage(me);
			error = ErrorCode.INVALID_PARAMS.toError("", message);
		}
		else if (ex instanceof ConstraintViolationException ce) {
			String message = buildValidationErrorMessage(ce);
			error = ErrorCode.INVALID_PARAMS.toError("", message);
		}
		else if (ex instanceof NoResourceFoundException) {
			throw ex;
		}
		else {
			error = ErrorCode.SYSTEM_ERROR.toError();
		}

		try {
			response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
			RequestContext context = RequestContextHolder.getRequestContext();

			String requestId = context == null ? "" : context.getRequestId();
			String json = JsonUtils.toJson(Result.error(requestId, error));

			Map<String, String> headers = HttpStreamUtils.getHeadersFromRequest(request);
			String body = HttpStreamUtils.getBodyFromRequest(request);
			LogUtils.monitor(context, "ExceptionHandler", "handleError", start, FAIL, body, json, headers, ex);

			response.setStatus(error.getStatusCode());
			response.getWriter().write(json);
			response.getWriter().flush();
		}
		catch (IOException e) {
			LogUtils.error("failed to write error response, err: {}", e.getMessage(), e);
		}
	}

	/**
	 * Builds validation error message from MethodArgumentNotValidException. Concatenates
	 * field names and their error messages.
	 * @param ex The validation exception
	 * @return Formatted error message string
	 */
	private String buildValidationErrorMessage(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors();
		if (CollectionUtils.isEmpty(errors)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (var err : errors) {
			sb.append(err.getField()).append(": ").append(err.getDefaultMessage());

			i++;
			if (i < errors.size()) {
				sb.append(", ");
			}
		}

		return sb.toString();
	}

	/**
	 * Builds validation error message from ConstraintViolationException. Concatenates
	 * property paths and their error messages.
	 * @param ex The constraint violation exception
	 * @return Formatted error message string
	 */
	private String buildValidationErrorMessage(ConstraintViolationException ex) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (var err : ex.getConstraintViolations()) {
			sb.append(err.getPropertyPath()).append(": ").append(err.getMessage());

			i++;
			if (i < ex.getConstraintViolations().size()) {
				sb.append(", ");
			}
		}

		return sb.toString();
	}

}
