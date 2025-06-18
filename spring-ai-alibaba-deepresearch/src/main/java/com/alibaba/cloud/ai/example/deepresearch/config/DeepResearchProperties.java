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

package com.alibaba.cloud.ai.example.deepresearch.config;

import com.google.common.collect.Maps;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sixiyida
 * @since 2025/6/14
 */
@ConfigurationProperties(prefix = DeepResearchProperties.PREFIX)
public class DeepResearchProperties {

	public static final String PREFIX = "spring.ai.alibaba.deepresearch";

	/**
	 * Parallel node count, key=node name, value=node count
	 */
	private Map<String, Integer> parallelNodeCount = new HashMap<>();

	/**
	 * McpClient mapping for Agent name. key=Agent name, value=McpClient Name
	 */
	private Map<String, Set<String>> mcpClientMapping = Maps.newHashMap();

	public Map<String, Integer> getParallelNodeCount() {
		return parallelNodeCount;
	}

	public void setParallelNodeCount(Map<String, Integer> parallelNodeCount) {
		this.parallelNodeCount = parallelNodeCount;
	}

	public Map<String, Set<String>> getMcpClientMapping() {
		return mcpClientMapping;
	}

	public void setMcpClientMapping(Map<String, Set<String>> mcpClientMapping) {
		this.mcpClientMapping = mcpClientMapping;
	}

}
