package com.alibaba.cloud.ai;

import com.alibaba.cloud.ai.function.WeatherSearchFunction;
import com.alibaba.cloud.ai.properties.GaoDeProperties;
import com.alibaba.cloud.ai.service.WebService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author YunLong
 */
@EnableConfigurationProperties(GaoDeProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.gaode-map", name = "enabled", havingValue = "true")
public class GaoDeConfig {

    @Bean
    @ConditionalOnMissingBean
    @Description("Get weather information according to address.")
    public WeatherSearchFunction getAddressWeatherFunction(GaoDeProperties gaoDeProperties) {
        return new WeatherSearchFunction(gaoDeProperties);
    }
}
