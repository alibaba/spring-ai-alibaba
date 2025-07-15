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

package com.alibaba.cloud.ai.mcp.client.component;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author yingzi
 * @since 2025/7/14
 */

public class McpReconnectTask implements Delayed {

	private final String serverName;

	private final long delay;

	public McpReconnectTask(String serverName, long delay, TimeUnit unit) {
		this.serverName = serverName;
		this.delay = System.nanoTime() + unit.toNanos(delay);
	}

	public String getServerName() {
		return serverName;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delay - System.nanoTime(), TimeUnit.NANOSECONDS);
	}

	@Override
	public int compareTo(Delayed o) {
		return Long.compare(this.delay, ((McpReconnectTask) o).delay);
	}

}
