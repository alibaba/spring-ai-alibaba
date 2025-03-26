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
package com.alibaba.cloud.ai.service.dsl.adapters;

import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.alibaba.cloud.ai.model.App;

import java.util.*;

/**
 * CustomDSLAdapter converts spring ai alibaba DSL to {@link App} and vice versa.
 */
@Component
public class CustomDSLAdapter extends AbstractDSLAdapter {

	private final Serializer serializer;

	private final ObjectMapper objectMapper;

	private final List<NodeDataConverter<?>> nodeDataConverters;

	public CustomDSLAdapter(@Qualifier("yaml") Serializer serializer, List<NodeDataConverter<?>> nodeDataConverters) {
		this.serializer = serializer;
		this.nodeDataConverters = nodeDataConverters;
		this.objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	}

	@Override
	public AppMetadata mapToMetadata(Map<String, Object> data) {
		Map<String, Object> metadataMap = (Map<String, Object>) data.get("metadata");
		AppMetadata metadata = objectMapper.convertValue(metadataMap, AppMetadata.class);
		metadata.setId(UUID.randomUUID().toString());
		return metadata;
	}

	@Override
	public Map<String, Object> metadataToMap(AppMetadata metadata) {
		Map<String, Object> data = new HashMap<>();
		data.put("metadata", objectMapper.convertValue(metadata, new TypeReference<>() {
		}));
		return data;
	}

	@Override
	public Workflow mapToWorkflow(Map<String, Object> data) {
		Workflow workflow = new Workflow();
		Map<String, Object> specMap = (Map<String, Object>) data.get("spec");
		if (specMap.containsKey("workflowVars")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) specMap.get("workflowVars");
			List<Variable> workflowVars = variables.stream()
				.map(variable -> objectMapper.convertValue(variable, Variable.class))
				.toList();
			workflow.setWorkflowVars(workflowVars);
		}
		if (specMap.containsKey("envVars")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) specMap.get("envVars");
			List<Variable> envVars = variables.stream()
				.map(variable -> objectMapper.convertValue(variable, Variable.class))
				.toList();
			workflow.setEnvVars(envVars);
		}
		if (specMap.containsKey("graph")) {
			Graph graph = constructGraph((Map<String, Object>) specMap.get("graph"));
			workflow.setGraph(graph);
		}
		return workflow;
	}

	private Graph constructGraph(Map<String, Object> data) {
		Graph graph = new Graph();
		List<Node> nodes = new ArrayList<>();
		List<Edge> edges = new ArrayList<>();
		// convert nodes
		if (data.containsKey("nodes")) {
			List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) data.get("nodes");
			nodes = nodeMaps.stream().map(this::constructNode).toList();
		}
		// convert edges
		if (data.containsKey("edges")) {
			edges = objectMapper.convertValue(data.get("edges"), new TypeReference<>() {
			});
		}
		graph.setNodes(nodes);
		graph.setEdges(edges);
		return graph;
	}

	private Node constructNode(Map<String, Object> nodeMap) {
		Map<String, Object> nodeDataMap = (Map<String, Object>) nodeMap.remove("data");
		Node node = objectMapper.convertValue(nodeMap, Node.class);
		NodeType nodeType = NodeType.fromValue(node.getType())
			.orElseThrow(() -> new NotImplementedException("Unsupported Node Type: " + node.getType()));
		NodeDataConverter<?> nodeDataConverter = getNodeDataConverter(nodeType);
		node.setData(nodeDataConverter.parseMapData(nodeDataMap, DSLDialectType.CUSTOM));
		return node;
	}

	private NodeDataConverter<?> getNodeDataConverter(NodeType nodeType) {
		return nodeDataConverters.stream()
			.filter(converter -> converter.supportNodeType(nodeType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("invalid dify node type " + nodeType));
	}

	@Override
	public Map<String, Object> workflowToMap(Workflow workflow) {
		Map<String, Object> data = new HashMap<>();
		data.put("spec", objectMapper.convertValue(workflow, new TypeReference<>() {
		}));
		return data;
	}

	// TODO
	@Override
	public ChatBot mapToChatBot(Map<String, Object> data) {
		return null;
	}

	// TODO
	@Override
	public Map<String, Object> chatbotToMap(ChatBot chatBot) {
		return null;
	}

	@Override
	public void validateDSLData(Map<String, Object> data) {
		Map<String, Object> metadataMap = (Map<String, Object>) data.get("metadata");
		if (metadataMap == null || !metadataMap.containsKey("mode")) {
			throw new IllegalArgumentException("invalid dsl");
		}
	}

	@Override
	public Serializer getSerializer() {
		return serializer;
	}

	@Override
	public Boolean supportDialect(DSLDialectType dialectType) {
		return DSLDialectType.CUSTOM.equals(dialectType);
	}

}
