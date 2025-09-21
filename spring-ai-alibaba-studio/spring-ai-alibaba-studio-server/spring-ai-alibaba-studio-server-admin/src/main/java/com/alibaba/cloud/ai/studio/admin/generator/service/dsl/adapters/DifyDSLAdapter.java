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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Graph;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.IterationNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.commons.lang3.NotImplementedException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * DifyDSLAdapter converts Dify DSL to {@link App} and vice versa.
 */
@Component
public class DifyDSLAdapter extends AbstractDSLAdapter {

	private static final String[] DIFY_CHATBOT_MODES = { "chat", "completion", "agent-chat" };

	private static final String[] DIFY_WORKFLOW_MODES = { "workflow", "advanced-chat" };

	public DifyDSLAdapter(List<NodeDataConverter<? extends NodeData>> nodeDataConverters,
			@Qualifier("yaml") Serializer serializer) {
		super(nodeDataConverters, serializer);
	}

	@Override
	public void validateDSLData(Map<String, Object> dslData) {
		if (dslData == null || !dslData.containsKey("app")) {
			throw new IllegalArgumentException("invalid dify dsl");
		}
	}

	@Override
	public Serializer getSerializer() {
		return serializer;
	}

	@Override
	public AppMetadata mapToMetadata(Map<String, Object> data) {
		Map<String, Object> map = (Map<String, Object>) data.get("app");
		AppMetadata metadata = new AppMetadata();
		if (Arrays.asList(DIFY_CHATBOT_MODES).contains((String) map.get("mode"))) {
			metadata.setMode(AppMetadata.CHATBOT_MODE);
		}
		else if (Arrays.asList(DIFY_WORKFLOW_MODES).contains((String) map.get("mode"))) {
			metadata.setMode(AppMetadata.WORKFLOW_MODE);
		}
		else {
			throw new IllegalArgumentException("unknown dify app mode" + map.get("mode"));
		}
		metadata.setId(UUID.randomUUID().toString());
		metadata.setName((String) map.getOrDefault("name", metadata.getMode() + "-" + metadata.getId()));
		metadata.setDescription((String) map.getOrDefault("description", ""));
		return metadata;
	}

	@Override
	public Map<String, Object> metadataToMap(AppMetadata metadata) {
		Map<String, Object> data = new HashMap<>();
		String difyMode = metadata.getMode().equals(AppMetadata.WORKFLOW_MODE) ? "workflow" : "agent-chat";
		data.put("app", Map.of("name", metadata.getName(), "description", metadata.getDescription(), "mode", difyMode));
		data.put("kind", "app");
		return data;
	}

	@Override
	public Workflow mapToWorkflow(Map<String, Object> data) {
		Map<String, Object> workflowData = MapReadUtil.safeCastToMapWithStringKey(data.get("workflow"));
		Workflow workflow = new Workflow();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// map key is snake_case style
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		List<Variable> convVars = new ArrayList<>();
		if (workflowData.containsKey("conversation_variables")) {
			List<Map<String, Object>> variables = MapReadUtil
				.safeCastToListWithMap(workflowData.get("conversation_variables"));
			convVars = variables.stream().map(this::convertToVariable).toList();
		}

		List<Variable> envVars = List.of();
		if (workflowData.containsKey("environment_variables")) {
			List<Map<String, Object>> variables = MapReadUtil
				.safeCastToListWithMap(workflowData.get("environment_variables"));
			envVars = variables.stream().map(this::convertToVariable).toList();
		}
		List<Variable> sysVars = List.of(new Variable("sys_query", VariableType.STRING),
				new Variable("sys_files", VariableType.ARRAY_FILE),
				new Variable("sys_dialogue_count", VariableType.NUMBER),
				new Variable("sys_conversation_id", VariableType.STRING),
				new Variable("sys_user_id", VariableType.STRING), new Variable("sys_app_id", VariableType.STRING),
				new Variable("sys_workflow_id", VariableType.STRING),
				new Variable("sys_workflow_run_id", VariableType.STRING));
		workflow.setEnvVars(Stream.of(envVars, sysVars).flatMap(List::stream).toList());

		Graph graph = constructGraph(MapReadUtil.safeCastToMapWithStringKey(workflowData.get("graph")));

		workflow.setGraph(graph);
		// register overAllState output key
		List<Variable> extraVars = graph.getNodes().stream().flatMap(node -> {
			NodeType type = node.getType();
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> conv = (NodeDataConverter<NodeData>) getNodeDataConverter(type);
			return conv.extractWorkflowVars(node.getData());
		}).toList();

		List<Variable> allVars = new ArrayList<>(Stream.concat(convVars.stream(), extraVars.stream())
			.collect(Collectors.toMap(Variable::getName, v -> v, (v1, v2) -> v1))
			.values());

		workflow.setWorkflowVars(allVars);

		return workflow;
	}

