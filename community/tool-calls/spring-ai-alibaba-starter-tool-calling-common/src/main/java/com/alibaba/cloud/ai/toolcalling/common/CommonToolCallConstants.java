
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

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

/**
 * @author vlsmb
 */
public final class CommonToolCallConstants {

	// Configuration prefix for tool calling in application.yaml
	public static final String TOOL_CALLING_CONFIG_PREFIX = "spring.ai.alibaba.toolcalling";

	// Default base URL
	public static final String DEFAULT_BASE_URL = "/";

	// Default network timeout in minutes
	public static final int DEFAULT_NETWORK_TIMEOUT = 10;

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	// Maximum memory size in bytes (5MB)
	public static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	// Default connection timeout in milliseconds
	public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

	// Default response timeout in seconds
	public static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 10;

	public static final String NOT_BLANK_REGEX = "\\S+";

	// Default Agents
	public static final String[] DEFAULT_USER_AGENTS = {
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" };

	// Default error handler for RestClient bean
	public static final ResponseErrorHandler DEFAULT_RESTCLIENT_ERROR_HANDLER = new ResponseErrorHandler() {
		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			return response.getStatusCode().isError();
		}

		// original method is deprecated
		@Override
		public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
			throw new RuntimeException(
					"Server error, code: " + response.getStatusCode() + ", message: " + response.getStatusText());
		}
	};

	// Default exception handler for WebClient 4xx errors
	public static final Function<ClientResponse, Mono<? extends Throwable>> DEFAULT_WEBCLIENT_4XX_EXCEPTION = response -> Mono
		.error(new RuntimeException("Server error, code: " + response.statusCode().value()));

	// Default exception handler for WebClient 5xx errors
	public static final Function<ClientResponse, Mono<? extends Throwable>> DEFAULT_WEBCLIENT_5XX_EXCEPTION = response -> Mono
		.error(new RuntimeException("Server error, code: " + response.statusCode().value()));

}
