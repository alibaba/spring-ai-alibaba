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
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ Consumer Manager Manages multiple consumers and provides message consumption
 * capabilities
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqConsumerManager {

	// RocketMQ client service provider
	private static final ClientServiceProvider provider = ClientServiceProvider.loadService();

	// MQ configuration properties
	private final MqConfigProperties mqConfigProperties;

	// Client configuration for RocketMQ
	private final ClientConfiguration clientConfiguration;

	// Map of consumer groups to their corresponding consumer instances
	private final Map<String, PushConsumer> consumerMap = new ConcurrentHashMap<>();

	/**
	 * Subscribe to a topic with specified consumer group
	 * @param group consumer group name
	 * @param topic topic to subscribe
	 * @param handler message handler for processing messages
	 */
	public void subscribe(String group, String topic, MqConsumerHandler<MqMessage> handler) {
		FilterExpression filterExpression = FilterExpression.SUB_ALL;

		try {
			PushConsumer consumer = provider.newPushConsumerBuilder()
				.setConsumerGroup(group)
				.setClientConfiguration(clientConfiguration)
				.setSubscriptionExpressions(Map.of(topic, filterExpression))
				.setMaxCacheMessageCount(mqConfigProperties.getMaxCacheMessageCount())
				.setMaxCacheMessageSizeInBytes(mqConfigProperties.getMaxCacheMessageSizeInBytes())
				.setConsumptionThreadCount(mqConfigProperties.getConsumptionThreadCount())
				.setMessageListener(messageView -> {
					try {
						if (handler != null) {
							MqMessage mqMessage = buildMqMessage(messageView);
							handler.handle(mqMessage);
						}
					}
					catch (Exception e) {
						log.error("Failed to consume message, topic: {}, tag: {}", messageView.getTopic(),
								messageView.getTag(), e);
						return ConsumeResult.FAILURE;

					}
					return ConsumeResult.SUCCESS;
				})
				.build();

			consumerMap.put(group, consumer);
			log.info("Subscribed to group: {}, topic: {}", group, topic);
		}
		catch (ClientException e) {
			log.error("Failed to subscribe to group: {}", group, e);
			throw new RuntimeException("Failed to subscribe", e);
		}
	}

	/**
	 * Shutdown all consumers gracefully
	 */
	@PreDestroy
	public void shutdown() {
		consumerMap.forEach((group, consumer) -> {
			try {
				consumer.close();
			}
			catch (IOException e) {
				log.error("Failed to close consumer, group: {}", group, e);
			}

			log.info("Consumer shutdown successfully, group: {}", group);
		});
	}

	/**
	 * Convert RocketMQ MessageView to internal MqMessage format
	 * @param messageView RocketMQ message view
	 * @return internal MqMessage representation
	 */
	private MqMessage buildMqMessage(MessageView messageView) {
		MqMessage mqMessage = MqMessage.builder()
			.messageId(messageView.getMessageId().toString())
			.topic(messageView.getTopic())
			.tag(messageView.getTag().orElse(null))
			.keys(CollectionUtils.isEmpty(messageView.getKeys()) ? null : messageView.getKeys().stream().toList())
			.body(StandardCharsets.UTF_8.decode(messageView.getBody()).toString())
			.deliveryTimestamp(messageView.getDeliveryTimestamp().orElse(null))
			.properties(messageView.getProperties())
			.build();

		messageView.getProperties().forEach(mqMessage::addProperty);
		return mqMessage;
	}

}
