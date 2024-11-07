package dev.ai.alibaba.samples.executor.function;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public record WeatherConfig(String apiKey, String apiUrl) {
}
