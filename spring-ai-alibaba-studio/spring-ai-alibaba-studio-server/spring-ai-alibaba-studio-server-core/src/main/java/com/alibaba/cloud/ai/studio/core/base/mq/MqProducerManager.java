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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages RocketMQ producers and provides message sending capabilities.
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducerManager {

	/** RocketMQ client service provider */
	private static final ClientServiceProvider provider = ClientServiceProvider.loadService();

	/** MQ configuration properties */
	private final MqConfigProperties mqConfigProperties;

	/**
	 * Sends a message synchronously
	 * @param producer MQ producer instance
	 * @param message Message to be sent
	 * @return Send result containing message ID
	 */
	public SendResult send(Producer producer, MqMessage message) {
		try {
			SendReceipt sendReceipt = producer.send(buildMessage(message));
			return SendResult.builder().messageId(sendReceipt.getMessageId().toString()).build();
		}
		catch (Exception e) {
			log.error("Failed to send message, topic: {}", message.getTopic(), e);
			throw new RuntimeException("Failed to send message", e);
		}
	}

	/**
	 * Sends a delayed message synchronously
	 * @param producer MQ producer instance
	 * @param message Message to be sent
	 * @param delaySeconds Delay time in seconds
	 * @return Send result containing message ID
	 */
	public SendResult sendDelay(Producer producer, MqMessage message, int delaySeconds) {
		try {
			// 计算延迟时间戳
			long deliveryTimestamp = System.currentTimeMillis() + (delaySeconds * 1000L);
			message.setDeliveryTimestamp(deliveryTimestamp);

			SendReceipt sendReceipt = producer.send(buildMessage(message));
			return SendResult.builder().messageId(sendReceipt.getMessageId().toString()).build();
		}
		catch (Exception e) {
			log.error("Failed to send delayed message, topic: {}, delay: {}s", message.getTopic(), delaySeconds, e);
			throw new RuntimeException("Failed to send delayed message", e);
		}
	}

	/**
	 * Sends messages asynchronously with callback
	 * @param producer MQ producer instance
	 * @param messages List of messages to be sent
	 * @param callback Callback to handle send results
	 */
	public void sendAsync(Producer producer, List<MqMessage> messages, SendCallback callback) {
		try {
			List<CompletableFuture<SendReceipt>> futures = new ArrayList<>();
			for (MqMessage message : messages) {
				CompletableFuture<SendReceipt> future = producer.sendAsync(buildMessage(message));
				futures.add(future);
			}

			for (CompletableFuture<SendReceipt> future : futures) {
				try {
					SendReceipt sendReceipt = future.get(mqConfigProperties.getSendMessageTimeoutMs(),
							TimeUnit.MILLISECONDS);
					callback.onSuccess(SendResult.builder().messageId(sendReceipt.getMessageId().toString()).build());
				}
				catch (Exception e) {
					callback.onError(e);
				}
			}
		}
		catch (Exception e) {
			log.error("Failed to async send message", e);
			throw new RuntimeException("Failed to async send message", e);
		}
	}

	/**
	 * Sends messages asynchronously with success and error handlers
	 * @param producer MQ producer instance
	 * @param messages List of messages to be sent
	 * @param onSuccess Success handler
	 * @param onError Error handler
	 */
	public void sendAsync(Producer producer, List<MqMessage> messages, Consumer<SendResult> onSuccess,
			Consumer<Throwable> onError) {
		sendAsync(producer, messages, new SendCallback() {
			@Override
			public void onSuccess(SendResult sendResult) {
				if (onSuccess != null) {
					onSuccess.accept(sendResult);
				}
			}

			@Override
			public void onError(Throwable e) {
				if (onError != null) {
					onError.accept(e);
				}
			}
		});
	}

	/**
	 * Builds a RocketMQ message from MqMessage
	 * @param message Source message
	 * @return Built RocketMQ message
	 */
	private Message buildMessage(MqMessage message) {
		MessageBuilder msg = provider.newMessageBuilder()
			.setTopic(message.getTopic())
			.setKeys(message.getKeys() == null ? null : message.getKeys().toArray(new String[] {}))
			.setBody(message.getBody().getBytes())
			.setTag(message.getTag());

		if (message.getDeliveryTimestamp() != null) {
			msg.setDeliveryTimestamp(message.getDeliveryTimestamp());
		}

		if (!CollectionUtils.isEmpty(message.getProperties())) {
			for (Map.Entry<String, String> entry : message.getProperties().entrySet()) {
				msg.addProperty(entry.getKey(), entry.getValue());
			}
		}

		return msg.build();
	}

}