	private Graph constructGraph(Map<String, Object> data) {
		Graph graph = new Graph();
		List<Node> nodes;
		List<Edge> edges;

		// convert nodes
		if (data.containsKey("nodes")) {
			List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) data.get("nodes");
			nodes = new ArrayList<>(constructNodes(nodeMaps));
		}
		else {
			nodes = new ArrayList<>();
		}

		// convert edges
		if (data.containsKey("edges")) {
			List<Map<String, Object>> edgeMaps = (List<Map<String, Object>>) data.get("edges");
			edges = new ArrayList<>(constructEdges(edgeMaps));
		}
		else {
			edges = new ArrayList<>();
		}

		Map<String, String> varNames = nodes.stream()
			.collect(Collectors.toMap(Node::getId, n -> n.getData().getVarName()));
		Map<String, Node> nodeIdMap = nodes.stream().collect(Collectors.toMap(Node::getId, n -> n));

		// 根据parnetId进行分组，为了给迭代节点的起始节点传递迭代数据
		Map<String, List<Node>> groupByParentId = nodes.stream()
			.filter(node -> Objects.nonNull(node.getParentId()))
			.collect(Collectors.groupingBy(Node::getParentId));

		// 统计具有出度的节点
		Set<String> nodeIdHasOut = edges.stream().map(Edge::getSource).collect(Collectors.toSet());

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

			// 添加迭代节点的终止节点（Dify的DSL没有提供但为了后续正常转换，这里需要添加）
			NodeData nodeData = new IterationNodeData((IterationNodeData) nodeIdMap.get(parentId).getData());
			nodeData.setVarName(nodeData.getVarName() + "_end");
			Node endNode = new Node();
			endNode.setData(nodeData).setType(NodeType.ITERATION_END).setParentId(parentId);
			nodes.add(endNode);

			// 计算每个节点的出度，出度为0的点将与迭代终止节点相连接
			subNodes.stream().map(Node::getId).filter(id -> !nodeIdHasOut.contains(id)).forEach(id -> {
				Edge newEdge = new Edge().setSource(id).setTarget(nodeData.getVarName());
				edges.add(newEdge);
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
		ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);

		Map<NodeType, Integer> counters = new HashMap<>();
		List<Node> nodes = new ArrayList<>();

		Map<Class<? extends NodeData>, BiConsumer<? super NodeData, Map<String, String>>> postProcessConsumers = new HashMap<>();

		for (Map<String, Object> nodeMap : nodeMaps) {
			@SuppressWarnings("unchecked")
			Map<String, Object> nodeDataMap = (Map<String, Object>) nodeMap.get("data");
			String difyNodeType = (String) nodeDataMap.get("type");
			if (difyNodeType == null || difyNodeType.isBlank()) {
				// This node is just a "note", skip it, and the corresponding node will
				// not be generated [compatible dify]
				continue;
			}
			String nodeId = (String) nodeMap.get("id");
			nodeDataMap.put("id", nodeId);
			// determine the type of dify node is supported yet
			NodeType nodeType = NodeType.fromDifyValue(difyNodeType)
				.orElseThrow(() -> new NotImplementedException("unsupported node type " + difyNodeType));

			// convert node map to workflow node using jackson
			nodeMap.remove("data");
			nodeMap.remove("type");
			Node node = objectMapper.convertValue(nodeMap, Node.class);
			// set title and desc
			String parentId = Optional.ofNullable(MapReadUtil.getMapDeepValue(nodeMap, String.class, "parentId"))
				.or(() -> Optional.ofNullable(MapReadUtil.getMapDeepValue(nodeDataMap, String.class, "iteration_id")))
				.orElse(null);
			node.setTitle((String) nodeDataMap.get("title"))
				.setDesc((String) nodeDataMap.get("desc"))
				.setParentId(parentId);

			// convert node data using specific WorkflowNodeDataConverter
			@SuppressWarnings("unchecked")
			NodeDataConverter<NodeData> converter = (NodeDataConverter<NodeData>) getNodeDataConverter(nodeType);

			NodeData data = converter.parseMapData(nodeDataMap, DSLDialectType.DIFY);

			// Generate a readable varName and inject it into NodeData
			int count = counters.merge(NodeType.isEmpty(nodeType) ? NodeType.EMPTY : nodeType, 1, Integer::sum);
			String varName = converter.generateVarName(count);

			data.setVarName(varName);

			// 获得处理输入变量名称的Consumer，当所有节点都处理完时使用
			postProcessConsumers.put(data.getClass(), converter.postProcessConsumer(DSLDialectType.DIFY));

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
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return edgeMaps.stream().map(edgeMap -> objectMapper.convertValue(edgeMap, Edge.class)).toList();
	}

	@Override
	public Map<String, Object> workflowToMap(Workflow workflow) {
		Map<String, Object> data = new HashMap<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Map<String, Object>> workflowVars = objectMapper.convertValue(workflow.getWorkflowVars(), List.class);
		List<Map<String, Object>> envVars = objectMapper.convertValue(workflow.getEnvVars(), List.class);
		Graph graph = workflow.getGraph();
		Map<String, Object> graphMap = deconstructGraph(graph);
		data.put("workflow",
				Map.of("conversation_variables", workflowVars, "environment_variables", envVars, "graph", graphMap));
		return data;
	}

	private Map<String, Object> deconstructGraph(Graph graph) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// deconstruct edge
		List<Map<String, Object>> edgeMaps = deconstructEdge(graph.getEdges());
		// deconstruct node
		List<Map<String, Object>> nodeMaps = deconstructNode(graph.getNodes());
		return Map.of("edges", edgeMaps, "nodes", nodeMaps);
	}

