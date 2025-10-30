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

package com.alibaba.cloud.ai.agent.dto;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

@JsonDeserialize(builder = Session.Builder.class)
public final class Session {
	private final String id;

	private final String appName;

	private final String userId;

	private Instant lastUpdateTime;

	private Session(
			String appName,
			String userId,
			String id,
			Instant lastUpdateTime) {
		this.id = id;
		this.appName = appName;
		this.userId = userId;
		this.lastUpdateTime = lastUpdateTime;
	}

	public static Builder builder(String id) {
		return new Builder(id);
	}

	@JsonProperty("id")
	public String id() {
		return id;
	}

	@JsonProperty("appName")
	public String appName() {
		return appName;
	}

	@JsonProperty("userId")
	public String userId() {
		return userId;
	}

	public void lastUpdateTime(Instant lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public Instant lastUpdateTime() {
		return lastUpdateTime;
	}

	@JsonProperty("lastUpdateTime")
	public double getLastUpdateTimeAsDouble() {
		if (lastUpdateTime == null) {
			return 0.0;
		}
		long seconds = lastUpdateTime.getEpochSecond();
		int nanos = lastUpdateTime.getNano();
		return seconds + nanos / (double) Duration.ofSeconds(1).toNanos();
	}

	@Override
	public String toString() {
		return "";
	}

	/** Builder for {@link Session}. */
	public static final class Builder {
		private String id;
		private String appName;
		private String userId;
		private Instant lastUpdateTime = Instant.EPOCH;

		public Builder(String id) {
			this.id = id;
		}

		@JsonCreator
		private Builder() {
		}

		@CanIgnoreReturnValue
		@JsonProperty("id")
		public Builder id(String id) {
			this.id = id;
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
		public Builder lastUpdateTime(Instant lastUpdateTime) {
			this.lastUpdateTime = lastUpdateTime;
			return this;
		}

		@CanIgnoreReturnValue
		@JsonProperty("lastUpdateTime")
		public Builder lastUpdateTimeSeconds(double seconds) {
			long secs = (long) seconds;
			// Convert fractional part to nanoseconds
			long nanos = (long) ((seconds - secs) * Duration.ofSeconds(1).toNanos());
			this.lastUpdateTime = Instant.ofEpochSecond(secs, nanos);
			return this;
		}

		public Session build() {
			if (id == null) {
				throw new IllegalStateException("Session id is null");
			}
			return new Session(appName, userId, id, lastUpdateTime);
		}
	}
}
