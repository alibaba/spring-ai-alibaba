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

package com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.builder.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Graph;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.nodedata.IterationNodeData;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.Serializer;
import com.alibaba.cloud.ai.studio.admin.builder.generator.utils.MapReadUtil;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author vlsmb
 * @since 2025/8/27
 */
// TODO: 与DifyDSLAdapter合并一些重复代�?
@Component
public class StudioDSLAdapter extends AbstractDSLAdapter {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

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

		// 节点的输出变�?
		List<Variable> extraVars = graph.getNodes().stream().flatMap(node -> {
			NodeType type = node.getType();
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> conv = (NodeDataConverter<NodeData>) getNodeDataConverter(type);
			return conv.extractWorkflowVars(node.getData());
		}).toList();

		// 会话变量
		Map<?, ?> variableConfigObj = MapReadUtil.getMapDeepValue(data, Map.class, "config", "global_config",
				"variable_config");
		WorkflowConfig.VariableConfig variableConfig = OBJECT_MAPPER.convertValue(variableConfigObj,
				WorkflowConfig.VariableConfig.class);
		List<Variable> conversationVars = variableConfig.getConversationParams()
			.stream()
			.map(param -> new Variable("conversation_" + param.getKey(),
					VariableType.fromStudioValue(param.getType()).orElse(VariableType.OBJECT))
				.setDescription(param.getDesc())
				.setValue(param.getDefaultValue()))
			.toList();

		// 预制变量
		List<Variable> reserveVars = List.of(new Variable("sys_query", VariableType.STRING),
				new Variable("sys_history_list", VariableType.ARRAY_STRING)
					.setVariableStrategy(Variable.Strategy.APPEND));

		workflow.setWorkflowVars(extraVars);
		workflow.setEnvVars(Stream.of(conversationVars, reserveVars).flatMap(List::stream).toList());

		return workflow;
	}

	private Graph constructGraph(Map<String, Object> data) {
		Graph graph = new Graph();

		List<Map<String, Object>> nodeMap = new ArrayList<>(Optional
			.ofNullable(
					MapReadUtil.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "nodes")))
			.orElse(List.of()));
		List<Map<String, Object>> edgeMap = new ArrayList<>(Optional
			.ofNullable(
					MapReadUtil.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "edges")))
			.orElse(List.of()));

		List<Map<String, Object>> innerNodeMaps = new ArrayList<>();
		List<Map<String, Object>> innerEdgeMaps = new ArrayList<>();

		// 展开迭代节点内部的Node和Edge
		nodeMap.forEach(map -> {
			NodeType type = NodeType.fromStudioValue(MapReadUtil.getMapDeepValue(map, String.class, "type"))
				.orElseThrow(() -> new UnsupportedOperationException("unsupported node type " + map.get("type")));
			if (NodeType.ITERATION.equals(type)) {
				List<Map<String, Object>> innerNode = MapReadUtil.safeCastToListWithMap(
						MapReadUtil.getMapDeepValue(map, List.class, "config", "node_param", "block", "nodes"));
				if (innerNode != null) {
					innerNodeMaps.addAll(innerNode);
				}
				List<Map<String, Object>> innerEdge = MapReadUtil.safeCastToListWithMap(
						MapReadUtil.getMapDeepValue(map, List.class, "config", "node_param", "block", "edges"));
				if (innerEdge != null) {
					innerEdgeMaps.addAll(innerEdge);
				}
			}
		});
		nodeMap.addAll(innerNodeMaps);
		edgeMap.addAll(innerEdgeMaps);

		List<Node> nodes = this.constructNodes(nodeMap);
		List<Edge> edges = this.constructEdges(edgeMap);

		Map<String, String> varNames = nodes.stream()
			.collect(Collectors.toMap(Node::getId, n -> n.getData().getVarName()));
		Map<String, Node> nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::getId, n -> n));

		// 根据parnetId进行分组，为了给迭代节点的起始节点传递迭代数�?
		Map<String, List<Node>> groupByParentId = nodes.stream()
			.filter(node -> Objects.nonNull(node.getParentId()))
			.collect(Collectors.groupingBy(Node::getParentId));

		groupByParentId.forEach((parentId, subNodes) -> {
			subNodes.forEach(node -> {
				if (NodeType.ITERATION_START.equals(node.getType())) {
					IterationNodeData nodeData = new IterationNodeData(
							(IterationNodeData) nodeIdMap.get(parentId).getData());
					nodeData.setVarName(nodeIdMap.get(parentId).getData().getVarName() + "_start");
					varNames.put(node.getId(), nodeData.getVarName());
					node.setData(nodeData);
				}
				else if (NodeType.ITERATION_END.equals(node.getType())) {
					IterationNodeData nodeData = new IterationNodeData(
							(IterationNodeData) nodeIdMap.get(parentId).getData());
					nodeData.setVarName(nodeIdMap.get(parentId).getData().getVarName() + "_end");
					varNames.put(node.getId(), nodeData.getVarName());
					node.setData(nodeData);
				}
			});
		});

		// 将Edge里的source和target都转换成varName
		edges.forEach(edge -> {
			edge.setSource(varNames.getOrDefault(edge.getSource(), edge.getSource()));
			edge.setTarget(varNames.getOrDefault(edge.getTarget(), edge.getTarget()));
		});

		// 将Iteration节点起始改为iteration_start，并将Iteration节点结束改为iteration_end
		Map<String, Node> nodeVarMap = nodes.stream().collect(Collectors.toMap(n -> n.getData().getVarName(), n -> n));
		edges.forEach(edge -> {
			if (NodeType.ITERATION.equals(nodeVarMap.get(edge.getSource()).getType())) {
				edge.setSource(edge.getSource() + "_end");
			}
			if (NodeType.ITERATION.equals(nodeVarMap.get(edge.getTarget()).getType())) {
				edge.setTarget(edge.getTarget() + "_start");
			}
		});

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
			node.setId(nodeId)
				.setType(nodeType)
				.setTitle(nodeTitle)
				.setParentId(MapReadUtil.getMapDeepValue(nodeMap, String.class, "parent_id"));

			// convert node data using specific WorkflowNodeDataConverter
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> converter = (NodeDataConverter<NodeData>) getNodeDataConverter(nodeType);

			NodeData data = converter.parseMapData(nodeMap, DSLDialectType.STUDIO);

			// Generate a readable varName and inject it into NodeData
			int count = counters.merge(NodeType.isEmpty(nodeType) ? NodeType.EMPTY : nodeType, 1, Integer::sum);
			String varName = converter.generateVarName(count);

			data.setVarName(varName);

			// 获得处理输入变量名称的Consumer，当所有节点都处理完时使用
			postProcessConsumers.put(data.getClass(), converter.postProcessConsumer(DSLDialectType.STUDIO));

			node.setData(data);
			node.setType(nodeType);
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
				.setTargetHandle(targetHandle);
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
		String type = MapReadUtil.getMapDeepValue(data, String.class, "type");
		if (!"workflow".equalsIgnoreCase(type)) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		Map<?, ?> config = MapReadUtil.getMapDeepValue(data, Map.class, "config");
		if (config == null) {
			throw new IllegalArgumentException("config is null");
		}

		// 检查config是否为WorkflowConfig
		try {
			OBJECT_MAPPER.convertValue(config, WorkflowConfig.class);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid config!");
		}
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
