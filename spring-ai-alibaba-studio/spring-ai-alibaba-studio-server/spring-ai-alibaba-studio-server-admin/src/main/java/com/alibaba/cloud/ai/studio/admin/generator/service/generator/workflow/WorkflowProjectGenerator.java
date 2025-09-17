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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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

	private static final String GRAPH_BUILDER_TEMPLATE_NAME = "GraphBuilder.java";

	private static final String GRAPH_BUILDER_STATE_SECTION = "stateSection";

	private static final String GRAPH_BUILDER_NODE_SECTION = "nodeSection";

	private static final String GRAPH_BUILDER_EDGE_SECTION = "edgeSection";

	private static final String GRAPH_BUILDER_IMPORT_SECTION = "importSection";

	private static final String GRAPH_BUILDER_ASSIST_METHOD_CODE = "assistMethodCode";

	private static final String GRAPH_RUN_TEMPLATE_NAME = "GraphRunController.java";

	private static final String PACKAGE_NAME = "packageName";

	private static final List<String> GRAPH_COMMON_IMPORTS = List.of("com.alibaba.cloud.ai.graph.CompiledGraph",
			"com.alibaba.cloud.ai.graph.KeyStrategy", "com.alibaba.cloud.ai.graph.OverAllState",
			"com.alibaba.cloud.ai.graph.StateGraph", "com.alibaba.cloud.ai.graph.action.AsyncEdgeAction",
			"com.alibaba.cloud.ai.graph.action.AsyncNodeAction", "com.alibaba.cloud.ai.graph.action.NodeAction",
			"com.alibaba.cloud.ai.graph.exception.GraphStateException", "org.springframework.ai.chat.client.ChatClient",
			"org.springframework.ai.chat.model.ChatModel", "org.springframework.context.annotation.Bean",
			"org.springframework.stereotype.Component", "java.util.HashMap", "java.util.Map", "java.util.List",
			"static com.alibaba.cloud.ai.graph.StateGraph.END", "static com.alibaba.cloud.ai.graph.StateGraph.START");

	private final List<DSLAdapter> dslAdapters;

	private final TemplateRenderer templateRenderer;

	private final Map<NodeType, NodeSection<? extends NodeData>> nodeSectionMap;

	public WorkflowProjectGenerator(List<DSLAdapter> dslAdapters,
			ObjectProvider<MustacheTemplateRenderer> templateRenderer,
			List<NodeSection<? extends NodeData>> nodeNodeSections) {
		this.dslAdapters = dslAdapters;
		this.templateRenderer = templateRenderer
			.getIfAvailable(() -> new MustacheTemplateRenderer("classpath:/templates"));
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

		String assistMethodCode = renderAssistMethodCode(nodes, projectDescription.getDslDialectType());
		String stateSectionStr = renderStateSections(
				Stream.of(workflow.getWorkflowVars(), workflow.getEnvVars()).flatMap(List::stream).toList());
		String nodeSectionStr = renderNodeSections(nodes, varNames);
		String edgeSectionStr = renderEdgeSections(workflow.getGraph().getEdges(), nodes, varNames);

		Map<String, Object> graphBuilderModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr, GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edgeSectionStr, GRAPH_BUILDER_IMPORT_SECTION, renderImportSection(workflow),
				GRAPH_BUILDER_ASSIST_METHOD_CODE, assistMethodCode);
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

	private String renderEdgeSections(List<Edge> edges, List<Node> nodes, Map<String, String> varNames) {
		// nodeVarName -> node的映射
		Map<String, Node> nodeMap = nodes.stream()
			.collect(Collectors.toMap(node -> node.getData().getVarName(), Function.identity()));

		// 根据source进行分组
		Map<String, List<Edge>> edgeGroup = edges.stream().collect(Collectors.groupingBy(Edge::getSource));

		StringBuilder sb = new StringBuilder();

		// 调用每一个source节点的renderEdges方法
		edgeGroup.forEach((varName, edgeList) -> {
			NodeType nodeType = nodeMap.get(varName).getType();
			@SuppressWarnings("unchecked")
			NodeSection<NodeData> section = (NodeSection<NodeData>) nodeSectionMap.get(nodeType);
			sb.append(section.renderEdges(nodeMap.get(varName).getData(), edgeList));
		});

		// 统一生成end节点到StateGraph.END的边（避免边重复）
		List<String> endNodeList = nodes.stream()
			.filter(node -> NodeType.END.equals(node.getType()))
			.map(Node::getId)
			.map(varNames::get)
			.toList();

		if (!endNodeList.isEmpty()) {
			sb.append(String.format("// Edges For [end]%n"));
			sb.append("stateGraph");
			endNodeList.forEach(endName -> sb.append(String.format("%n.addEdge(\"%s\", END)", endName)));
			sb.append(String.format(";%n"));
		}

		return sb.toString();
	}

	private String renderImportSection(Workflow workflow) {
		// construct a set of node types
		Set<NodeType> uniqueTypes = workflow.getGraph()
			.getNodes()
			.stream()
			.map(Node::getType)
			.collect(Collectors.toSet());

		if (uniqueTypes.isEmpty()) {
			return "";
		}

		List<String> commonImports = uniqueTypes.stream()
			.map(nodeSectionMap::get)
			.map(NodeSection::getImports)
			.flatMap(List::stream)
			.distinct()
			.toList();
		// 按照字典序升序排序，其中static开头的放在后面
		List<String> allImports = Stream.of(commonImports, GRAPH_COMMON_IMPORTS)
			.flatMap(List::stream)
			.distinct()
			.sorted(Comparator.comparing((String s) -> s.startsWith("static")).thenComparing(String::compareTo))
			.toList();

		StringBuilder sb = new StringBuilder();
		allImports.forEach(className -> sb.append("import ").append(className).append(";\n"));

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
