package com.alibaba.cloud.ai.functioncalling.baidusearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author KrakenZJC
 **/
@ConditionalOnClass(BaiduSearchService.class)
@ConditionalOnProperty(value = "spring.ai.alibaba.functioncalling.baidusearch", name = "enabled", havingValue = "true")
public class BaiduSearchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use baidu search engine to query for the latest news.")
	public BaiduSearchService baiduSearchFunction() {
		return new BaiduSearchService();
	}

}