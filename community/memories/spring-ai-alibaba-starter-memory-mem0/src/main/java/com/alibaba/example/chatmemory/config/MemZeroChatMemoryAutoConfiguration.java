package com.alibaba.example.chatmemory.config;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.example.chatmemory.mem0.MemZeroMemoryStore;
import com.alibaba.example.chatmemory.mem0.MemZeroServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration(before = { ChatMemoryAutoConfiguration.class })
@ConditionalOnProperty(prefix = "mem0.server", name = "version", havingValue = "v1.1")
public class MemZeroChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MemZeroChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public MemZeroChatMemoryProperties memZeroChatMemoryProperties() {
		return new MemZeroChatMemoryProperties();
	}

	@Bean
	@ConditionalOnBean(MemZeroChatMemoryProperties.class)
	public MemZeroServiceClient elasticsearchRestClient(MemZeroChatMemoryProperties properties,
			ResourceLoader resourceLoader) {
		MemZeroServiceClient memZeroServiceClient = new MemZeroServiceClient(properties, resourceLoader);
		logger.info("Initialized MemZeroService Client.success!");
		// 将client配置项交给Server初始化Mem0实例
		memZeroServiceClient.configure(properties.getServer());
		logger.info("Initialized MemZeroService Server success!.");
		return memZeroServiceClient;
	}

	@Bean
	@ConditionalOnBean(MemZeroServiceClient.class)
	public VectorStore memZeroMemoryStore(MemZeroServiceClient client) {
		// TODO 客户端初始化后，需要初始化一系列python中的配置
		return MemZeroMemoryStore.builder(client).build();
	}

}
