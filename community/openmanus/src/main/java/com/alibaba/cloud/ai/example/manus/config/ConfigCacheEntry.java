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
package com.alibaba.cloud.ai.example.manus.config;

public class ConfigCacheEntry<T> {

	private T value;

	private long lastUpdateTime;

	private static final long EXPIRATION_TIME = 30000; // 30秒过期

	public ConfigCacheEntry(T value) {
		this.value = value;
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
		this.lastUpdateTime = System.currentTimeMillis();
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - lastUpdateTime > EXPIRATION_TIME;
	}

}
