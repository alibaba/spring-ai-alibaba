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
package com.alibaba.cloud.ai.service.generator.workflow;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppModeEnum;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.workflow.Case;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.Edge;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import com.alibaba.cloud.ai.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.KnowledgeRetrievalNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.generator.GraphProjectDescription;
import com.alibaba.cloud.ai.service.generator.ProjectGenerator;
import com.google.common.base.CaseFormat;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import io.spring.initializr.generator.io.template.TemplateRenderer;
import io.spring.initializr.generator.project.ProjectDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkflowProjectGenerator implements ProjectGenerator {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(WorkflowProjectGenerator.class);

	private final String GRAPH_BUILDER_TEMPLATE_NAME = "GraphBuilder.java";

	private final String GRAPH_BUILDER_STATE_SECTION = "stateSection";

	private final String GRAPH_BUILDER_NODE_SECTION = "nodeSection";

	private final String GRAPH_BUILDER_EDGE_SECTION = "edgeSection";

	private final String GRAPH_BUILDER_START_INPUTS_SECTION = "startInputsSection";

	private final String GRAPH_BUILDER_IMPORT_SECTION = "importSection";

	private final String GRAPH_RUN_TEMPLATE_NAME = "GraphRunController.java";

	private final String PACKAGE_NAME = "packageName";

	private final String HAS_RETRIEVER = "hasRetriever";

	private final String HAS_CODE = "hasCode";

	private final DSLAdapter dslAdapter;

	private final TemplateRenderer templateRenderer;

	private final List<NodeSection> nodeNodeSections;

	public WorkflowProjectGenerator(@Qualifier("difyDSLAdapter") DSLAdapter dslAdapter,
			ObjectProvider<MustacheTemplateRenderer> templateRenderer, List<NodeSection> nodeNodeSections) {
		this.dslAdapter = dslAdapter;
		this.templateRenderer = templateRenderer
			.getIfAvailable(() -> new MustacheTemplateRenderer("classpath:/templates"));
		this.nodeNodeSections = nodeNodeSections;
	}

	@Override
	public Boolean supportAppMode(AppModeEnum appModeEnum) {
		return Objects.equals(appModeEnum, AppModeEnum.WORKFLOW);
	}

	@Override
	public void generate(GraphProjectDescription projectDescription, Path projectRoot) {
		App app = dslAdapter.importDSL(projectDescription.getDsl());
		Workflow workflow = (Workflow) app.getSpec();

		List<Node> nodes = workflow.getGraph().getNodes();
		Map<String, String> varNames = nodes.stream()
			.collect(Collectors.toMap(Node::getId, n -> n.getData().getVarName()));

		boolean hasRetriever = nodes.stream()
			.map(Node::getData)
			.anyMatch(nd -> nd instanceof KnowledgeRetrievalNodeData);

		boolean hasCode = nodes.stream().map(Node::getData).anyMatch(nd -> nd instanceof CodeNodeData);

		String stateSectionStr = renderStateSections(workflow.getWorkflowVars());
		String nodeSectionStr = renderNodeSections(nodes, varNames);
		String edgeSectionStr = renderEdgeSections(workflow.getGraph().getEdges(), nodes, varNames);

		Map<String, Object> graphBuilderModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr, GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edgeSectionStr, HAS_RETRIEVER, hasRetriever, GRAPH_BUILDER_IMPORT_SECTION,
				renderImportSection(workflow), HAS_CODE, hasCode);
		Map<String, Object> graphRunControllerModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_START_INPUTS_SECTION, renderStartInputSection(workflow));
		renderAndWriteTemplates(List.of(GRAPH_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
				List.of(graphBuilderModel, graphRunControllerModel), projectRoot, projectDescription);
	}

	@SuppressWarnings("unused")
	private Map<String, String> assignVariableNames(List<Node> nodes) {
		Map<NodeType, Integer> counter = new HashMap<>();
		Map<String, String> varNames = new HashMap<>();
		for (Node node : nodes) {
			NodeType type = NodeType.fromValue(node.getType()).orElseThrow();
			int idx = counter.merge(type, 1, Integer::sum);
			// generate similar questionClassifier1, http1, llm1, aggregator1, ...
			String base = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, type.name());
			String varName = base + idx;
			varNames.put(node.getId(), varName);
		}
		return varNames;
	}

	private String renderStateSections(List<Variable> overallStateVars) {
		if (overallStateVars == null || overallStateVars.isEmpty()) {
			return "";
		}
		String template = """
				() -> {
				  Map<String, KeyStrategy> strategies = new HashMap<>();
				  %s
				  return strategies;
				}
				""";

		String keyStrategies = overallStateVars.stream()
			.map(var -> String.format("strategies.put(\"%s\", (o1, o2) -> o2);", var.getName()))
			.collect(Collectors.joining("\n"));

		return String.format(template, keyStrategies);
	}

	private String renderNodeSections(List<Node> nodes, Map<String, String> varNames) {
		StringBuilder sb = new StringBuilder();
		for (Node node : nodes) {
			String varName = varNames.get(node.getId());
			NodeType nodeType = NodeType.fromValue(node.getType()).orElseThrow();
			for (NodeSection section : nodeNodeSections) {
				if (section.support(nodeType)) {
					sb.append(section.render(node, varName));
					break;
				}
			}
		}
		return sb.toString();
	}

	String renderEdgeSections(List<Edge> edges, List<Node> nodes, Map<String, String> varNames) {
		StringBuilder sb = new StringBuilder();
		Map<String, Node> nodeMap = nodes.stream().collect(Collectors.toMap(Node::getId, n -> n));

		// conditional edge set: sourceId -> List<Edge>
		Map<String, List<Edge>> conditionalEdgesMap = edges.stream()
			.filter(e -> e.getSourceHandle() != null && !"source".equals(e.getSourceHandle()))
			.collect(Collectors.groupingBy(Edge::getSource));

		// Set to track rendered edges to avoid duplicates
		Set<String> renderedEdges = new HashSet<>();

		// common edge
		for (Edge edge : edges) {
			String sourceId = edge.getSource();
			String targetId = edge.getTarget();
			String srcVar = varNames.get(sourceId);
			String tgtVar = varNames.get(targetId);
			Map<String, Object> data = edge.getData();
			String sourceType = data != null ? (String) data.get("sourceType") : null;
			String targetType = data != null ? (String) data.get("targetType") : null;

			// Skip if already rendered as conditional
			if (edge.getSourceHandle() != null && !"source".equals(edge.getSourceHandle())) {
				continue;
			}

			// 迭代节点作为边的终止点时直接使用节点ID，作为边的起始点时使用ID_out
			if (sourceType != null && sourceType.equalsIgnoreCase("iteration")) {
				srcVar += "_out";
			}

			String key = srcVar + "->" + tgtVar;
			if (renderedEdges.contains(key)) {
				continue;
			}
			renderedEdges.add(key);

			// START and END special handling
			if ("start".equals(sourceType)) {
				sb.append(String.format("stateGraph.addEdge(START, \"%s\");%n", tgtVar));
			}
			else if ("end".equals(targetType)) {
				sb.append(String.format("stateGraph.addEdge(\"%s\", END);%n", srcVar));
			}
			else {
				sb.append(String.format("stateGraph.addEdge(\"%s\", \"%s\");%n", srcVar, tgtVar));
			}
		}

		// conditional edge（aggregate by sourceId）
		for (Map.Entry<String, List<Edge>> entry : conditionalEdgesMap.entrySet()) {
			String sourceId = entry.getKey();
			String srcVar = varNames.get(sourceId);
			List<Edge> condEdges = entry.getValue();
			Node sourceNode = nodeMap.get(sourceId);
			NodeData sourceData = sourceNode.getData();

			List<String> conditions = new ArrayList<>();
			List<String> mappings = new ArrayList<>();

			if (sourceData instanceof BranchNodeData branchData) {
				for (Edge e : condEdges) {
					Map<String, Object> data = e.getData();
					String targetType = data != null ? (String) data.get("targetType") : null;
					String handleId = e.getSourceHandle();
					String tgtVar2 = varNames.get(e.getTarget());

					Case matchingCase = branchData.getCases()
						.stream()
						.filter(c -> c.getId().equals(handleId))
						.findFirst()
						.orElse(null);

					if (matchingCase != null && !matchingCase.getConditions().isEmpty()) {
						String conditionLogic = generateBranchConditionLogic(matchingCase);
						conditions.add(String.format("if (%s) return \"%s\";", conditionLogic, handleId));

						if ("end".equals(targetType)) {
							mappings.add(String.format("\"%s\", END", handleId));
						}
						else {
							mappings.add(String.format("\"%s\", \"%s\"", handleId, tgtVar2));
						}
					}
				}
			}
			else {
				for (Edge e : condEdges) {
					Map<String, Object> data = e.getData();
					String targetType = data != null ? (String) data.get("targetType") : null;
					String conditionKey = resolveConditionKey(sourceData, e.getSourceHandle());
					String tgtVar2 = varNames.get(e.getTarget());
					if ("end".equals(targetType)) {
						conditions.add(String.format("if (value.contains(\"%s\")) return \"%s\";", conditionKey,
								conditionKey));
						mappings.add(String.format("\"%s\", END", conditionKey));
						continue;
					}
					conditions
						.add(String.format("if (value.contains(\"%s\")) return \"%s\";", conditionKey, conditionKey));
					mappings.add(String.format("\"%s\", \"%s\"", conditionKey, tgtVar2));
				}
			}

			String lambdaContent = String.join("\n", conditions);
			String mapContent = String.join(", ", mappings);

			String stateAccessCode;
			if (sourceData instanceof BranchNodeData) {
				stateAccessCode = "";
				// nodes
			}
			else {
				stateAccessCode = String
					.format("String value = state.value(\"%s_output\", String.class).orElse(\"\");%n", srcVar);
			}

			sb.append(String.format(
					"stateGraph.addConditionalEdges(\"%s\",%n" + "            edge_async(state -> {%n" + "%s%s%n"
							+ "return null;%n" + "            }),%n" + "            Map.of(%s)%n" + ");%n",
					srcVar, stateAccessCode, lambdaContent, mapContent));
		}

		return sb.toString();
	}

	private String resolveConditionKey(NodeData data, String handleId) {
		if (data instanceof QuestionClassifierNodeData classifier) {
			return classifier.getClasses()
				.stream()
				.filter(c -> c.getId().equals(handleId))
				.map(QuestionClassifierNodeData.ClassConfig::getText)
				.findFirst()
				.orElse(handleId);
		}
		// todo: extend to other node types that support conditional edges
		return handleId;
	}

	private String renderStartInputSection(Workflow workflow) {
		List<Variable> startInputs = workflow.getWorkflowVars()
			.stream()
			.filter(v -> workflow.getGraph()
				.getNodes()
				.stream()
				.anyMatch(n -> n.getData() instanceof StartNodeData
						&& ((StartNodeData) n.getData()).getStartInputs() != null
						&& ((StartNodeData) n.getData()).getStartInputs()
							.stream()
							.anyMatch(i -> i.getVariable().equals(v.getName()))))
			.toList();

		if (startInputs.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Map<String, Object> startInputs = new HashMap<>();\n");
		for (Variable var : startInputs) {
			sb.append(String.format("startInputs.put(\"%s\", inputs.get(\"%s\")); // %s%n", var.getName(),
					var.getName(), var.getDescription()));
		}
		sb.append("return graph.invoke(startInputs).get().data();\n");

		return sb.toString();
	}

	private String renderImportSection(Workflow workflow) {
		// construct a list of node types
		Map<String, String> nodeTypeToClass = Map.ofEntries(
				Map.entry(NodeType.ANSWER.difyValue(), "com.alibaba.cloud.ai.graph.node.AnswerNode"),
				Map.entry(NodeType.CODE.difyValue(), "com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction;"),
				Map.entry(NodeType.LLM.difyValue(), "com.alibaba.cloud.ai.graph.node.LlmNode"),
				Map.entry(NodeType.BRANCH.value(), "com.alibaba.cloud.ai.graph.node.BranchNode"),
				Map.entry(NodeType.DOC_EXTRACTOR.difyValue(), "com.alibaba.cloud.ai.graph.node.DocumentExtractorNode"),
				Map.entry(NodeType.HTTP.difyValue(), "com.alibaba.cloud.ai.graph.node.HttpNode"),
				Map.entry(NodeType.LIST_OPERATOR.difyValue(), "com.alibaba.cloud.ai.graph.node.ListOperatorNode"),
				Map.entry(NodeType.QUESTION_CLASSIFIER.difyValue(),
						"com.alibaba.cloud.ai.graph.node.QuestionClassifierNode"),
				Map.entry(NodeType.PARAMETER_PARSING.difyValue(),
						"com.alibaba.cloud.ai.graph.node.ParameterParsingNode"),
				Map.entry(NodeType.TEMPLATE_TRANSFORM.difyValue(),
						"com.alibaba.cloud.ai.graph.node.TemplateTransformNode"),
				Map.entry(NodeType.TOOL.difyValue(), "com.alibaba.cloud.ai.graph.node.ToolNode"),
				Map.entry(NodeType.KNOWLEDGE_RETRIEVAL.difyValue(),
						"com.alibaba.cloud.ai.graph.node.KnowledgeRetrievalNode"),
				Map.entry(NodeType.VARIABLE_AGGREGATOR.difyValue(),
						"com.alibaba.cloud.ai.graph.node.VariableAggregatorNode"),
				Map.entry(NodeType.ITERATION.difyValue(), "com.alibaba.cloud.ai.graph.node.IterationNode"));

		Set<String> uniqueTypes = workflow.getGraph()
			.getNodes()
			.stream()
			.map(Node::getType)
			.filter(nodeTypeToClass::containsKey)
			.collect(Collectors.toSet());

		if (uniqueTypes.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (String type : uniqueTypes) {
			String className = nodeTypeToClass.get(type);
			if (type.equals(NodeType.BRANCH.value()) || type.equals(NodeType.QUESTION_CLASSIFIER.difyValue())) {
				sb.append("import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;\n");
			}
			sb.append("import ").append(className).append(";\n");
		}

		return sb.toString();
	}

	private void renderAndWriteTemplates(List<String> templateNames, List<Map<String, Object>> models, Path projectRoot,
			ProjectDescription projectDescription) {
		// todo: may to standardize the code format via the IdentifierGeneratorFactory
		Path fileRoot = createDirectory(projectRoot, projectDescription);
		for (int i = 0; i < templateNames.size(); i++) {
			String templateName = templateNames.get(i);
			String template;
			try {
				template = templateRenderer.render(templateName, models.get(i));
			}
			catch (IOException e) {
				throw new RuntimeException("Got error when rendering template" + templateName, e);
			}
			Path file;
			try {
				file = Files.createFile(fileRoot.resolve(templateName));
			}
			catch (IOException e) {
				throw new RuntimeException("Got error when creating file", e);
			}
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
				writer.print(template);
			}
			catch (IOException e) {
				throw new RuntimeException("Got error when writing template " + templateName, e);
			}
		}
	}

	private Path createDirectory(Path projectRoot, ProjectDescription projectDescription) {
		StringBuilder pathBuilder = new StringBuilder("src/main/").append(projectDescription.getLanguage().id());
		String packagePath = projectDescription.getPackageName().replace('.', '/');
		pathBuilder.append("/").append(packagePath).append("/graph/");
		Path fileRoot;
		try {
			fileRoot = Files.createDirectories(projectRoot.resolve(pathBuilder.toString()));
		}
		catch (Exception e) {
			throw new RuntimeException("Got error when creating files", e);
		}
		return fileRoot;
	}

	String generateBranchConditionLogic(Case caseData) {
		List<Case.Condition> conditions = caseData.getConditions();
		if (conditions == null || conditions.isEmpty()) {
			return "false";
		}

		List<String> conditionStrings = new ArrayList<>();
		for (Case.Condition condition : conditions) {
			String conditionStr = generateSingleCondition(condition);
			if (conditionStr != null && !conditionStr.trim().isEmpty()) {
				conditionStrings.add(conditionStr);
			}
		}

		if (conditionStrings.isEmpty()) {
			return "false";
		}

		String logicalOperator = caseData.getLogicalOperator() != null ? caseData.getLogicalOperator().getValue()
				: "&&";
		String joinOperator = "or".equalsIgnoreCase(logicalOperator) || "||".equals(logicalOperator) ? " || " : " && ";

		return "(" + String.join(joinOperator, conditionStrings) + ")";
	}

	String generateSingleCondition(Case.Condition condition) {
		if (condition.getVariableSelector() == null) {
			return "false";
		}

		String varName = condition.getVariableSelector().getName();
		String varType = condition.getVarType();
		String comparisonOperator = condition.getComparisonOperator() != null
				? condition.getComparisonOperator().getValue() : "equals";
		String value = condition.getValue();

		String javaType;
		String defaultValue;
		String stateAccessTemplate;

		switch (varType != null ? varType.toLowerCase() : "string") {
			case "string":
				javaType = "String.class";
				defaultValue = "\"\"";
				stateAccessTemplate = "state.value(\"%s\", %s).orElse(%s)";
				break;
			case "number":
			case "integer":
				javaType = "Integer.class";
				defaultValue = "0";
				stateAccessTemplate = "state.value(\"%s\", %s).orElse(%s)";
				break;
			case "float":
			case "double":
				javaType = "Double.class";
				defaultValue = "0.0";
				stateAccessTemplate = "state.value(\"%s\", %s).orElse(%s)";
				break;
			case "boolean":
				javaType = "Boolean.class";
				defaultValue = "false";
				stateAccessTemplate = "state.value(\"%s\", %s).orElse(%s)";
				break;
			case "file":
			case "object":
			case "list":
			case "array":
				// comparison
				javaType = "Object.class";
				defaultValue = "null";
				stateAccessTemplate = "String.valueOf(state.value(\"%s\", %s).orElse(%s))";
				break;
			default:
				// Default to Object for unknown types
				javaType = "Object.class";
				defaultValue = "null";
				stateAccessTemplate = "String.valueOf(state.value(\"%s\", %s).orElse(%s))";
				break;
		}

		String stateAccess = String.format(stateAccessTemplate, varName, javaType, defaultValue);

		return generateComparison(stateAccess, comparisonOperator, value, varType);
	}

	String generateComparison(String leftSide, String operator, String value, String varType) {
		if (operator == null || value == null) {
			return "false";
		}

		String escapedValue = value.replace("\"", "\\\"");

		switch (operator.toLowerCase()) {
			case "equals":
			case "==":
			case "=":
			case "is": // Add support for "is" operator used in file type comparisons
				if ("string".equalsIgnoreCase(varType)) {
					return String.format("Objects.equals(%s, \"%s\")", leftSide, escapedValue);
				}
				else if ("file".equalsIgnoreCase(varType) || "object".equalsIgnoreCase(varType)
						|| "list".equalsIgnoreCase(varType) || "array".equalsIgnoreCase(varType)) {
					return String.format("%s.equals(\"%s\")", leftSide, escapedValue);
				}
				else {
					return String.format("Objects.equals(%s, %s)", leftSide, formatValueForType(value, varType));
				}
			case "not_equals":
			case "!=":
			case "not equals":
				if ("string".equalsIgnoreCase(varType)) {
					return String.format("!Objects.equals(%s, \"%s\")", leftSide, escapedValue);
				}
				else if ("file".equalsIgnoreCase(varType) || "object".equalsIgnoreCase(varType)
						|| "list".equalsIgnoreCase(varType) || "array".equalsIgnoreCase(varType)) {
					return String.format("!%s.equals(\"%s\")", leftSide, escapedValue);
				}
				else {
					return String.format("!Objects.equals(%s, %s)", leftSide, formatValueForType(value, varType));
				}
			case "contains":
				return String.format("%s.toString().contains(\"%s\")", leftSide, escapedValue);
			case "not_contains":
			case "not contains":
				return String.format("!%s.toString().contains(\"%s\")", leftSide, escapedValue);
			case "starts_with":
			case "startswith":
				return String.format("%s.toString().startsWith(\"%s\")", leftSide, escapedValue);
			case "ends_with":
			case "endswith":
				return String.format("%s.toString().endsWith(\"%s\")", leftSide, escapedValue);
			case "greater_than":
			case ">":
			case "gt":
				return String.format("Double.parseDouble(%s.toString()) > %s", leftSide,
						formatValueForType(value, "number"));
			case "less_than":
			case "<":
			case "lt":
				return String.format("Double.parseDouble(%s.toString()) < %s", leftSide,
						formatValueForType(value, "number"));
			case "greater_than_or_equal":
			case ">=":
			case "gte":
				return String.format("Double.parseDouble(%s.toString()) >= %s", leftSide,
						formatValueForType(value, "number"));
			case "less_than_or_equal":
			case "<=":
			case "lte":
				return String.format("Double.parseDouble(%s.toString()) <= %s", leftSide,
						formatValueForType(value, "number"));
			default:
				return String.format("Objects.equals(%s.toString(), \"%s\")", leftSide, escapedValue);
		}
	}

	String formatValueForType(String value, String varType) {
		if (value == null) {
			return "null";
		}

		switch (varType != null ? varType.toLowerCase() : "string") {
			case "string":
				return "\"" + value.replace("\"", "\\\"") + "\"";
			case "number":
			case "integer":
			case "float":
			case "double":
				try {
					Double.parseDouble(value);
					return value;
				}
				catch (NumberFormatException e) {
					return "0";
				}
			case "boolean":
				return Boolean.parseBoolean(value) ? "true" : "false";
			default:
				return "\"" + value.replace("\"", "\\\"") + "\"";
		}
	}

}
