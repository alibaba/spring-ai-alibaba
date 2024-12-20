package com.alibaba.cloud.ai.functioncalling.bingsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author: KrakenZJC
 **/
@ConditionalOnClass(BingSearchService.class)
@EnableConfigurationProperties(BingSearchProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.bingsearch", name = "enabled", havingValue = "true")
public class BingSearchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use bing search engine to query for the latest news.")
	@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.bing", name = "enabled", havingValue = "true")
	public BingSearchService bingSearchFunction(BingSearchProperties properties) {
		return new BingSearchService(properties);
	}

}