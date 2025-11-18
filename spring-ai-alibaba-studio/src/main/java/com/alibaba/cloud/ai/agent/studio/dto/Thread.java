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

package com.alibaba.cloud.ai.agent.studio.dto;

import com.alibaba.cloud.ai.agent.studio.dto.messages.MessageDTO;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

@JsonDeserialize(builder = Thread.Builder.class)
public final class Thread {
	private final String threadId;
	private final String appName;
	private final String userId;


	private final Map<String, MessageDTO> values;

	private Thread(
			String threadId,
			String appName,
			String userId,
			Map<String, MessageDTO> values) {
		this.threadId = threadId;
		this.appName = appName;
		this.userId = userId;
		this.values = values;
	}

	public static Builder builder(String id) {
		return new Builder(id);
	}

	@JsonProperty("thread_id")
	public String threadId() {
		return threadId;
	}

	@JsonProperty("appName")
	public String appName() {
		return appName;
	}

	@JsonProperty("userId")
	public String userId() {
		return userId;
	}

	@JsonProperty("values")
	public Map<String, MessageDTO> values() {
		return values;
	}

	@Override
	public String toString() {
		return "";
	}

	/** Builder for {@link Thread}. */
	public static final class Builder {
		private String threadId;
		private String appName;
		private String userId;
		private Map<String, MessageDTO> values;

		public Builder(String threadId) {
			this.threadId = threadId;
		}

		@JsonCreator
		private Builder() {
		}

		@CanIgnoreReturnValue
		@JsonProperty("thread_id")
		public Builder threadId(String threadId) {
			this.threadId = threadId;
			return this;
		}

		@CanIgnoreReturnValue
		@JsonProperty("appName")
		public Builder appName(String appName) {
			this.appName = appName;
			return this;
		}

		@CanIgnoreReturnValue
		@JsonProperty("userId")
		public Builder userId(String userId) {
			this.userId = userId;
			return this;
		}

		@CanIgnoreReturnValue
		@JsonProperty("values")
		public Builder values(Map<String, MessageDTO> values) {
			this.values = values;
			return this;
		}

		public Thread build() {
			if (threadId == null) {
				throw new IllegalStateException("Thread id is null");
			}
			return new Thread(threadId, appName, userId, values);
		}
	}
}
