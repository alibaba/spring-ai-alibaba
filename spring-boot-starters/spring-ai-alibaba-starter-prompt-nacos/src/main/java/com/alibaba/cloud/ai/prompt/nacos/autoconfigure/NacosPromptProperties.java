/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.prompt.nacos.autoconfigure;

import java.util.Properties;

import com.alibaba.nacos.api.PropertyKeyConst;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = NacosPromptProperties.PREFIX)
public class NacosPromptProperties {

	public static final String PREFIX = "spring.ai.alibaba.prompt.nacos";

	private String namespace = "public";

	private String serverAddr = "127.0.0.1:8848";

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	public Properties getNacosProperties() {
		Properties properties = new Properties();
		properties.put(PropertyKeyConst.NAMESPACE, this.namespace);
		properties.put(PropertyKeyConst.SERVER_ADDR, this.serverAddr);
		return properties;
	}

}
