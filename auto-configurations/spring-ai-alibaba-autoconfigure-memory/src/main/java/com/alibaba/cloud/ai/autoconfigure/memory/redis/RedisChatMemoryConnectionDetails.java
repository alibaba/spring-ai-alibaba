/*
 * Copyright 2023-2025 the original author or authors.
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
package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import java.util.List;

/**
 * Adapts RedisChatMemoryProperties to RedisChatMemoryConnectionDetails.
 *
 * @author benym
 * @since 2025/7/30 20:39
 */
public class RedisChatMemoryConnectionDetails implements RedisMemoryConnectionDetails {

	private final RedisChatMemoryProperties properties;

	public RedisChatMemoryConnectionDetails(RedisChatMemoryProperties properties) {
		this.properties = properties;
	}

	@Override
	public String getUsername() {
		return properties.getUsername();
	}

	@Override
	public String getPassword() {
		return properties.getPassword();
	}

	@Override
	public Standalone getStandalone() {
		return Standalone.of(properties.getHost(), properties.getPort());
	}

	@Override
	public Cluster getCluster() {
		RedisChatMemoryProperties.Cluster cluster = this.properties.getCluster();
		List<Node> nodes = (cluster != null) ? cluster.getNodes().stream().map(this::asNode).toList() : null;
		return (nodes != null) ? () -> nodes : null;
	}

	private Node asNode(String node) {
		int portSeparatorIndex = node.lastIndexOf(':');
		String host = node.substring(0, portSeparatorIndex);
		int port = Integer.parseInt(node.substring(portSeparatorIndex + 1));
		return new Node(host, port);
	}

}
