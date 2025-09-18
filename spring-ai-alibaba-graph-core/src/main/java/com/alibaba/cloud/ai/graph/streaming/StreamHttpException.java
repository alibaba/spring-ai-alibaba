/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.streaming;

/**
 * StreamHttpNode专用异常类 提供更详细的流式HTTP处理异常信息
 */
public class StreamHttpException extends RuntimeException {

	private final String nodeId;

	private final int httpStatus;

	private final String url;

	public StreamHttpException(String nodeId, String url, String message) {
		this(nodeId, url, -1, message, null);
	}

	public StreamHttpException(String nodeId, String url, String message, Throwable cause) {
		this(nodeId, url, -1, message, cause);
	}

	public StreamHttpException(String nodeId, String url, int httpStatus, String message, Throwable cause) {
		super(String.format("StreamHttpNode[%s] failed: %s (URL: %s, Status: %d)", nodeId, message, url, httpStatus),
				cause);
		this.nodeId = nodeId;
		this.httpStatus = httpStatus;
		this.url = url;
	}

	public String getNodeId() {
		return nodeId;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * 创建网络异常
	 */
	public static StreamHttpException networkError(String nodeId, String url, Throwable cause) {
		return new StreamHttpException(nodeId, url, "Network connection failed", cause);
	}

	/**
	 * 创建HTTP状态异常
	 */
	public static StreamHttpException httpError(String nodeId, String url, int status, String message) {
		return new StreamHttpException(nodeId, url, status, "HTTP error: " + message, null);
	}

	/**
	 * 创建数据解析异常
	 */
	public static StreamHttpException parseError(String nodeId, String url, String message, Throwable cause) {
		return new StreamHttpException(nodeId, url, "Data parsing failed: " + message, cause);
	}

	/**
	 * 创建超时异常
	 */
	public static StreamHttpException timeoutError(String nodeId, String url, String message) {
		return new StreamHttpException(nodeId, url, "Request timeout: " + message, null);
	}

}
