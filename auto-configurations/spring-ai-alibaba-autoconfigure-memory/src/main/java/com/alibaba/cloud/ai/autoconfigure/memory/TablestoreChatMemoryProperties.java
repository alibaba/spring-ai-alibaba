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

package com.alibaba.cloud.ai.autoconfigure.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Tablestore chat memory.
 */
@ConfigurationProperties(prefix = "spring.ai.memory.tablestore")
public class TablestoreChatMemoryProperties {

	private String endpoint;

	private String instanceName;

	private String accessKeyId;

	private String accessKeySecret;

	private String sessionTableName = "session";

	private String sessionSecondaryIndexName = "session_secondary_index";

	private String messageTableName = "message";

	private String messageSecondaryIndexName = "message_secondary_index";

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getAccessKeySecret() {
		return accessKeySecret;
	}

	public void setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
	}

	public String getSessionTableName() {
		return sessionTableName;
	}

	public void setSessionTableName(String sessionTableName) {
		this.sessionTableName = sessionTableName;
	}

	public String getSessionSecondaryIndexName() {
		return sessionSecondaryIndexName;
	}

	public void setSessionSecondaryIndexName(String sessionSecondaryIndexName) {
		this.sessionSecondaryIndexName = sessionSecondaryIndexName;
	}

	public String getMessageTableName() {
		return messageTableName;
	}

	public void setMessageTableName(String messageTableName) {
		this.messageTableName = messageTableName;
	}

	public String getMessageSecondaryIndexName() {
		return messageSecondaryIndexName;
	}

	public void setMessageSecondaryIndexName(String messageSecondaryIndexName) {
		this.messageSecondaryIndexName = messageSecondaryIndexName;
	}

}
