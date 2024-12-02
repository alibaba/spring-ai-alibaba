package com.alibaba.cloud.ai.service.impl.dsl;

import com.alibaba.cloud.ai.common.exception.InvalidParamException;
import com.alibaba.cloud.ai.model.app.AppMetadata;
import com.alibaba.cloud.ai.model.app.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.app.workflow.*;
import com.alibaba.cloud.ai.store.AppSaver;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DifyDSLAdapter extends AbstractDSLAdapter {

	private static final String DIFY_DIALECT = "dify";

	private static final String[] DIFY_CHATBOT_MODES = { "chat", "completion", "agent-chat" };

	private static final String[] DIFY_WORKFLOW_MODES = { "workflow", "advanced-chat" };

	public DifyDSLAdapter(AppSaver appSaver) {
		super(appSaver);
	}

	@Override
	public void validateDSLData(Map<String, Object> dslData) {
		if (dslData == null || !dslData.containsKey("app")) {
			throw new InvalidParamException("invalid dify dsl");
		}
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
			throw new InvalidParamException("unknown dify app mode" + map.get("mode"));
		}
		metadata.setId(UUID.randomUUID().toString());
		metadata.setName((String) map.getOrDefault("name", metadata.getMode() + "-" + metadata.getId()));
		metadata.setDescription((String) map.getOrDefault("description", ""));
		return metadata;
	}

	@Override
	public Workflow mapToWorkflow(Map<String, Object> data) {
		Workflow workflow = new Workflow();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// map key is snake_case style
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		if (data.containsKey("conversation_variables")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("conversation_variables");
			List<Variable> workflowVars = new ArrayList<>(variables.size());
			variables.forEach(variable -> workflowVars.add(objectMapper.convertValue(variable, Variable.class)));
			workflow.setWorkflowVars(workflowVars);
		}
		if (data.containsKey("environment_variables")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("environment_variables");
			List<Variable> envVars = new ArrayList<>(variables.size());
			variables.forEach(variable -> envVars.add(objectMapper.convertValue(variable, Variable.class)));
			workflow.setEnvVars(envVars);
		}
		if (!data.containsKey("graph")) {
			throw new InvalidParamException(
					"invalid dify dsl: specified mode is advanced-chat or workflow, but no 'graph' found");
		}
		workflow.setGraph(constructGraph((Map<String, Object>) data.get("graph")));
		return workflow;
	}

	private WorkflowGraph constructGraph(Map<String, Object> data) {
		WorkflowGraph graph = new WorkflowGraph();
		List<WorkflowEdge> workflowEdges = new ArrayList<>();
		List<WorkflowNode> workflowNodes = new ArrayList<>();
		List<WorkflowNode> branchNodes = new ArrayList<>();
		Map<String, WorkflowEdge> branchEdges = new HashMap<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// convert nodes
		if (data.containsKey("nodes")) {
			List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
			for (Map<String, Object> node : nodes) {
				WorkflowNode n = objectMapper.convertValue(node, WorkflowNode.class);
				// collect if-else node
				if (n.getData().get("type").equals("if-else")) {
					branchNodes.add(n);
				}
				else {
					WorkflowNodeType nodeType = WorkflowNodeType.difyValueOf((String) n.getData().get("type"));
					if (nodeType == null) {
						throw new InvalidParamException(
								"invalid dify dsl: unsupported node type " + n.getData().get("type"));
					}
					n.setType(nodeType.value());
					workflowNodes.add(n);
				}
			}
		}
		// convert edges
		if (data.containsKey("edges")) {
			List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
			for (Map<String, Object> edge : edges) {
				WorkflowEdge workflowEdge = objectMapper.convertValue(edge, WorkflowEdge.class);
				if (edge.get("sourceHandle").equals("source")) {
					workflowEdge.setType(WorkflowEdgeType.DIRECT.value());
					workflowEdges.add(workflowEdge);
				}
				else {
					// collect if-else edges
					branchEdges.put((String) edge.get("sourceHandle"), workflowEdge);
				}
			}
		}
		// convert if-else node to condition edge
		// objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		for (WorkflowNode node : branchNodes) {
			List<Case> cases = new ArrayList<>();
			Map<String, String> targetMap = new HashMap<>();
			List<Map<String, Object>> casesData = (List<Map<String, java.lang.Object>>) node.getData().get("cases");
			for (Map<String, Object> caseData : casesData) {
				Case c = objectMapper.convertValue(caseData, Case.class);
				WorkflowEdge edge = branchEdges.get(c.getId());
				targetMap.put(c.getId(), edge.getTarget());
				cases.add(c);
			}
			WorkflowEdge conditionEdge = new WorkflowEdge().setId(node.getId())
				.setType(WorkflowEdgeType.CONDITION.value())
				.setSource(node.getId())
				.setCases(cases)
				.setTargetMap(targetMap)
				.setZIndex(0);
			workflowEdges.add(conditionEdge);
		}

		graph.setNodes(workflowNodes);
		graph.setEdges(workflowEdges);
		return graph;
	}

	@Override
	public Map<String, Object> workflowToMap(Workflow workflow) {
		Map<String, Object> data = new HashMap<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Map<String, Object>> workflowVars = objectMapper.convertValue(workflow.getWorkflowVars(), List.class);
		List<Map<String, Object>> envVars = objectMapper.convertValue(workflow.getEnvVars(), List.class);
		data.put("conversation_variables", workflowVars);
		data.put("environment_variables", envVars);

		WorkflowGraph graph = workflow.getGraph();
		Map<String, Object> graphData = deconstructGraph(graph);
		data.put("graph", graphData);
		return data;
	}

	private Map<String, Object> deconstructGraph(WorkflowGraph graph) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Map<String, Object>> edgesData = new ArrayList<>();
		List<Map<String, Object>> nodesData = new ArrayList<>();
		for (WorkflowEdge edge : graph.getEdges()) {
			if (edge.getType().equals(WorkflowEdgeType.DIRECT.value())) {
				Map<String, Object> e = objectMapper.convertValue(edge, Map.class);
				e.put("sourceHandle", "source");
				e.put("targetHandle", "target");
				e.put("type", "custom");
				edgesData.add(e);
				continue;
			}
			Map<String, String> targetMap = edge.getTargetMap();
			for (Case c : edge.getCases()) {
				Map<String, Object> e = new HashMap<>();
				e.put("source", edge.getSource());
				e.put("sourceHandle", c.getId());
				e.put("target", targetMap.get(c.getId()));
				e.put("targetHandle", "target");
				e.put("type", "custom");
				e.put("zIndex", 0);
				e.put("selected", false);
				edgesData.add(e);
			}
			Map<String, Object> n = new HashMap<>();
			n.put("id", edge.getId());
			n.put("type", "custom");
			n.put("width", 250);
			n.put("height", 250);
			n.put("data", Map.of("cases", objectMapper.convertValue(edge.getCases(), List.class), "desc", "",
					"selected", false, "title", "conditional edge", "type", "if-else"));
			nodesData.add(n);

		}

		for (WorkflowNode node : graph.getNodes()) {
			Map<String, Object> n = objectMapper.convertValue(node, Map.class);
			WorkflowNodeType nodeType = WorkflowNodeType.valueOf(node.getType());
			n.put("type", "custom");
			Map<String, Object> nodeData = (Map<String, Object>) n.getOrDefault("data", new HashMap<String, Object>());
			nodeData.put("type", nodeType.difyValue());
			n.put("data", nodeData);
			nodesData.add(n);
		}
		return Map.of("edges", edgesData, "nodes", nodesData);
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
	public Boolean supportDialect(String dialect) {
		return DIFY_DIALECT.equals(dialect);
	}

}
