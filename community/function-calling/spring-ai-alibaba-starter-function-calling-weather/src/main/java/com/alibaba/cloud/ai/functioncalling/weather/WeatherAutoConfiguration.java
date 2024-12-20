package com.alibaba.cloud.ai.functioncalling.weather;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnClass(WeatherService.class)
@EnableConfigurationProperties(WeatherProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.weather", name = "enabled", havingValue = "true")
public class WeatherAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use api.weather to get weather information.")
	public WeatherService getWeatherServiceFunction(WeatherProperties properties) {
		return new WeatherService(properties);
	}

}
