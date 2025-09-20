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

package com.alibaba.cloud.ai.manus.coordinator.exception;

import com.alibaba.cloud.ai.manus.coordinator.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * Global exception handler for coordinator tool operations
 */
@RestControllerAdvice
public class CoordinatorToolExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorToolExceptionHandler.class);

	/**
	 * Handle custom CoordinatorToolException
	 */
	@ExceptionHandler(CoordinatorToolException.class)
	public ResponseEntity<ErrorResponse> handleCoordinatorToolException(CoordinatorToolException e) {
		log.error("CoordinatorToolException: {}", e.getMessage(), e);

		ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getMessage());

		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * Handle validation exceptions
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		log.error("Validation error: {}", e.getMessage());

		String message = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.findFirst()
			.orElse("Validation failed");

		ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", message);

		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * Handle constraint violation exceptions
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
		log.error("Constraint violation: {}", e.getMessage());

		ErrorResponse errorResponse = new ErrorResponse("CONSTRAINT_VIOLATION", e.getMessage());

		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * Handle generic exceptions
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
		log.error("Unexpected error: {}", e.getMessage(), e);

		ErrorResponse errorResponse = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

}
