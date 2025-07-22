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
package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.nodedata.TemplateTransformNodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for TemplateTransformNodeSection code generation functionality.
 */
class TemplateTransformNodeSectionTest {

	private TemplateTransformNodeSection templateTransformNodeSection;

	@BeforeEach
	void setUp() {
		templateTransformNodeSection = new TemplateTransformNodeSection();
	}

	@Test
	void testSupportsTemplateTransformNodeType() {
		// Given

		// When & Then
		assertThat(templateTransformNodeSection.support(NodeType.TEMPLATE_TRANSFORM)).isTrue();
		assertThat(templateTransformNodeSection.support(NodeType.LLM)).isFalse();
	}

	@Test
	void testRenderTemplateTransformNode() {
		// Given
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		nodeData.setTemplate("Hello {{name}}, welcome to {{place}}!");
		nodeData.setOutputKey("greeting");
		nodeData.setVarName("templateTransform1");

		Node node = new Node();
		node.setData(nodeData);

		String varName = "templateTransform1";

		// When
		String result = templateTransformNodeSection.render(node, varName);

		// Then
		assertThat(result).contains("TemplateTransformNode.builder()");
		assertThat(result).contains(".template(\"Hello {{name}}, welcome to {{place}}!\")");
		assertThat(result).contains(".outputKey(\"greeting\")");
		assertThat(result).contains(".build()");
	}

	@Test
	void testRenderWithSpecialCharacters() {
		// Given
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		nodeData.setTemplate("Quote: \"Hello world\", newline: \n, tab: \t");
		nodeData.setOutputKey("special_output");
		nodeData.setVarName("templateTransform2");

		Node node = new Node();
		node.setData(nodeData);

		String varName = "templateTransform2";

		// When
		String result = templateTransformNodeSection.render(node, varName);

		// Then
		// Should properly escape special characters
		assertThat(result).contains("TemplateTransformNode.builder()");
		assertThat(result).contains("\\\""); // escaped quotes
		assertThat(result).contains("\\n"); // escaped newline
		assertThat(result).contains("\\t"); // escaped tab
		assertThat(result).contains(".outputKey(\"special_output\")");
		assertThat(result).contains(".build()");
	}

	@Test
	void testRenderWithEmptyTemplate() {
		// Given
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		nodeData.setTemplate("");
		nodeData.setOutputKey("empty_output");
		nodeData.setVarName("templateTransform3");

		Node node = new Node();
		node.setData(nodeData);

		String varName = "templateTransform3";

		// When
		String result = templateTransformNodeSection.render(node, varName);

		// Then
		assertThat(result).contains("TemplateTransformNode.builder()");
		assertThat(result).contains(".template(\"\")");
		assertThat(result).contains(".outputKey(\"empty_output\")");
		assertThat(result).contains(".build()");
	}

}
