package com.alibaba.cloud.ai.example.graph.react.tool.weather.function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(WeatherService.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.toolcalling.weather", name = "enabled", havingValue = "true")
public class WeatherAutoConfiguration {

    @Bean(name = "getWeatherFunction")
    @ConditionalOnMissingBean
    @Description("Use api.weather to get weather information.")
    public WeatherService getWeatherServiceFunction(WeatherProperties properties) {
        return new WeatherService(properties);
    }

}
