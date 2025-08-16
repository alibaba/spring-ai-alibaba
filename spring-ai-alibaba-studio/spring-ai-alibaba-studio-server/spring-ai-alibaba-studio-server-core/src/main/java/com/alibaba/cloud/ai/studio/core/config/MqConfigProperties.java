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

package com.alibaba.cloud.ai.studio.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ configuration properties for multiple producers and consumers
 *
 * @since 1.0.0.3
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rocketmq")
public class MqConfigProperties {

	/** RocketMQ server endpoints */
	private String endpoints;

	/** Maximum number of retry attempts for sending messages */
	private int maxAttempts = 1;

	/** Message sending timeout in milliseconds */
	private int sendMessageTimeoutMs = 3000;

	/** Maximum number of cached messages */
	private int maxCacheMessageCount = 1024;

	/** Maximum size of cached messages in bytes */
	private int maxCacheMessageSizeInBytes = 64 * 1024 * 1024;

	/** Number of consumer threads */
	private int consumptionThreadCount = 20;

	/** Topic for document indexing */
	private String documentIndexTopic = "topic_saa_studio_document_index";

	/** Consumer group for document indexing */
	private String documentIndexGroup = "group_saa_studio_document_index";

}
