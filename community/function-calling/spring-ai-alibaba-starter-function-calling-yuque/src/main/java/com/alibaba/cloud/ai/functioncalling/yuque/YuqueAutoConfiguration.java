package com.alibaba.cloud.ai.functioncalling.yuque;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.yuque", name = "enabled", havingValue = "true")
@ConditionalOnClass
@EnableConfigurationProperties(YuqueProperties.class)
public class YuqueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("Use yuque api to invoke a http request to create a doc.")
    public YuqueQueryDocService createYuqueDocFunction (YuqueProperties yuqueProperties) {
        return new YuqueQueryDocService(yuqueProperties);
    }
}
