package com.alibaba.cloud.ai.plugin.serpapi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(SerpApiService.class)
@EnableConfigurationProperties(SerpApiProperties.class)
public class SerpApiConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use SerpApi search to query for the latest news.")
	@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.serpapi", name = "enabled", havingValue = "true")
	public SerpApiService serpApiService(SerpApiProperties properties) {
		return new SerpApiService(properties);
	}

}
