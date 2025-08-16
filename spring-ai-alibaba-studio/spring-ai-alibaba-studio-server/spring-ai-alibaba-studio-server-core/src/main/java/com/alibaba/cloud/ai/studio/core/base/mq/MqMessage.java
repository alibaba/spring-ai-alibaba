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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic message class for message queue operations. Supports different message types
 * and properties.
 *
 * @since 1.0.0.3
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MqMessage implements Serializable {

	/** Unique identifier for the message */
	private String messageId;

	/** Topic to which the message belongs */
	@NotNull
	private String topic;

	/** Message tag for categorization */
	@NotNull
	private String tag;

	/** List of message keys for identification */
	private List<String> keys;

	/** Actual content of the message */
	private String body;

	/** Additional properties associated with the message */
	private Map<String, String> properties = new HashMap<>();

	/** Timestamp when the message was delivered */
	private Long deliveryTimestamp;

	/**
	 * Adds a property to the message
	 * @param key property key
	 * @param value property value
	 * @return this message instance
	 */
	public MqMessage addProperty(String key, String value) {
		this.properties.put(key, value);
		return this;
	}

	/**
	 * Retrieves a property value by its key
	 * @param key property key
	 * @return property value
	 */
	public String getProperty(String key) {
		return this.properties.get(key);
	}

}
