package com.alibaba.example.chatmemory.config;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.example.chatmemory.mem0.Mem0MemoryStore;
import com.alibaba.example.chatmemory.mem0.Mem0ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration(before = { ChatMemoryAutoConfiguration.class })
@ConditionalOnProperty(prefix = "mem0.server", name = "version")
@EnableConfigurationProperties({ Mem0ChatMemoryProperties.class })
public class Mem0ChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnBean(Mem0ChatMemoryProperties.class)
	public Mem0ServiceClient elasticsearchRestClient(Mem0ChatMemoryProperties properties,
			ResourceLoader resourceLoader) {
		Mem0ServiceClient mem0ServiceClient = new Mem0ServiceClient(properties, resourceLoader);
		logger.info("Initialized Mem0Service Client.success!");
		// 将client配置项交给Server初始化Mem0实例
		mem0ServiceClient.configure(properties.getServer());
		logger.info("Initialized Mem0ZeroService Server success!.");
		return mem0ServiceClient;
	}

	@Bean
	@ConditionalOnBean(Mem0ServiceClient.class)
	public VectorStore mem0MemoryStore(Mem0ServiceClient client) {
		// TODO 客户端初始化后，需要初始化一系列python中的配置
		return Mem0MemoryStore.builder(client).build();
	}

}
