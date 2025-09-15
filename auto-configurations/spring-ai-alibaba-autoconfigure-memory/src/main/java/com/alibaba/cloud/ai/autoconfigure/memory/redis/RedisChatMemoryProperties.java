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

package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Redis chat memory.
 *
 * @author Jast
 * @author benym
 */
@ConfigurationProperties(prefix = "spring.ai.memory.redis")
public class RedisChatMemoryProperties {

	/**
	 * Redis server host.
	 */
	private String host = "127.0.0.1";

	/**
	 * Redis server port.
	 */
	private int port = 6379;

	/**
	 * Redis server username.
	 */
	private String username;

	/**
	 * Redis server password.
	 */
	private String password;

	/**
	 * Connection timeout in milliseconds.
	 */
	private int timeout = 2000;

	/**
	 * Type of client to use. By default, auto-detected according to the classpath.
	 */
	private ClientType clientType;

	/**
	 * Redis cluster properties.
	 */
	private Cluster cluster;

	/**
	 * Redis memory mode.
	 */
	private Mode mode;

	/**
	 * SSL properties.
	 */
	private final Ssl ssl = new Ssl();

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

	public ClientType getClientType() {
		return clientType;
	}

	public void setClientType(ClientType clientType) {
		this.clientType = clientType;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Ssl getSsl() {
		return ssl;
	}

	/**
	 * Type of Redis client to use.
	 */
	public enum ClientType {

		/**
		 * Use the Lettuce redis client.
		 */
		LETTUCE,

		/**
		 * Use the Jedis redis client.
		 */
		JEDIS,

		/**
		 * Use the Redisson redis client.
		 */
		REDISSON

	}

	/**
	 * Cluster properties
	 */
	public static class Cluster {

		/**
		 * List of "host:port" pairs to bootstrap from. This represents an "initial" list
		 * of cluster nodes and is required to have at least one entry.
		 */
		private List<String> nodes;

		public List<String> getNodes() {
			return nodes;
		}

		public void setNodes(List<String> nodes) {
			this.nodes = nodes;
		}

	}

	/**
	 * Type of Redis memory mode to use
	 */
	public enum Mode {

		/**
		 * Use the standalone mode
		 */
		STANDALONE,

		/**
		 * Use the cluster mode
		 */
		CLUSTER

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name based on spring.ssl configuration.
		 */
		private String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

}
