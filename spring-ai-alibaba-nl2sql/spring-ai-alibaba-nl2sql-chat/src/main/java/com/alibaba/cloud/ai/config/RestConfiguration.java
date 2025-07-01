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

package com.alibaba.cloud.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class RestConfiguration {

	@Value("${rest.connect.timeout:600}")
	private long connectTimeout;

	@Value("${rest.read.timeout:600}")
	private long readTimeout;

	@Value("${webclient.response.timeout:600}")
	private long responseTimeout;

	@Bean
	public RestClientCustomizer restClientCustomizer() {
		return restClientBuilder -> restClientBuilder
			.requestFactory(ClientHttpRequestFactoryBuilder.reactor().withCustomizer(factory -> {
				factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
				factory.setReadTimeout(Duration.ofSeconds(readTimeout));
			}).build());
	}

	@Bean
	public WebClient.Builder webClientBuilder() {

		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
					HttpClient.create().responseTimeout(Duration.ofSeconds(responseTimeout))));
	}

}
