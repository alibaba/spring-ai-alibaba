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

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Function;

/**
 * @author vlsmb
 */
public final class CommonToolCallConstants {

	// toolcall properties prefix in application.yaml
	public static final String TOOL_CALLING_CONFIG_PREFIX = "spring.ai.alibaba.toolcalling";

	public static final String DEFAULT_BASE_URL = "/";

	// default timeout minutes
	public static final int DEFAULT_NETWORK_TIMEOUT = 10;

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	public static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	// default error handler for restclient bean
	public static final ResponseErrorHandler DEFAULT_RESTCLIENT_ERROR_HANDLER = new ResponseErrorHandler() {
		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			return response.getStatusCode().isError();
		}

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			throw new RuntimeException(
					"Server error, code: " + response.getStatusCode() + ", message: " + response.getStatusText());
		}
	};

	// default exceptionFunction for webclient
	public static final Function<ClientResponse, Mono<? extends Throwable>> DEFAULT_WEBCLIENT_4XX_EXCEPTION = response -> Mono
		.error(new RuntimeException("Server error, code: " + response.statusCode().value()));

	// default exceptionFunction for webclient
	public static final Function<ClientResponse, Mono<? extends Throwable>> DEFAULT_WEBCLIENT_5XX_EXCEPTION = response -> Mono
		.error(new RuntimeException("Server error, code: " + response.statusCode().value()));

}
