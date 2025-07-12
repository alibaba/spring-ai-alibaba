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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.advisor.RoutingNodeAdvisor;
import com.alibaba.cloud.ai.example.deepresearch.model.NodeDefinition;
import com.alibaba.cloud.ai.example.deepresearch.util.NodeSelectionUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Abstract class representing a node in a graph. It defines the structure for nodes with
 * a method to get their definition.
 *
 * @author ViliamSun
 * @since 1.0.0
 */
public abstract class AbstractNode {

	protected final ChatClient.Builder builder;

	private NodeDefinition nodeDefinition;

	public AbstractNode(ObjectProvider<ChatClient.Builder> builder) {
		this.builder = builder.getIfAvailable();
	}

	protected ChatClient chatClient() {
		return builder
			.defaultAdvisors(RoutingNodeAdvisor.Builder()
				.selections(NodeSelectionUtil.getAvailableNodes())
				.JSONParser(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
				.build())
			.build();
	}

	public NodeDefinition getNodeDefinition() {
		return nodeDefinition;
	}

	public void setNodeDefinition(NodeDefinition nodeDefinition) {
		this.nodeDefinition = nodeDefinition;
	}

}
