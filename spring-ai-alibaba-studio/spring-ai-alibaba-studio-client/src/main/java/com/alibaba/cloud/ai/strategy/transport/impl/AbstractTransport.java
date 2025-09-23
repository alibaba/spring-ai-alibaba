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

package com.alibaba.cloud.ai.strategy.transport.impl;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import com.alibaba.cloud.ai.strategy.McpInspectorTransportStrategy;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public abstract class AbstractTransport implements McpInspectorTransportStrategy {

	protected AtomicInteger counter = new AtomicInteger(0);

	public static ConcurrentHashMap<McpTransportType, AbstractTransport> transportTypeMap = new ConcurrentHashMap<>();

	@PostConstruct
	private void init() {
		transportTypeMap.put(getTransportType(), this);
	}

	@Override
	public McpSyncClient connect(McpConnectRequest mcpConnectRequest) {
		return transportTypeMap.get(getTransportType()).connect(mcpConnectRequest);
	}

	protected abstract McpTransportType getTransportType();

	public abstract String getClientName();

}
