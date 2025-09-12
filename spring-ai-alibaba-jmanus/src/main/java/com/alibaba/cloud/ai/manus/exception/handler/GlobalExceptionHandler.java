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
package com.alibaba.cloud.ai.manus.exception.handler;

import com.alibaba.cloud.ai.manus.exception.PlanException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author dahua
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Handle plan exceptions
	 */
	@ExceptionHandler(PlanException.class)
	public ResponseEntity handlePlanException(PlanException ex) {
		return ResponseEntity.internalServerError().body(ex.getMessage());
	}

	/**
	 * Handle all uncaught exceptions
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity handleGlobalException(Exception ex) {
		return ResponseEntity.internalServerError().body(ex.getMessage());
	}

}
