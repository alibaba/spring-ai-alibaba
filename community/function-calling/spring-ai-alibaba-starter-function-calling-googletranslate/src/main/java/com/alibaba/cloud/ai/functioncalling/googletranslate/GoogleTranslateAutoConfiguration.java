package com.alibaba.cloud.ai.functioncalling.googletranslate;


import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author erasernoob
 */
@ConditionalOnClass({ GoogleTranslateService.class })
@EnableConfigurationProperties(GoogleTranslateProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.googletranlate", name = "enabled",
        havingValue = "true")
public class GoogleTranslateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("Implement natural language translation capabilities.")
    public GoogleTranslateService GoogleTranslateFunction(GoogleTranslateProperties properties) {
        return new GoogleTranslateService(properties);
    }
}
