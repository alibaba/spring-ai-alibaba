package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Redis Memory support.
 *
 * @author benym
 * @date 2025/7/30 23:35
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@Import({JedisRedisChatMemoryConnectionConfiguration.class, RedissonRedisChatMemoryConnectionConfiguration.class})
public class RedisMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionDetails.class)
    RedisChatMemoryConnectionDetails redisChatMemoryConnectionDetails(RedisChatMemoryProperties properties) {
        return new RedisChatMemoryConnectionDetails(properties);
    }
}
