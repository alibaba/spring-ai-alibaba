package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Redis Chat Memory using Redisson.
 *
 * @author benym
 * @date 2025/7/30 19:01
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ RedissonRedisChatMemoryRepository.class, RedissonClient.class })
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@ConditionalOnProperty(name = "spring.ai.memory.redis.client-type", havingValue = "redisson", matchIfMissing = true)
public class RedissonRedisChatMemoryAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedissonRedisChatMemoryAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    RedissonRedisChatMemoryRepository redisChatMemoryRepository(RedisChatMemoryProperties properties) {
        logger.info("Configuring Redis chat memory repository using Redisson");
        return RedissonRedisChatMemoryRepository.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .password(properties.getPassword())
                .timeout(properties.getTimeout())
                .build();
    }
}
