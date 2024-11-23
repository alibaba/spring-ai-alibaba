package com.alibaba.cloud.ai.memory.config;



import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
@AutoConfiguration
@EnableConfigurationProperties
public class MemoryAutoConfiguration {
    @Value("${spring.ai.memory.memorytype}")
    private String memoryType;
    @Value("${spring.ai.memory.persistenttype}")
    private String persistenttype;
    @Bean
    public InMemoryChatMemory getInMemoryChatMemoryClass() {
            return new InMemoryChatMemory();
    }
}
