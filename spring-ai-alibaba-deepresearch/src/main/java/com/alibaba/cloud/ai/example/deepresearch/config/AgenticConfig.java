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

import com.alibaba.cloud.ai.example.deepresearch.node.AbstractNode;
import com.alibaba.cloud.ai.example.deepresearch.util.NodeSelectionUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Register the nodes participating in routing into the node container
 *
 * @author ViliamSun
 * @since 1.0.0
 */

@Configuration
public class AgenticConfig implements InitializingBean {

	private final Map<String, String> agentDescriptions;

	public AgenticConfig(List<AbstractNode> nodes) {
		this.agentDescriptions = nodes.stream()
			.collect(Collectors.toMap(node -> node.getNodeDefinition().name(),
					node -> node.getNodeDefinition().description()));
	}

	@Override
	public void afterPropertiesSet() {
		NodeSelectionUtil.init(this.agentDescriptions);
	}

	@Bean
	public Map<String, String> agentDescriptions() {
		return this.agentDescriptions;
	}

}
