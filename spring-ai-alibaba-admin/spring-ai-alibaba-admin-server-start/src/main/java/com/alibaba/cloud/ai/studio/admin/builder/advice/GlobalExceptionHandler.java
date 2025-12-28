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

package com.alibaba.cloud.ai.studio.admin.builder.advice;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.admin.builder.utils.HttpStreamUtils;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.exception.ExceptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
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
 * converts them into standardized error responses. Supports both BizException and
 * StudioException, as well as various validation and HTTP exceptions.
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
		else if (ex instanceof StudioException se) {
			// Handle StudioException - convert to Error format
			String message = ExceptionUtils.getAllExceptionMsg(se);
			int statusCode = se.getErrCode();
			// Determine error type based on status code
			String type = (statusCode >= 400 && statusCode < 500)
					? "invalid_request_error"
					: "response_error";
			error = Error.builder()
				.code(String.valueOf(statusCode))
				.message(message)
				.statusCode(statusCode)
				.type(type)
				.build();
		}
		else if (ex instanceof HttpMessageNotReadableException hmne) {
			// Enhanced error message extraction for JSON parsing errors
			String message = "请求参数格式错误，请检查JSON格式是否正确";
			if (hmne.getMessage() != null) {
				if (hmne.getMessage().contains("JSON parse error")) {
					message = "JSON格式错误，请检查参数格式";
				}
				else if (hmne.getMessage().contains("Required request body is missing")) {
					message = "请求体不能为空";
				}
			}
			// Create Error object directly with custom message
			ErrorCode invalidJson = ErrorCode.INVALID_JSON;
			error = Error.builder()
				.statusCode(invalidJson.getStatusCode())
				.type(invalidJson.getType())
				.code(invalidJson.getCode())
				.message(message)
				.build();
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
		else if (ex instanceof BindException be) {
			// Handle form binding validation errors
			String message = buildValidationErrorMessage(be);
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
	 * property paths and their error messages. Extracts field names from property paths.
	 * @param ex The constraint violation exception
	 * @return Formatted error message string
	 */
	private String buildValidationErrorMessage(ConstraintViolationException ex) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			String fieldName = getFieldName(violation);
			sb.append(fieldName).append(": ").append(violation.getMessage());

			i++;
			if (i < ex.getConstraintViolations().size()) {
				sb.append(", ");
			}
		}

		return sb.toString();
	}

	/**
	 * Builds validation error message from BindException. Concatenates field names and
	 * their error messages.
	 * @param ex The bind exception
	 * @return Formatted error message string
	 */
	private String buildValidationErrorMessage(BindException ex) {
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
	 * Extracts field name from constraint violation property path.
	 * @param violation The constraint violation
	 * @return The field name
	 */
	private String getFieldName(ConstraintViolation<?> violation) {
		String propertyPath = violation.getPropertyPath().toString();
		// Extract the last field name
		String[] parts = propertyPath.split("\\.");
		return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
	}

}
