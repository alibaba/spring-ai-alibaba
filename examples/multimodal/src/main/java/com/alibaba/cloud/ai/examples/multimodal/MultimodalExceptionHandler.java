/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Global exception handler for multimodal API. Returns JSON error body so the
 * frontend can show clear messages (e.g. 413 upload size exceeded).
 */
@RestControllerAdvice
public class MultimodalExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
		String message = "上传文件过大，超过服务器允许的最大大小。请缩小图片或联系管理员调整 spring.servlet.multipart.max-file-size 配置。";
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(Map.of("error", message));
	}

	@ExceptionHandler(GraphRunnerException.class)
	public ResponseEntity<Map<String, String>> handleGraphRunnerException(GraphRunnerException e) {
		String message = e.getMessage() != null ? e.getMessage() : "Agent 执行失败";
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", message));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
		String message = e.getMessage() != null ? e.getMessage() : "请求参数错误";
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}
}
