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
package com.alibaba.cloud.ai.studio.admin.interceptor;

import com.alibaba.cloud.ai.studio.interceptor.ApiKeyAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for setting up web request interceptors
 */
@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

	/** Interceptor for token-based authentication */
	private final TokenAuthInterceptor tokenAuthInterceptor;

	/** Interceptor for API key authentication */
	private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;

	/**
	 * Configures request interceptors for different API paths
	 * @param registry The interceptor registry
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(tokenAuthInterceptor)
			.addPathPatterns("/console/v1/**")
			.addPathPatterns("/starter.zip")
			.excludePathPatterns("/console/v1/auth/login", "/console/v1/auth/refresh-token", "/console/v1/system/**")
			.excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**")
			.excludePathPatterns("/test/**");

		registry.addInterceptor(apiKeyAuthInterceptor).addPathPatterns("/api/v1/**");
	}

}
