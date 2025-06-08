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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppModeEnum;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.workflow.*;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
		String edgeSectionStr = renderEdgeSections(workflow.getGraph().getEdges());

		Map<String, String> graphBuilderModel = Map.of(
				PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr,
				GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edgeSectionStr
		);
		Map<String, String> graphRunControllerModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());
		renderAndWriteTemplates(
				List.of(GRAPH_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
				List.of(graphBuilderModel, graphRunControllerModel),
				projectRoot, projectDescription
		);
	}

	private Map<String, String> generateVarNames(List<Node> nodes) {
		Map<String, Integer> counters = new HashMap<>();
		Map<String, String> result = new LinkedHashMap<>();
		for (Node node : nodes) {
			NodeType type = NodeType.fromValue(node.getType()).orElseThrow();
			String base = toLowerCamel(type.name().replace('-', ' '));
			int idx = counters.merge(base, 1, Integer::sum);
			String varName = base + idx + "Node";
			result.put(node.getId(), varName);
		}
		return result;
	}

	private String toLowerCamel(String text) {
		String[] parts = text.split("\\s+");
		StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
		for (int i = 1; i < parts.length; i++) {
			sb.append(Character.toUpperCase(parts[i].charAt(0)))
					.append(parts[i].substring(1).toLowerCase());
		}
		return sb.toString();
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

	private String renderEdgeSections(List<Edge> edges) {
		StringBuilder sb = new StringBuilder();
		for (Edge e : edges) {
			String srcCode = StateGraph.START.equals(e.getSource())
					? "START" : "\"" + e.getSource() + "\"";
			String tgtCode = StateGraph.END.equals(e.getTarget())
					? "END"   : "\"" + e.getTarget() + "\"";
			sb.append("        stateGraph.addEdge(")
					.append(srcCode).append(", ").append(tgtCode).append(");\n");
		}
		sb.append("\n");
		return sb.toString();
	}

	private void renderAndWriteTemplates(List<String> templateNames, List<Map<String, String>> models, Path projectRoot,
			ProjectDescription projectDescription) {
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
