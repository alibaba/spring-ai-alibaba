package com.alibaba.cloud.ai.toolcalling.commontoolcall;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JsonParseService.class)
public class CommonToolCallAutoConfiguration {

	@Bean
	public JsonParseService jsonParseService() {
		return new JsonParseService();
	}

	@Bean
	@ConditionalOnMissingBean(WebClientService.class)
	public WebClientService commonWebClientService() {
		return new WebClientService(jsonParseService());
	}

	@Bean
	@ConditionalOnMissingBean(RestClientService.class)
	public RestClientService commonRestClientService() {
		return new RestClientService(jsonParseService());
	}

}
