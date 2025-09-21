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
package com.alibaba.cloud.ai.memory.redis.builder;

import org.springframework.boot.ssl.SslBundles;

import java.util.List;

/**
 * Base builder for Redis chat memory repositories.
 *
 * @author benym
 * @since 2025/8/26 16:47
 */
public abstract class RedisChatMemoryBuilder<T extends RedisChatMemoryBuilder<T>> {

	protected String host = "127.0.0.1";

	/**
	 * example 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
	 */
	protected List<String> nodes;

	protected int port = 6379;

	protected String username;

	protected String password;

	protected int timeout = 2000;

	protected boolean useCluster = false;

	protected boolean useSsl = false;

	protected String bundle;

	protected SslBundles sslBundles;

	protected abstract T self();

	public T host(String host) {
		this.host = host;
		return self();
	}

	public T nodes(List<String> nodes) {
		this.nodes = nodes;
		this.useCluster = true;
		return self();
	}

	public T port(int port) {
		this.port = port;
		return self();
	}

	public T username(String username) {
		this.username = username;
		return self();
	}

	public T password(String password) {
		this.password = password;
		return self();
	}

	public T timeout(int timeout) {
		this.timeout = timeout;
		return self();
	}

	public T useSsl(boolean useSsl) {
		this.useSsl = useSsl;
		return self();
	}

	public T bundle(String bundle) {
		this.bundle = bundle;
		return self();
	}

	public T sslBundles(SslBundles sslBundles) {
		this.sslBundles = sslBundles;
		return self();
	}

}
