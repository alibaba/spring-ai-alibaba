package com.alibaba.cloud.ai.plugin.searchapi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(SearchApiService.class)
@EnableConfigurationProperties(SearchApiProperties.class)
public class SearchApiConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @Description("Use SearchApi search to query for the latest news.")
    @ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.searchapi", name = "enabled", havingValue = "true")
    public SearchApiService searchApiService(SearchApiProperties properties) {
        return new SearchApiService(properties);
    }
}
