package com.alibaba.cloud.ai.plugin.googleserper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(GoogleSerperService.class)
@EnableConfigurationProperties(GoogleSerperProperties.class)
public class GoogleSerperConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("Use google serper to query for the latest news.") // function
    @ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.googleserper", name = "enabled", havingValue = "true")
    public GoogleSerperService bingSearchService(GoogleSerperProperties properties) {
        return new GoogleSerperService(properties);
    }
}
