/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.mq;

import com.alibaba.cloud.ai.studio.core.config.MqConfigProperties;
import lombok.Data;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RocketMQ client setup. Provides beans for client configuration
 * and document index producer.
 *
 * @since 1.0.0.3
 */
@Data
@Configuration
public class MqConfig {

	/**
	 * Creates a RocketMQ client configuration bean.
	 * @param mqConfigProperties Configuration properties for MQ
	 * @return Configured client configuration
	 */
	@Bean
	public ClientConfiguration clientConfiguration(MqConfigProperties mqConfigProperties) {
		ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
			.setEndpoints(mqConfigProperties.getEndpoints());
		return builder.build();
	}

	/**
	 * Creates a RocketMQ producer bean for document indexing.
	 * @param clientConfiguration The client configuration
	 * @param mqConfigProperties Configuration properties for MQ
	 * @return Configured document index producer
	 * @throws ClientException if producer creation fails
	 */
	@Bean
	public Producer documentIndexProducer(ClientConfiguration clientConfiguration,
			MqConfigProperties mqConfigProperties) throws ClientException {
		ClientServiceProvider provider = ClientServiceProvider.loadService();
		return provider.newProducerBuilder()
			.setTopics(mqConfigProperties.getDocumentIndexTopic())
			.setMaxAttempts(mqConfigProperties.getMaxAttempts())
			.setClientConfiguration(clientConfiguration)
			.build();
	}

}
