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

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Redis Memory Connection Configuration
 *
 * @author benym
 * @date 2025/7/30 21:24
 */
public abstract class RedisChatMemoryConnectionAutoConfiguration {

	private final RedisChatMemoryProperties properties;

	private final RedisChatMemoryConnectionDetails connectionDetails;

	public RedisChatMemoryConnectionAutoConfiguration(RedisChatMemoryProperties properties,
			RedisChatMemoryConnectionDetails connectionDetails) {
		this.properties = properties;
		this.connectionDetails = connectionDetails;
	}

	protected RedisChatMemoryProperties.Mode getRedisChatMemoryMode() {
		RedisChatMemoryProperties.Mode mode = properties.getMode();
		if (mode == null) {
			return RedisChatMemoryProperties.Mode.STANDALONE;
		}
		return properties.getMode();
	}

	protected final RedisChatMemoryStandaloneConfiguration getStandaloneConfiguration() {
		RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		return new RedisChatMemoryStandaloneConfiguration(standalone.getHost(), standalone.getPort(),
				connectionDetails.getUsername(), connectionDetails.getPassword(), properties.getTimeout());
	}

	protected final RedisChatMemoryClusterConfiguration getClusterConfiguration() {
		if (properties.getCluster() == null || CollectionUtils.isEmpty(properties.getCluster().getNodes())) {
			return null;
		}
		List<String> nodes = getNodes(connectionDetails.getCluster());
		return new RedisChatMemoryClusterConfiguration(nodes, connectionDetails.getUsername(),
				connectionDetails.getPassword(), properties.getTimeout());
	}

	private List<String> getNodes(RedisConnectionDetails.Cluster cluster) {
		List<String> clusterNodes = new ArrayList<>();
		for (RedisConnectionDetails.Node node : cluster.getNodes()) {
			clusterNodes.add(node.host() + ":" + node.port());
		}
		return clusterNodes;
	}

}
