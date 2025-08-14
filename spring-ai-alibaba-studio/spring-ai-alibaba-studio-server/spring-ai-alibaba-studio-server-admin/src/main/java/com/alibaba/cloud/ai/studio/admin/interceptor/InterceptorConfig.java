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
			.excludePathPatterns("/console/v1/auth/login", "/console/v1/auth/refresh-token", "/console/v1/system/**")
			.excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**")
			.excludePathPatterns("/test/**");

		registry.addInterceptor(apiKeyAuthInterceptor).addPathPatterns("/api/v1/**");
	}

}
