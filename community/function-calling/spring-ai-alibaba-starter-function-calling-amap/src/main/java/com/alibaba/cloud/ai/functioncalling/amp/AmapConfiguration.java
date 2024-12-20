package com.alibaba.cloud.ai.functioncalling.amp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author YunLong
 */
@Configuration
@EnableConfigurationProperties(AmapProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.amap", name = "enabled", havingValue = "true")
public class AmapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get weather information according to address.")
	public WeatherSearchService gaoDeGetAddressWeatherFunction(AmapProperties amapProperties) {
		return new WeatherSearchService(amapProperties);
	}

}
