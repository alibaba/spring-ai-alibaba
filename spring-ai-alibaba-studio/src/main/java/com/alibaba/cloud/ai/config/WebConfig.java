package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.tracing.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final TraceIdInterceptor traceIdInterceptor;

	public WebConfig(TraceIdInterceptor traceIdInterceptor) {
		this.traceIdInterceptor = traceIdInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
	}

}
