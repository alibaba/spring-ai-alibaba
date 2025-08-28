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

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Redis Memory Connection Configuration
 *
 * @author benym
 * @since 2025/7/30 21:24
 */
public abstract class RedisChatMemoryConnectionAutoConfiguration<T extends ChatMemoryRepository> {

	private final RedisChatMemoryProperties properties;

	private final RedisChatMemoryConnectionDetails connectionDetails;

	private final SslBundles sslBundles;

	public RedisChatMemoryConnectionAutoConfiguration(RedisChatMemoryProperties properties,
			RedisChatMemoryConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
		this.properties = properties;
		this.connectionDetails = connectionDetails;
		this.sslBundles = sslBundles.getIfAvailable();
	}

	/**
	 * Creates a standalone chat memory repository instance using Redis This abstract
	 * method must be implemented by concrete subclasses to provide a specific
	 * implementation of the chat memory repository.
	 * @param standaloneConfiguration The Redis configuration for standalone mode
	 * @return An initialized standalone instance of the redis chat memory repository
	 * implementation
	 */
	protected abstract T createStandaloneChatMemoryRepository(
			RedisChatMemoryStandaloneConfiguration standaloneConfiguration);

	/**
	 * Creates a cluster chat memory repository instance using Redis This abstract method
	 * must be implemented by concrete subclasses to provide a specific implementation of
	 * the chat memory repository.
	 * @param clusterConfiguration The Redis configuration for cluster mode
	 * @return An initialized cluster instance of the redis chat memory repository
	 * implementation
	 */
	protected abstract T createClusterChatMemoryRepository(RedisChatMemoryClusterConfiguration clusterConfiguration);

	/**
	 * Builds and returns a Redis-based chat memory repository instance based on the
	 * configured mode.
	 * @return A fully configured redis chat memory repository instance
	 */
	protected T buildRedisChatMemoryRepository() {
		RedisChatMemoryProperties.Mode redisChatMemoryMode = getRedisChatMemoryMode();
		if (redisChatMemoryMode == RedisChatMemoryProperties.Mode.STANDALONE) {
			RedisChatMemoryStandaloneConfiguration standaloneConfiguration = getStandaloneConfiguration();
			return createStandaloneChatMemoryRepository(standaloneConfiguration);
		}
		else if (redisChatMemoryMode == RedisChatMemoryProperties.Mode.CLUSTER) {
			RedisChatMemoryClusterConfiguration clusterConfiguration = getClusterConfiguration();
			return createClusterChatMemoryRepository(clusterConfiguration);
		}
		else {
			throw new IllegalArgumentException("Unsupported Redis Chat Memory mode: " + redisChatMemoryMode);
		}
	}

	/**
	 * Retrieves the Redis chat memory mode from configuration properties. This method
	 * provides a safe way to get the deployment mode with fallback logic: - Returns
	 * STANDALONE mode if no mode is explicitly configured - Returns the configured mode
	 * otherwise
	 * @return The configured Redis mode (STANDALONE by default if not specified)
	 */
	protected RedisChatMemoryProperties.Mode getRedisChatMemoryMode() {
		RedisChatMemoryProperties.Mode mode = properties.getMode();
		if (mode == null) {
			return RedisChatMemoryProperties.Mode.STANDALONE;
		}
		return properties.getMode();
	}

	/**
	 * Constructs and returns the standalone Redis configuration for chat memory
	 * @return Fully configured standalone Redis chat memory configuration
	 */
	protected final RedisChatMemoryStandaloneConfiguration getStandaloneConfiguration() {
		RedisMemoryConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		return new RedisChatMemoryStandaloneConfiguration(standalone.getHost(), standalone.getPort(),
				connectionDetails.getUsername(), connectionDetails.getPassword(), properties.getTimeout(),
				properties.getSsl(), sslBundles);
	}

	/**
	 * Constructs and returns the cluster Redis configuration for chat memory
	 * @return Fully configured cluster Redis chat memory configuration
	 */
	protected final RedisChatMemoryClusterConfiguration getClusterConfiguration() {
		if (properties.getCluster() == null || CollectionUtils.isEmpty(properties.getCluster().getNodes())) {
			throw new IllegalStateException("Cluster nodes configuration is required for CLUSTER mode");
		}
		List<String> nodes = getNodes(connectionDetails.getCluster());
		return new RedisChatMemoryClusterConfiguration(nodes, connectionDetails.getUsername(),
				connectionDetails.getPassword(), properties.getTimeout(), properties.getSsl(), sslBundles);
	}

	/**
	 * Transforms cluster node details into connection strings.
	 * @param cluster The cluster connection details containing node information
	 * @return List of Redis node connection strings
	 */
	private List<String> getNodes(RedisMemoryConnectionDetails.Cluster cluster) {
		List<String> clusterNodes = new ArrayList<>();
		for (RedisMemoryConnectionDetails.Node node : cluster.getNodes()) {
			clusterNodes.add(node.host() + ":" + node.port());
		}
		return clusterNodes;
	}

}
