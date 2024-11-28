package com.alibaba.cloud.ai.plugin.translate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author zhang
 * @date 2024/11/27
 * @Description
 */
@Configuration
@ConditionalOnClass(TranslateService.class)
@EnableConfigurationProperties(TranslateProperties.class)
public class TranslateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("Implement natural language translation capabilities") // function
    public TranslateService translateService(TranslateProperties properties) {
        return new TranslateService(properties);
    }

}
