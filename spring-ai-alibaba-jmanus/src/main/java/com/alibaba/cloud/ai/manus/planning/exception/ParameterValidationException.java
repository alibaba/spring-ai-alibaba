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
package com.alibaba.cloud.ai.manus.planning.exception;

/**
 * Exception thrown when parameter validation fails in plan templates This exception
 * provides detailed information about missing or incompatible parameters to help users
 * understand and fix the problem
 */
public class ParameterValidationException extends RuntimeException {

	/**
	 * Constructs a new ParameterValidationException with the specified detail message
	 * @param message the detail message explaining the parameter validation failure
	 */
	public ParameterValidationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new ParameterValidationException with the specified detail message and
	 * cause
	 * @param message the detail message explaining the parameter validation failure
	 * @param cause the cause of the parameter validation failure
	 */
	public ParameterValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
