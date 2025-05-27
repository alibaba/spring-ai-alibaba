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
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.generator.GraphProjectDescription;
import com.alibaba.cloud.ai.service.generator.ProjectGenerator;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	public WorkflowProjectGenerator(@Qualifier("customDSLAdapter") DSLAdapter dslAdapter,
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
		// import dsl
		App app = dslAdapter.importDSL(projectDescription.getDsl());
		Workflow workflow = (Workflow) app.getSpec();
		// render sections
		List<Variable> overallStateVars = workflow.getWorkflowVars();
		String stateSectionStr = renderStateSections(overallStateVars);
		String nodeSectionStr = renderNodeSections(workflow.getGraph().getNodes());
		String edSectionStr = renderEdgeSections(workflow.getGraph().getEdges());
		Map<String, String> graphBuilderModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName(),
				GRAPH_BUILDER_STATE_SECTION, stateSectionStr, GRAPH_BUILDER_NODE_SECTION, nodeSectionStr,
				GRAPH_BUILDER_EDGE_SECTION, edSectionStr);
		Map<String, String> graphRunControllerModel = Map.of(PACKAGE_NAME, projectDescription.getPackageName());
		// render and write templates
		renderAndWriteTemplates(List.of(GRAPH_BUILDER_TEMPLATE_NAME, GRAPH_RUN_TEMPLATE_NAME),
				List.of(graphBuilderModel, graphRunControllerModel), projectRoot, projectDescription);
	}

	private String renderStateSections(List<Variable> overallStateVars) {
		String registerKeyFormat = "overallState.registerKeyAndStrategy(\"%s\", (o1, o2) -> o2);\n";
		StringBuilder stringBuilder = new StringBuilder();
		List<String> vars = overallStateVars.stream().map(Variable::getName).toList();
		vars.forEach(var -> {
			stringBuilder.append(String.format(registerKeyFormat, var));
		});
		return stringBuilder.toString();
	}

	private String renderNodeSections(List<Node> nodes) {
		StringBuilder stringBuilder = new StringBuilder();
		nodes.forEach(node -> {
			// 1. temporary solution
			NodeType nodeType = NodeType.fromValue(node.getType()).orElse(null);
			log.warn("Unsupported node type for generation: {}", node.getType());
			if (Objects.isNull(nodeType)) {
				return;
			}
			// 2. The right way to handle the exception:
			// NodeType nodeType = NodeType.fromValue(node.getType())
			// .orElseThrow(()-> new NotImplementedException("Unsupported node type for
			// generation" + node.getType()));
			for (NodeSection nodeSection : nodeNodeSections) {
				if (nodeSection.support(nodeType)) {
					stringBuilder.append(nodeSection.render(node.getData()));
					break;
				}
			}
		});
		return stringBuilder.toString();
	}

	private String renderEdgeSections(List<Edge> edges) {
		// todo
		return "";
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
