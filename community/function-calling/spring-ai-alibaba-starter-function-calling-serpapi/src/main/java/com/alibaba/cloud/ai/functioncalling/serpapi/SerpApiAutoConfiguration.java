package com.alibaba.cloud.ai.functioncalling.serpapi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnClass(SerpApiService.class)
@EnableConfigurationProperties(SerpApiProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.serpapi", name = "enabled", havingValue = "true")
public class SerpApiAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use SerpApi search to query for the latest news.")
	public SerpApiService serpApiFunction(SerpApiProperties properties) {
		return new SerpApiService(properties);
	}

}
