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
package com.alibaba.cloud.ai.autoconfigure.memory;

import com.alibaba.cloud.ai.memory.mem0.core.Mem0Client;
import com.alibaba.cloud.ai.memory.mem0.core.Mem0Server;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = Mem0ChatMemoryProperties.MEM0_PREFIX)
public class Mem0ChatMemoryProperties {

	public static final String MEM0_PREFIX = "spring.ai.alibaba.mem0";

	private Mem0Client client;

	private Mem0Server server;

	public Mem0Client getClient() {
		return client;
	}

	public void setClient(Mem0Client client) {
		this.client = client;
	}

	public Mem0Server getServer() {
		return server;
	}

	public void setServer(Mem0Server server) {
		this.server = server;
	}

}
