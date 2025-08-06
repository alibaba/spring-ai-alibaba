/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP 工具配置属性
 */
@Component
@ConfigurationProperties(prefix = "mcp.tools")
public class McpToolProperties {

	/**
	 * 是否启用自动发现
	 */
	private boolean autoDiscover = true;

	/**
	 * 要扫描的包路径
	 */
	private List<String> packages = List.of("com.alibaba.cloud.ai.example.manus.tool",
			"com.alibaba.cloud.ai.example.manus.inhouse.tool");

	/**
	 * 要排除的包路径
	 */
	private List<String> excluded = List.of("com.alibaba.cloud.ai.example.manus.tool.internal");

	/**
	 * 是否启用调试日志
	 */
	private boolean debug = false;

	public boolean isAutoDiscover() {
		return autoDiscover;
	}

	public void setAutoDiscover(boolean autoDiscover) {
		this.autoDiscover = autoDiscover;
	}

	public List<String> getPackages() {
		return packages;
	}

	public void setPackages(List<String> packages) {
		this.packages = packages;
	}

	public List<String> getExcluded() {
		return excluded;
	}

	public void setExcluded(List<String> excluded) {
		this.excluded = excluded;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
