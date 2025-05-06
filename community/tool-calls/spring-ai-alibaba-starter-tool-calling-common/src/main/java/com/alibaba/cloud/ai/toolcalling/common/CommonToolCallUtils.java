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
package com.alibaba.cloud.ai.toolcalling.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author vlsmb
 */
public final class CommonToolCallUtils {

	private CommonToolCallUtils() {

	}

	public static class MultiValueMapBuilder<K, V> {

		private MultiValueMap<K, V> params;

		private MultiValueMapBuilder() {
			params = new LinkedMultiValueMap<>();
		}

		public MultiValueMapBuilder<K, V> add(K key, V value) {
			params.add(key, value);
			return this;
		}

		public MultiValueMap<K, V> build() {
			return params;
		}

	}

	public static <K, V> MultiValueMapBuilder<K, V> multiValueMapBuilder() {
		return new MultiValueMapBuilder<>();
	}

	/**
	 * 构建通用 WebClient，支持自定义 Header、超时、内存等参数
	 * 
	 * @param headers                自定义 Header
	 * @param connectTimeoutMillis   连接超时（毫秒）
	 * @param responseTimeoutSeconds 响应超时（秒）
	 * @param maxInMemorySize        最大内存（字节）
	 * @return WebClient 实例
	 */
	public static WebClient buildWebClient(Map<String, String> headers, int connectTimeoutMillis,
			int responseTimeoutSeconds, int maxInMemorySize) {
		WebClient.Builder builder = WebClient.builder();
		if (headers != null) {
			headers.forEach(builder::defaultHeader);
		}
		builder.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize));
		builder.clientConnector(new ReactorClientHttpConnector(HttpClient.create()
				.option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
				.responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))));
		return builder.build();
	}

	/**
	 * 通用的错误处理方法
	 * 
	 * @param serviceName 服务名称
	 * @param operation   要执行的操作
	 * @param logger      日志记录器
	 * @return 操作结果
	 */
	public static <T> T handleServiceError(String serviceName, Supplier<T> operation, Logger logger) {
		try {
			return operation.get();
		} catch (Exception e) {
			logger.error("Failed to invoke {} service due to: {}", serviceName, e.getMessage());
			return null;
		}
	}

	/**
	 * 通用的参数验证方法
	 * 
	 * @param params 要验证的参数
	 * @return 验证结果
	 */
	public static boolean validateRequestParams(Object... params) {
		return Arrays.stream(params)
				.allMatch(
						param -> param != null && (!(param instanceof String) || StringUtils.hasText((String) param)));
	}

	/**
	 * 通用的JSON响应解析方法
	 * 
	 * @param responseData  响应数据
	 * @param typeReference 目标类型引用
	 * @param logger        日志记录器
	 * @return 解析结果
	 */
	public static <T> T parseJsonResponse(String responseData, TypeReference<T> typeReference, Logger logger) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(responseData, typeReference);
		} catch (Exception e) {
			logger.error("Failed to parse response data: {}", e.getMessage());
			return null;
		}
	}

}
