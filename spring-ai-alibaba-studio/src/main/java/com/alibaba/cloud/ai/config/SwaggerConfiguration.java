package com.alibaba.cloud.ai.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

	@Bean
	public GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder().group("studio接口文档").pathsToMatch("/studio/**").build();
	}

}
