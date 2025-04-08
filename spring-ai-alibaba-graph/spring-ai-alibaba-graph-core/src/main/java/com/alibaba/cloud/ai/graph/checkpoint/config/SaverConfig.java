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
package com.alibaba.cloud.ai.graph.checkpoint.config;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import jodd.util.StringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant.MEMORY;

public class SaverConfig {

	private Map<String, BaseCheckpointSaver> savers = new ConcurrentHashMap<>();

	private String type = MEMORY;

	public String getType() {
		return type;
	}

	public SaverConfig setType(String type) {
		this.type = type;
		return this;
	}

	public static Builder builder() {
		return new Builder();
	}

	public SaverConfig register(String type, BaseCheckpointSaver saver) {
		// or computeIfPresent?
		savers.computeIfAbsent(type, s -> saver);
		return this;
	}

	public BaseCheckpointSaver get(String type) {
		if (StringUtil.isEmpty(type))
			throw new IllegalArgumentException("type isn't allow null");
		return savers.get(type);
	}

	public BaseCheckpointSaver get() {
		if (savers.size() == 1) {
			return savers.values().iterator().next();
		}
		return savers.get(this.type);
	}

	public static class Builder {

		private final SaverConfig config;

		Builder() {
			this.config = new SaverConfig();
		}

		public Builder type(String type) {
			this.config.type = type;
			return this;
		}

		public Builder register(String type, BaseCheckpointSaver saver) {
			this.config.register(type, saver);
			return this;
		}

		public SaverConfig build() {
			return this.config;
		}

	}

}
