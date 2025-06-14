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
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
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

	private static final Logger log = LoggerFactory.getLogger(WorkflowProjectGenerator.class);

	private final String GRAPH_BUILDER_TEMPLATE_NAME = "GraphBuilder.java";

	private final String GRAPH_BUILDER_STATE_SECTION = "stateSection";

	private final String GRAPH_BUILDER_NODE_SECTION = "nodeSection";

	private final String GRAPH_BUILDER_EDGE_SECTION = "edgeSection";

	private final String GRAPH_RUN_TEMPLATE_NAME = "GraphRunController.java";

	private final String PACKAGE_NAME = "packageName";

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
		Map<String, String> varNames = assignVariableNames(nodes);

		String stateSectionStr = renderStateSections(workflow.getWorkflowVars());
		String nodeSectionStr = renderNodeSections(nodes, varNames);
		String edgeSectionStr = renderEdgeSections(workflow.getGraph().getEdges(), nodes);

		Map<String, String> graphBuilderModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr, GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edgeSectionStr);
		Map<String, String> graphRunControllerModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());
		renderAndWriteTemplates(List.of(GRAPH_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
				List.of(graphBuilderModel, graphRunControllerModel), projectRoot, projectDescription);
	}

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
		// todo: update create overAllState by newest saa graph
		return overallStateVars.stream()
			.map(var -> String.format("            overAllState.registerKeyAndStrategy(\"%s\", (o1, o2) -> o2);%n",
					var.getName()))
			.collect(Collectors.joining());
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

	private String renderEdgeSections(List<Edge> edges, List<Node> nodes) {
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
			Map<String, Object> data = edge.getData();
			String sourceType = data != null ? (String) data.get("sourceType") : null;
			String targetType = data != null ? (String) data.get("targetType") : null;

			// Skip if already rendered as conditional
			if (edge.getSourceHandle() != null && !"source".equals(edge.getSourceHandle())) {
				continue;
			}

			String key = sourceId + "->" + targetId;
			if (renderedEdges.contains(key)) {
				continue;
			}
			renderedEdges.add(key);

			// START and END special handling
			if ("start".equals(sourceType)) {
				sb.append(String.format("        stateGraph.addEdge(START, \"%s\");%n", targetId));
			}
			else if ("end".equals(targetType)) {
				sb.append(String.format("        stateGraph.addEdge(\"%s\", END);%n", sourceId));
			}
			else {
				sb.append(String.format("        stateGraph.addEdge(\"%s\", \"%s\");%n", sourceId, targetId));
			}
		}

		// conditional edge（aggregate by sourceId）
		for (Map.Entry<String, List<Edge>> entry : conditionalEdgesMap.entrySet()) {
			String sourceId = entry.getKey();
			List<Edge> condEdges = entry.getValue();
			Node sourceNode = nodeMap.get(sourceId);
			NodeData sourceData = sourceNode.getData();

			List<String> conditions = new ArrayList<>();
			List<String> mappings = new ArrayList<>();

			for (Edge e : condEdges) {
				String conditionKey = resolveConditionKey(sourceData, e.getSourceHandle());
				String targetId = e.getTarget();
				conditions.add(String.format("            	if (value.contains(\"%s\")) return \"%s\";", conditionKey,
						conditionKey));
				mappings.add(String.format("\"%s\", \"%s\"", conditionKey, targetId));
			}

			String lambdaContent = String.join("\n", conditions);
			String mapContent = String.join(", ", mappings);

			sb.append(String.format(
					"        stateGraph.addConditionalEdges(\"%s\",%n" + "            edge_async(state -> {%n"
							+ "                String value = state.value(\"%s_output\", String.class).orElse(\"\");%n"
							+ "%s%n" + "                return null;%n" + "            }),%n"
							+ "            Map.of(%s)%n" + "        );%n",
					sourceId, sourceId, lambdaContent, mapContent));
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

	private void renderAndWriteTemplates(List<String> templateNames, List<Map<String, String>> models, Path projectRoot,
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

}
