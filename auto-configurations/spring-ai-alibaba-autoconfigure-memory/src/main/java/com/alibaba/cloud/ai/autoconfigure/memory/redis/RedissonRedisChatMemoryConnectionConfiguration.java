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
@ConditionalOnClass({RedissonRedisChatMemoryRepository.class, RedissonClient.class})
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@ConditionalOnProperty(name = "spring.ai.memory.redis.client-type", havingValue = "redisson", matchIfMissing = true)
public class RedissonRedisChatMemoryConnectionConfiguration extends RedisMemoryConnectionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedissonRedisChatMemoryConnectionConfiguration.class);

    public RedissonRedisChatMemoryConnectionConfiguration(RedisChatMemoryProperties properties, RedisChatMemoryConnectionDetails connectionDetails) {
        super(properties, connectionDetails);
    }

    @Bean
    @ConditionalOnMissingBean
    RedissonRedisChatMemoryRepository redisChatMemoryRepository() {
        if (getClusterConfiguration() != null) {
            logger.info("Configuring Redis Cluster chat memory repository using Redisson");
            RedisMemoryClusterConfiguration clusterConfiguration = getClusterConfiguration();
            return RedissonRedisChatMemoryRepository.builder()
                    .nods(clusterConfiguration.nodeAddresses())
                    .username(clusterConfiguration.username())
                    .password(clusterConfiguration.password())
                    .timeout(clusterConfiguration.timeout())
                    .build();
        }
        logger.info("Configuring Redis Standalone chat memory repository using Redisson");
        RedisMemoryStandaloneConfiguration standaloneConfiguration = getStandaloneConfiguration();
        return RedissonRedisChatMemoryRepository.builder()
                .host(standaloneConfiguration.hostName())
                .port(standaloneConfiguration.port())
                .username(standaloneConfiguration.username())
                .password(standaloneConfiguration.password())
                .timeout(standaloneConfiguration.timeout())
                .build();
    }
}
