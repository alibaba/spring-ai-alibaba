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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Graph;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author vlsmb
 * @since 2025/8/27
 */
@Component
public class StudioDSLAdapter extends AbstractDSLAdapter {

	public StudioDSLAdapter(List<NodeDataConverter<? extends NodeData>> nodeDataConverters,
			@Qualifier("json") Serializer serializer) {
		super(nodeDataConverters, serializer);
	}

	@Override
	public AppMetadata mapToMetadata(Map<String, Object> data) {
		String id = MapReadUtil.getMapDeepValue(data, String.class, "app_id");
		String name = MapReadUtil.getMapDeepValue(data, String.class, "name");
		String description = MapReadUtil.getMapDeepValue(data, String.class, "description");
		String mode = MapReadUtil.getMapDeepValue(data, String.class, "type");
		return new AppMetadata().setId(id).setName(name).setDescription(description).setMode(mode);
	}

	@Override
	public Map<String, Object> metadataToMap(AppMetadata metadata) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Workflow mapToWorkflow(Map<String, Object> data) {
		// 构建Graph
		Workflow workflow = new Workflow();
		Graph graph = this.constructGraph(data);
		workflow.setGraph(graph);

		// register overAllState output key
		List<Variable> extraVars = graph.getNodes().stream().flatMap(node -> {
			NodeType type = NodeType.fromValue(node.getType())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported NodeType: " + node.getType()));
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> conv = (NodeDataConverter<NodeData>) getNodeDataConverter(type);
			return conv.extractWorkflowVars(node.getData());
		}).toList();
		workflow.setWorkflowVars(extraVars);

		return workflow;
	}

	private Graph constructGraph(Map<String, Object> data) {
		Graph graph = new Graph();

		List<Map<String, Object>> nodeMap = MapReadUtil
			.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "nodes"));
		List<Map<String, Object>> edgeMap = MapReadUtil
			.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "edges"));

		List<Node> nodes = this.constructNodes(nodeMap);
		List<Edge> edges = this.constructEdges(edgeMap);
		graph.setNodes(nodes);
		graph.setEdges(edges);
		return graph;
	}

	private List<Node> constructNodes(List<Map<String, Object>> nodeMaps) {
		if (nodeMaps == null) {
			throw new IllegalStateException("nodeMaps is null");
		}

		Map<NodeType, Integer> counters = new HashMap<>();
		List<Node> nodes = new ArrayList<>();

		Map<Class<? extends NodeData>, BiConsumer<? super NodeData, Map<String, String>>> postProcessConsumers = new HashMap<>();

		for (Map<String, Object> nodeMap : nodeMaps) {
			String nodeTypeStr = MapReadUtil.getMapDeepValue(nodeMap, String.class, "type");
			if (nodeTypeStr == null || nodeTypeStr.isBlank()) {
				continue;
			}
			String nodeId = MapReadUtil.getMapDeepValue(nodeMap, String.class, "id");
			String nodeTitle = MapReadUtil.getMapDeepValue(nodeMap, String.class, "name");

			NodeType nodeType = NodeType.fromStudioValue(nodeTypeStr)
				.orElseThrow(() -> new NotImplementedException("unsupported node type " + nodeTypeStr));

			// 构造Node
			Node node = new Node();
			node.setId(nodeId).setType(nodeType.value()).setTitle(nodeTitle);

			// convert node data using specific WorkflowNodeDataConverter
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> converter = (NodeDataConverter<NodeData>) getNodeDataConverter(nodeType);

			NodeData data = converter.parseMapData(nodeMap, DSLDialectType.STUDIO);

			// Generate a readable varName and inject it into NodeData
			int count = counters.merge(nodeType, 1, Integer::sum);
			String varName = converter.generateVarName(count);

			data.setVarName(varName);

			// Post-processing: Overwrite the default outputKey and refresh the outputs
			converter.postProcessOutput(data, varName);

			// 获得处理输入变量名称的Consumer，当所有节点都处理完时使用
			postProcessConsumers.put(data.getClass(), converter.postProcessConsumer(DSLDialectType.DIFY));

			node.setData(data);
			node.setType(nodeType.value());
			nodes.add(node);
		}

		Map<String, String> varNames = nodes.stream()
			.collect(Collectors.toMap(Node::getId, n -> n.getData().getVarName()));

		// 执行每一个节点的postProcess
		nodes.forEach(node -> {
			Class<? extends NodeData> clazz = node.getData().getClass();
			BiConsumer<? super NodeData, Map<String, String>> consumer = postProcessConsumers.get(clazz);
			consumer.accept(node.getData(), varNames);
		});
		return nodes;
	}

	private List<Edge> constructEdges(List<Map<String, Object>> edgeMaps) {
		if (edgeMaps == null) {
			throw new IllegalStateException("edgeMaps is null");
		}
		return edgeMaps.stream().map(edgeMap -> {
			Edge edge = new Edge();
			String id = MapReadUtil.getMapDeepValue(edgeMap, String.class, "id");
			String source = MapReadUtil.getMapDeepValue(edgeMap, String.class, "source");
			String target = MapReadUtil.getMapDeepValue(edgeMap, String.class, "target");
			String sourceHandle = MapReadUtil.getMapDeepValue(edgeMap, String.class, "source_handle");
			String targetHandle = MapReadUtil.getMapDeepValue(edgeMap, String.class, "target_handle");
			edge.setId(id)
				.setSource(source)
				.setTarget(target)
				.setSourceHandle(sourceHandle)
				.setTargetHandle(targetHandle)
				.setDify(false);
			return edge;
		}).toList();
	}

	@Override
	public Map<String, Object> workflowToMap(Workflow workflow) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ChatBot mapToChatBot(Map<String, Object> data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> chatbotToMap(ChatBot chatBot) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void validateDSLData(Map<String, Object> data) {

	}

	@Override
	public Serializer getSerializer() {
		return this.serializer;
	}

	@Override
	public Boolean supportDialect(DSLDialectType dialectType) {
		return DSLDialectType.STUDIO.equals(dialectType);
	}

}
