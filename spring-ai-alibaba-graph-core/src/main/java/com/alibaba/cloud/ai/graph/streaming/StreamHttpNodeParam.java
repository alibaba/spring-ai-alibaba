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

import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class StreamHttpNodeParam {

	private WebClient webClient = WebClient.create();

	private HttpMethod method = HttpMethod.GET;

	private String url;

	private Map<String, String> headers = new HashMap<>();

	private Map<String, String> queryParams = new HashMap<>();

	private HttpRequestNodeBody body = new HttpRequestNodeBody();

	private AuthConfig authConfig;

	private RetryConfig retryConfig = new RetryConfig(3, 1000, true);

	private String outputKey;

	// 流式处理特有的配置
	private StreamFormat streamFormat = StreamFormat.SSE;

	// 性能和安全配置
	private long maxResponseSize = 50 * 1024 * 1024; // 50MB限制

	private int maxRedirects = 5; // 重定向次数限制

	private boolean allowInternalAddress = false; // 是否允许访问内网地址

	private Duration bufferTimeout = Duration.ofMillis(100); // 缓冲超时时间

	private StreamMode streamMode = StreamMode.DISTRIBUTE;

	private Duration readTimeout = Duration.ofMinutes(5);

	private int bufferSize = 8192;

	private String delimiter = "\n";

	/**
	 * 流格式枚举
	 */
	public enum StreamFormat {

		/**
		 * Server-Sent Events格式
		 */
		SSE,
		/**
		 * JSON Lines格式 (每行一个JSON对象)
		 */
		JSON_LINES,
		/**
		 * 纯文本流，按分隔符分割
		 */
		TEXT_STREAM

	}

	/**
	 * 流处理模式枚举
	 */
	public enum StreamMode {

		/**
		 * 分发模式：流中的每个元素都触发下游节点执行
		 */
		DISTRIBUTE,
		/**
		 * 聚合模式：收集完整流后再执行下游节点
		 */
		AGGREGATE

	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final StreamHttpNodeParam param = new StreamHttpNodeParam();

		public Builder webClient(WebClient webClient) {
			param.webClient = webClient;
			return this;
		}

		public Builder method(HttpMethod method) {
			param.method = method;
			return this;
		}

		public Builder url(String url) {
			param.url = url;
			return this;
		}

		public Builder header(String name, String value) {
			param.headers.put(name, value);
			return this;
		}

		public Builder headers(Map<String, String> headers) {
			param.headers.putAll(headers);
			return this;
		}

		public Builder queryParam(String name, String value) {
			param.queryParams.put(name, value);
			return this;
		}

		public Builder queryParams(Map<String, String> queryParams) {
			param.queryParams.putAll(queryParams);
			return this;
		}

		public Builder body(HttpRequestNodeBody body) {
			param.body = body;
			return this;
		}

		public Builder auth(AuthConfig authConfig) {
			param.authConfig = authConfig;
			return this;
		}

		public Builder retryConfig(RetryConfig retryConfig) {
			param.retryConfig = retryConfig;
			return this;
		}

		public Builder outputKey(String outputKey) {
			param.outputKey = outputKey;
			return this;
		}

		public Builder streamFormat(StreamFormat streamFormat) {
			param.streamFormat = streamFormat;
			return this;
		}

		public Builder streamMode(StreamMode streamMode) {
			param.streamMode = streamMode;
			return this;
		}

		public Builder readTimeout(Duration readTimeout) {
			param.readTimeout = readTimeout;
			return this;
		}

		public Builder bufferSize(int bufferSize) {
			param.bufferSize = bufferSize;
			return this;
		}

		public Builder delimiter(String delimiter) {
			param.delimiter = delimiter;
			return this;
		}

		public Builder allowInternalAddress(boolean allowInternalAddress) {
			param.allowInternalAddress = allowInternalAddress;
			return this;
		}

		public Builder bufferTimeout(Duration bufferTimeout) {
			param.bufferTimeout = bufferTimeout;
			return this;
		}

		public Builder maxResponseSize(long maxResponseSize) {
			param.maxResponseSize = maxResponseSize;
			return this;
		}

		public Builder maxRedirects(int maxRedirects) {
			param.maxRedirects = maxRedirects;
			return this;
		}

		public StreamHttpNodeParam build() {
			if (param.url == null || param.url.trim().isEmpty()) {
				throw new IllegalArgumentException("URL cannot be null or empty");
			}
			return param;
		}

	}

	// Getters
	public WebClient getWebClient() {
		return webClient;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public HttpRequestNodeBody getBody() {
		return body;
	}

	public AuthConfig getAuthConfig() {
		return authConfig;
	}

	public RetryConfig getRetryConfig() {
		return retryConfig;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public StreamFormat getStreamFormat() {
		return streamFormat;
	}

	public StreamMode getStreamMode() {
		return streamMode;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public long getMaxResponseSize() {
		return maxResponseSize;
	}

	public void setMaxResponseSize(long maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	public int getMaxRedirects() {
		return maxRedirects;
	}

	public void setMaxRedirects(int maxRedirects) {
		this.maxRedirects = maxRedirects;
	}

	public boolean isAllowInternalAddress() {
		return allowInternalAddress;
	}

	public void setAllowInternalAddress(boolean allowInternalAddress) {
		this.allowInternalAddress = allowInternalAddress;
	}

	public Duration getBufferTimeout() {
		return bufferTimeout;
	}

	public void setBufferTimeout(Duration bufferTimeout) {
		this.bufferTimeout = bufferTimeout;
	}

}
