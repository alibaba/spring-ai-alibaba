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

package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 反思机制配置属性
 *
 * @author sixiyida
 * @since 2025/7/10
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.deepresearch.reflection")
public class ReflectionProperties {

	/**
	 * 是否启用反思机制
	 */
	private boolean enabled = true;

	/**
	 * 最大反思尝试次数
	 */
	private int maxAttempts = 2;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

}