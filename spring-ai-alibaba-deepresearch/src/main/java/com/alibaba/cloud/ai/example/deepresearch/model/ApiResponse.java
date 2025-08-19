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

package com.alibaba.cloud.ai.example.deepresearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T>(

		@JsonProperty("code") Integer code,

		@JsonProperty("status") String status,

		@JsonProperty("message") String message,

		@JsonProperty("data") T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "success", "", data);
	}

	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(500, "error", message, null);
	}

	public static <T> ApiResponse<T> error(String message, T data) {
		return new ApiResponse<>(500, "error", message, data);
	}
}
