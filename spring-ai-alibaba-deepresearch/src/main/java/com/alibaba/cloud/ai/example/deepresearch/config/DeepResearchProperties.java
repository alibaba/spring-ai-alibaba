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

import java.util.Map;
import java.util.Set;

/**
 * @author Allen Hu
 * @since 2025/5/24 <<<<<<< HEAD
 * @author sixiyida
 * @since 2025/6/14 ======= >>>>>>> origin/main
 */
@ConfigurationProperties(prefix = DeepResearchProperties.PREFIX)
public class DeepResearchProperties {

	public static final String PREFIX = "spring.ai.alibaba.deepresearch";

	/**
	 * Number of researcher nodes to create
	 */
	private int researcherNodeCount = 3;

	/**
	 * Number of coder nodes to create
	 */
	private int coderNodeCount = 3;

	public int getResearcherNodeCount() {
		return researcherNodeCount;
	}

	public void setResearcherNodeCount(int researcherNodeCount) {
		this.researcherNodeCount = researcherNodeCount;
	}

	public int getCoderNodeCount() {
		return coderNodeCount;
	}

	public void setCoderNodeCount(int coderNodeCount) {
		this.coderNodeCount = coderNodeCount;
	}

	/**
	 * McpClient mapping for Agent name. key=Agent name, value=McpClient Name
	 */
	private Map<String, Set<String>> mcpClientMapping = Maps.newHashMap();

	public Map<String, Set<String>> getMcpClientMapping() {
		return mcpClientMapping;
	}

	public void setMcpClientMapping(Map<String, Set<String>> mcpClientMapping) {
		this.mcpClientMapping = mcpClientMapping;
	}

}
