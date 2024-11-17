package com.alibaba.cloud.ai.memory.config;


import com.alibaba.cloud.ai.memory.message.msql.MySQLChatMemory;
import com.alibaba.cloud.ai.memory.message.msql.MySQLPersistentStorageMemory;
import com.alibaba.cloud.ai.memory.message.redis.RedisChatMemory;
import com.alibaba.cloud.ai.memory.message.redis.RedisPersistentStorageMemory;
import com.alibaba.cloud.ai.memory.token.momory.InMemoryChatMemoryByToken;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
@AutoConfiguration(after = MemoryTypeProperties.class)
@EnableConfigurationProperties(MemoryTypeProperties.class)
public class MemoryAutoConfiguration {
    @Value("${spring.ai.memory.memorytype}")
    private String memoryType;
    @Value("${spring.ai.memory.persistenttype}")
    private String persistenttype;
    @Bean
    public InMemoryChatMemory getInMemoryChatMemoryClass() {
            return new InMemoryChatMemory();
    }
    @Bean
    public MySQLChatMemory getMySQLChatMemoryClass() {
        if (memoryType.equals("message") && persistenttype.equals("mysql")) {
            return new MySQLChatMemory(new MySQLPersistentStorageMemory());
        }
        return null;
    }
    @Bean
    public RedisChatMemory getRedisChatMemoryClass() {
        if (memoryType.equals("message") && persistenttype.equals("redis")) {
            return new RedisChatMemory(new RedisPersistentStorageMemory());
        }
        return null;
    }
    @Bean
    public InMemoryChatMemoryByToken getInMemoryChatMemoryByTokenClass() {
        if (memoryType.equals("token") && persistenttype.equals("memory")) {
            return new InMemoryChatMemoryByToken();
        }
        return null;
    }
}