	private List<Map<String, Object>> deconstructEdge(List<Edge> edges) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return edges.stream().map(edge -> {
			Map<String, Object> edgeMap = objectMapper.convertValue(edge, new TypeReference<>() {
			});
			edgeMap.put("type", "custom");
			return edgeMap;
		}).toList();

	}

	private List<Map<String, Object>> deconstructNode(List<Node> nodes) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, Object>> nodeMaps = new ArrayList<>();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (Node node : nodes) {
			Map<String, Object> n = objectMapper.convertValue(node, new TypeReference<>() {
			});
			NodeType nodeType = node.getType();
			NodeDataConverter<? extends NodeData> nodeDataConverter = getNodeDataConverter(nodeType);
			Map<String, Object> nodeData = dumpMapData(nodeDataConverter, node.getData());
			nodeData.put("type", nodeType.difyValue());
			nodeData.put("title", node.getTitle());
			nodeData.put("desc", node.getDesc());
			n.put("data", nodeData);
			n.put("type", "custom");
			nodeMaps.add(n);
		}
		return nodeMaps;
	}

	private <T extends NodeData> Map<String, Object> dumpMapData(NodeDataConverter<T> converter, NodeData data) {
		return converter.dumpMapData((T) data, DSLDialectType.DIFY);
	}

	@Override
	public ChatBot mapToChatBot(Map<String, Object> data) {
		// TODO
		return null;
	}

	@Override
	public Map<String, Object> chatbotToMap(ChatBot chatBot) {
		// TODO
		return null;
	}

	@Override
	public Boolean supportDialect(DSLDialectType dialectType) {
		return DSLDialectType.DIFY.equals(dialectType);
	}

	private Variable convertToVariable(Map<String, Object> variableMap) {
		String name = String.join("_",
				Optional.ofNullable(MapReadUtil.safeCastToList(variableMap.get("selector"), String.class))
					.orElseThrow(() -> new IllegalArgumentException("Invalid variable selector")));
		String value = Optional.ofNullable(variableMap.get("value")).map(Object::toString).orElse(null);
		VariableType type = VariableType
			.fromDifyValue(Optional.ofNullable(variableMap.get("value_type"))
				.map(Object::toString)
				.orElse(VariableType.OBJECT.difyValue()))
			.orElse(VariableType.OBJECT);
		return new Variable(name, type).setValue(value);
	}

}
