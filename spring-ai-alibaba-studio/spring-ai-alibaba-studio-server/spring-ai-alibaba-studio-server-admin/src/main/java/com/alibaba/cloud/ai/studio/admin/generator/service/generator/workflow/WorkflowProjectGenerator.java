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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppModeEnum;
import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.GraphProjectDescription;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.ProjectGenerator;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ContributorFileUtil;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import io.spring.initializr.generator.io.template.TemplateRenderer;
import io.spring.initializr.generator.project.ProjectDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class WorkflowProjectGenerator implements ProjectGenerator {

	private static final Logger log = LoggerFactory.getLogger(WorkflowProjectGenerator.class);

	private final String GRAPH_BUILDER_TEMPLATE_NAME = "GraphBuilder.java";

	private final String GRAPH_BUILDER_STATE_SECTION = "stateSection";

	private final String GRAPH_BUILDER_NODE_SECTION = "nodeSection";

	private final String GRAPH_BUILDER_EDGE_SECTION = "edgeSection";

	private final String GRAPH_BUILDER_IMPORT_SECTION = "importSection";

	private final String GRAPH_BUILDER_ASSIST_METHOD_CODE = "assistMethodCode";

	private final String GRAPH_RUN_TEMPLATE_NAME = "GraphRunController.java";

	private final String PACKAGE_NAME = "packageName";

	private final String HAS_CODE = "hasCode";

	private final List<DSLAdapter> dslAdapters;

	private final TemplateRenderer templateRenderer;

	private final List<NodeSection<? extends NodeData>> nodeNodeSections;

	private final Map<NodeType, NodeSection<? extends NodeData>> nodeSectionMap;

	public WorkflowProjectGenerator(List<DSLAdapter> dslAdapters,
			ObjectProvider<MustacheTemplateRenderer> templateRenderer,
			List<NodeSection<? extends NodeData>> nodeNodeSections) {
		this.dslAdapters = dslAdapters;
		this.templateRenderer = templateRenderer
			.getIfAvailable(() -> new MustacheTemplateRenderer("classpath:/templates"));
		this.nodeNodeSections = nodeNodeSections;
		this.nodeSectionMap = nodeNodeSections.stream().map(nodeSection -> {
			List<NodeType> nodeTypeList = Arrays.stream(NodeType.values()).filter(nodeSection::support).toList();
			if (nodeTypeList.isEmpty()) {
				return null;
			}
			return Map.entry(nodeTypeList.get(0), nodeSection);
		})
			.filter(Objects::nonNull)
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
	}

	@Override
	public Boolean supportAppMode(AppModeEnum appModeEnum) {
		return Objects.equals(appModeEnum, AppModeEnum.WORKFLOW);
	}

	@Override
	public void generate(GraphProjectDescription projectDescription, Path projectRoot) {
		DSLAdapter dslAdapter = dslAdapters.stream()
			.filter(t -> t.supportDialect(projectDescription.getDslDialectType()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException(
					"No DSL adapter found for dialect: " + projectDescription.getDslDialectType()));
		App app = dslAdapter.importDSL(projectDescription.getDsl());
		Workflow workflow = (Workflow) app.getSpec();

		List<Node> nodes = workflow.getGraph().getNodes();
		Map<String, String> varNames = nodes.stream()
			.collect(Collectors.toMap(Node::getId, n -> n.getData().getVarName()));

		boolean hasCode = nodes.stream().map(Node::getData).anyMatch(nd -> nd instanceof CodeNodeData);

		String assistMethodCode = renderAssistMethodCode(nodes, projectDescription.getDslDialectType());
		String stateSectionStr = renderStateSections(
				Stream.of(workflow.getWorkflowVars(), workflow.getEnvVars()).flatMap(List::stream).toList());
		String nodeSectionStr = renderNodeSections(nodes, varNames);
		String edgeSectionStr = renderEdgeSections(workflow.getGraph().getEdges(), nodes, varNames);

		Map<String, Object> graphBuilderModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr, GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edgeSectionStr, GRAPH_BUILDER_IMPORT_SECTION, renderImportSection(workflow),
				HAS_CODE, hasCode, GRAPH_BUILDER_ASSIST_METHOD_CODE, assistMethodCode);
		Map<String, Object> graphRunControllerModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());
		renderAndWriteTemplates(List.of(GRAPH_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
				List.of(graphBuilderModel, graphRunControllerModel), projectRoot, projectDescription);

		// 生成需要的资源文件
		this.generateResourceFiles(projectRoot,
				nodes.stream()
					.map(node -> Map.entry(node.getType(), node.getData()))
					.map(e -> Map.entry((NodeSection<NodeData>) nodeSectionMap.get(e.getKey()), e.getValue()))
					.map(e -> e.getKey().resourceFiles(projectDescription.getDslDialectType(), e.getValue()))
					.flatMap(List::stream)
					.toList());
	}

	private void generateResourceFiles(Path projectRoot, List<NodeSection.ResourceFile> resourceFiles) {
		resourceFiles.forEach(resourceFile -> {
			try (InputStream inputStream = resourceFile.inputStreamSupplier().get()) {
				ContributorFileUtil.saveResourceFile(projectRoot, resourceFile.fileName(), inputStream);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private String renderAssistMethodCode(List<Node> nodes, DSLDialectType dialectType) {
		StringBuilder sb = new StringBuilder();
		nodes.stream().map(Node::getType).distinct().map(nodeSectionMap::get).forEach(section -> {
			sb.append(section.assistMethodCode(dialectType));
			sb.append(String.format("%n"));
		});
		return sb.toString();
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
			.map(var -> String.format("strategies.put(\"%s\", %s);", var.getName(),
					Optional.ofNullable(var.getVariableStrategy()).orElse(Variable.Strategy.REPLACE).getCode()))
			.collect(Collectors.joining("\n"));

		return String.format(template, keyStrategies);
	}

	private String renderNodeSections(List<Node> nodes, Map<String, String> varNames) {
		StringBuilder sb = new StringBuilder();
		for (Node node : nodes) {
			String varName = varNames.get(node.getId());
			NodeType nodeType = node.getType();
			NodeSection<? extends NodeData> section = nodeSectionMap.get(nodeType);
			sb.append(section.render(node, varName));
		}
		return sb.toString();
	}

	// TODO: 目前这里渲染edge的逻辑与Dify转换高度耦合，需要优化
	private String renderEdgeSections(List<Edge> edges, List<Node> nodes, Map<String, String> varNames) {
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

			Node sourceNode = nodeMap.get(sourceId);
			NodeType sourceType = sourceNode != null ? sourceNode.getType() : null;

			// Skip if already rendered as conditional
			if (edge.getSourceHandle() != null && !"source".equals(edge.getSourceHandle()) && edge.isDify()) {
				continue;
			}

			// 迭代节点作为边的终止点时直接使用节点ID，作为边的起始点时使用ID_out
			// todo: 修改迭代节点终止ID，防止与变量冲突（Dify不冲突）
			if (sourceType != null && NodeType.ITERATION.equals(sourceType) && edge.isDify()) {
				srcVar += "_out";
			}

			String key = srcVar + "->" + tgtVar;
			if (renderedEdges.contains(key)) {
				continue;
			}
			renderedEdges.add(key);

			// START and END special handling
			if (NodeType.START.equals(sourceType)) {
				sb.append(String.format("stateGraph.addEdge(START, \"%s\");%n", tgtVar));
			}
			else {
				sb.append(String.format("stateGraph.addEdge(\"%s\", \"%s\");%n", srcVar, tgtVar));
			}
		}

		// conditional edge（aggregate by sourceId）
		for (Map.Entry<String, List<Edge>> entry : conditionalEdgesMap.entrySet()) {
			String nodeId = entry.getKey();
			Node node = nodeMap.get(nodeId);
			NodeType nodeType = node.getType();
			for (NodeSection section : nodeNodeSections) {
				if (section.support(nodeType)) {
					String edgeCode = section.renderConditionalEdges(node.getData(), nodeMap, entry, varNames);
					sb.append(edgeCode);
				}
			}
		}

		// 统一生成end节点到StateGraph.END的边（避免边重复）
		List<String> endNodeList = nodes.stream()
			.filter(node -> NodeType.END.equals(node.getType()))
			.map(Node::getId)
			.map(varNames::get)
			.toList();

		if (!endNodeList.isEmpty()) {
			sb.append("stateGraph");
			endNodeList.forEach(endName -> sb.append(String.format("%n.addEdge(\"%s\", END)", endName)));
			sb.append(String.format(";%n"));
		}

		return sb.toString();
	}

	private String renderImportSection(Workflow workflow) {
		// construct a list of node types
		Map<NodeType, List<String>> nodeTypeToClass = Map.ofEntries(
				Map.entry(NodeType.ANSWER, List.of("com.alibaba.cloud.ai.graph.node.AnswerNode")),
				Map.entry(NodeType.CODE, List.of("com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction",
						"com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig",
						"com.alibaba.cloud.ai.graph.node.code.CodeExecutor",
						"com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor", "java.io.IOException",
						"java.nio.file.Files", "java.nio.file.Path", "java.util.stream.Collectors")),
				Map.entry(NodeType.AGENT,
						List.of("com.alibaba.cloud.ai.graph.node.AgentNode",
								"org.springframework.ai.tool.ToolCallback")),
				Map.entry(NodeType.LLM,
						List.of("org.springframework.ai.chat.messages.Message",
								"org.springframework.ai.chat.messages.AssistantMessage",
								"org.springframework.ai.chat.messages.MessageType",
								"org.springframework.ai.chat.messages.SystemMessage",
								"org.springframework.ai.chat.messages.UserMessage",
								"com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions",
								"org.springframework.beans.factory.annotation.Autowired", "java.util.Optional")),
				Map.entry(NodeType.BRANCH,
						List.of("com.alibaba.cloud.ai.graph.node.BranchNode",
								"static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async")),
				Map.entry(NodeType.DOC_EXTRACTOR, List.of("com.alibaba.cloud.ai.graph.node.DocumentExtractorNode")),
				Map.entry(NodeType.HTTP,
						List.of("com.alibaba.cloud.ai.graph.node.HttpNode", "org.springframework.http.HttpMethod")),
				Map.entry(NodeType.LIST_OPERATOR,
						List.of("com.alibaba.cloud.ai.graph.node.ListOperatorNode", "java.util.Comparator")),
				Map.entry(NodeType.QUESTION_CLASSIFIER,
						List.of("com.alibaba.cloud.ai.graph.node.QuestionClassifierNode",
								"static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async")),
				Map.entry(NodeType.PARAMETER_PARSING,
						List.of("com.alibaba.cloud.ai.graph.node.ParameterParsingNode", "java.util.stream.Collectors")),
				Map.entry(NodeType.TEMPLATE_TRANSFORM,
						List.of("com.alibaba.cloud.ai.graph.node.TemplateTransformNode")),
				Map.entry(NodeType.TOOL,
						List.of("com.alibaba.cloud.ai.graph.node.ToolNode", "java.util.function.Function",
								"org.springframework.ai.tool.function.FunctionToolCallback")),
				Map.entry(NodeType.RETRIEVER, List.of("com.alibaba.cloud.ai.graph.node.KnowledgeRetrievalNode",
						"org.springframework.ai.embedding.EmbeddingModel", "org.springframework.ai.reader.TextReader",
						"org.springframework.ai.transformer.splitter.TokenTextSplitter",
						"org.springframework.ai.vectorstore.SimpleVectorStore",
						"org.springframework.ai.vectorstore.VectorStore",
						"org.springframework.beans.factory.annotation.Value", "org.springframework.core.io.Resource",
						"org.springframework.ai.document.Document",
						"org.springframework.beans.factory.annotation.Autowired",
						"org.springframework.core.io.ResourceLoader", "java.util.Optional")),
				Map.entry(NodeType.AGGREGATOR,
						List.of("com.alibaba.cloud.ai.graph.node.VariableAggregatorNode",
								"java.util.stream.Collectors")),
				Map.entry(NodeType.ASSIGNER, List.of("com.alibaba.cloud.ai.graph.node.AssignerNode")),
				Map.entry(NodeType.ITERATION, List.of("com.alibaba.cloud.ai.graph.node.IterationNode")),
				Map.entry(NodeType.END, List.of("java.util.stream.Stream", "java.util.stream.Collectors",
						"org.springframework.ai.chat.prompt.PromptTemplate")));

		Set<NodeType> uniqueTypes = workflow.getGraph()
			.getNodes()
			.stream()
			.map(Node::getType)
			.filter(nodeTypeToClass::containsKey)
			.collect(Collectors.toSet());

		if (uniqueTypes.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		uniqueTypes.stream()
			.map(nodeTypeToClass::get)
			.flatMap(List::stream)
			.distinct()
			.forEach(className -> sb.append("import ").append(className).append(";\n"));

		return sb.toString();
	}

	private void renderAndWriteTemplates(List<String> templateNames, List<Map<String, Object>> models, Path projectRoot,
			ProjectDescription projectDescription) {
		// todo: may to standardize the code format via the IdentifierGeneratorFactory
		Path fileRoot = ContributorFileUtil.createDirectory(projectRoot, projectDescription);
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

}
