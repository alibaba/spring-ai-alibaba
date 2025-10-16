/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP Session storage configuration properties
 *
 * @author Libres-coder
 * @since 2025.10.16
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.router.session")
public class McpSessionProperties {

	private StoreType storeType = StoreType.MEMORY;

	private Redis redis = new Redis();

	public StoreType getStoreType() {
		return storeType;
	}

	public void setStoreType(StoreType storeType) {
		this.storeType = storeType;
	}

	public Redis getRedis() {
		return redis;
	}

	public void setRedis(Redis redis) {
		this.redis = redis;
	}

	public enum StoreType {

		MEMORY, REDIS

	}

	public static class Redis {

		private String keyPrefix = "mcp:session:";

		private long ttl = 1800;

		public String getKeyPrefix() {
			return keyPrefix;
		}

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		public long getTtl() {
			return ttl;
		}

		public void setTtl(long ttl) {
			this.ttl = ttl;
		}

	}

}

