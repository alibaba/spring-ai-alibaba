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

package com.alibaba.cloud.ai.autoconfigure.mcp.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author yingzi
 * @since 2025/7/14
 */
@ConfigurationProperties(prefix = McpRecoveryAutoProperties.CONFIG_PREFIX)
public class McpRecoveryAutoProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.recovery";

	private Duration delay = Duration.ofSeconds(5L);

	;

	private Duration stop = Duration.ofSeconds(10L);

	public Duration getDelay() {
		return delay;
	}

	public void setDelay(Duration delay) {
		this.delay = delay;
	}

	public Duration getStop() {
		return stop;
	}

	public void setStop(Duration stop) {
		this.stop = stop;
	}

}
