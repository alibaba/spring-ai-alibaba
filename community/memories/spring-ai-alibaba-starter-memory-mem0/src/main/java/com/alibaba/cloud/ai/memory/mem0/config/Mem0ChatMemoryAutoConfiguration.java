/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.memory.mem0.config;

import com.alibaba.cloud.ai.memory.mem0.core.Mem0MemoryStore;
import com.alibaba.cloud.ai.memory.mem0.core.Mem0ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration
@ConditionalOnProperty(prefix = Mem0ChatMemoryProperties.MEM0_PREFIX + ".server", name = "version",
		matchIfMissing = false)
@EnableConfigurationProperties({ Mem0ChatMemoryProperties.class })
public class Mem0ChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryAutoConfiguration.class);

	@Bean
	public Mem0ServiceClient mem0ServiceClient(Mem0ChatMemoryProperties properties, ResourceLoader resourceLoader) {
		Mem0ServiceClient mem0ServiceClient = new Mem0ServiceClient(properties, resourceLoader);
		logger.info("Initialized Mem0Service Client.success!");
		// Pass the client configuration items to the Server to initialize the Mem0
		// instance
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
