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

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.Case;
import com.alibaba.cloud.ai.model.workflow.ComparisonOperatorType;
import com.alibaba.cloud.ai.model.workflow.Edge;
import com.alibaba.cloud.ai.model.workflow.LogicalOperatorType;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import io.spring.initializr.generator.io.template.MustacheTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowProjectGeneratorTest {

	@Mock
	private DSLAdapter dslAdapter;

	@Mock
	private ObjectProvider<MustacheTemplateRenderer> templateRendererProvider;

	private WorkflowProjectGenerator generator;

	@BeforeEach
	void setUp() {
		when(templateRendererProvider.getIfAvailable(org.mockito.ArgumentMatchers.any()))
			.thenReturn(new MustacheTemplateRenderer("classpath:/templates"));
		generator = new WorkflowProjectGenerator(dslAdapter, templateRendererProvider, List.of());
	}

	@Test
	void testGenerateBranchConditionLogic_StringType() {
		Case caseData = createTestCase("string", "equals", ".docx");
		String result = generator.generateBranchConditionLogic(caseData);
		assertThat(result).contains("Objects.equals");
		assertThat(result).contains("String.class");
		assertThat(result).contains(".docx");
	}

	@Test
	void testGenerateBranchConditionLogic_ObjectType() {
		Case caseData = createTestCase("object", "equals", ".docx");
		String result = generator.generateBranchConditionLogic(caseData);
		assertThat(result).contains("Object.class");
		assertThat(result).contains("String.valueOf");
		assertThat(result).contains(".docx");
		assertThat(result).doesNotContain("String.class");
	}

	@Test
	void testGenerateBranchConditionLogic_ListType() {
		Case caseData = createTestCase("list", "contains", "document");
		String result = generator.generateBranchConditionLogic(caseData);
		assertThat(result).contains("Object.class");
		assertThat(result).contains("String.valueOf");
		assertThat(result).contains("contains");
		assertThat(result).contains("document");
		assertThat(result).doesNotContain("String.class");
	}

	@Test
	void testGenerateBranchConditionLogic_MultipleConditionsWithOR() {
		Case caseData = new Case().setId("true")
			.setLogicalOperator(LogicalOperatorType.OR)
			.setConditions(Arrays.asList(createCondition("file", "equals", ".docx"),
					createCondition("file", "equals", ".md"), createCondition("file", "equals", ".txt")));
		String result = generator.generateBranchConditionLogic(caseData);
		assertThat(result).contains("||");
		assertThat(result).contains(".docx");
		assertThat(result).contains(".md");
		assertThat(result).contains(".txt");
		assertThat(result).contains("Object.class");
		assertThat(result).contains("String.valueOf");
	}

	@Test
	void testGenerateBranchConditionLogic_MultipleConditionsWithAND() {
		Case caseData = new Case().setId("complex")
			.setLogicalOperator(LogicalOperatorType.AND)
			.setConditions(Arrays.asList(createCondition("string", "equals", "value1"),
					createCondition("number", "greater_than", "100")));
		String result = generator.generateBranchConditionLogic(caseData);
		assertThat(result).contains("&&");
		assertThat(result).contains("value1");
		assertThat(result).contains("100");
	}

	@Test
	void testGenerateComparison_ObjectTypeEquality() {
		String leftSide = "String.valueOf(state.value(\"upload_file\", Object.class).orElse(null))";
		String operator = "equals";
		String value = ".xlsx";
		String varType = "object";
		String result = generator.generateComparison(leftSide, operator, value, varType);
		assertThat(result).contains(".equals(\".xlsx\")");
		assertThat(result).doesNotContain("Objects.equals");
	}

	@Test
	void testGenerateComparison_ListTypeContains() {
		String leftSide = "String.valueOf(state.value(\"items\", Object.class).orElse(null))";
		String operator = "contains";
		String value = "item1";
		String varType = "list";
		String result = generator.generateComparison(leftSide, operator, value, varType);
		assertThat(result).contains(".toString().contains(\"item1\")");
	}

	@Test
	void testGenerateComparison_NumericComparison() {
		String leftSide = "state.value(\"count\", Integer.class).orElse(0)";
		String operator = "greater_than";
		String value = "5";
		String varType = "number";
		String result = generator.generateComparison(leftSide, operator, value, varType);
		assertThat(result).contains("Double.parseDouble");
		assertThat(result).contains(">");
		assertThat(result).contains("5");
	}

	@Test
	void testFormatValueForType_ObjectType() {
		String result = generator.formatValueForType(".docx", "object");
		assertThat(result).isEqualTo("\".docx\"");
	}

	@Test
	void testFormatValueForType_NumberType() {
		String result = generator.formatValueForType("123", "number");
		assertThat(result).isEqualTo("123");
	}

	@Test
	void testRenderEdgeSections_BranchNodeWithObjectParameter() {
		List<Edge> edges = createTestEdgesForBranchNode();
		List<Node> nodes = createTestNodesWithBranchNode();
		Map<String, String> varNames = Map.of("startNode", "startNode1", "branchNode", "branchNode1", "endNode",
				"endNode1");
		String result = generator.renderEdgeSections(edges, nodes, varNames);
		assertThat(result).contains("addConditionalEdges");
		assertThat(result).contains("edge_async");
		assertThat(result).contains("Object.class");
		assertThat(result).doesNotContain("String.class");
		assertThat(result).contains("String.valueOf");
		assertThat(result).contains(".equals");
		assertThat(result).contains(".docx");
		assertThat(result).contains(".xlsx");
	}

	@Test
	void testBugFix2017_ObjectParameterHandling() {
		Case fileTypeCase = new Case().setId("true")
			.setLogicalOperator(LogicalOperatorType.OR)
			.setConditions(Arrays.asList(createFileCondition(".docx"), createFileCondition(".md"),
					createFileCondition(".txt")));
		String conditionLogic = generator.generateBranchConditionLogic(fileTypeCase);
		assertThat(conditionLogic).contains("Object.class");
		assertThat(conditionLogic).contains("String.valueOf");
		assertThat(conditionLogic).doesNotContain("String.class");
		assertThat(conditionLogic).contains("||");
		assertThat(conditionLogic).contains(".docx");
		assertThat(conditionLogic).contains(".md");
		assertThat(conditionLogic).contains(".txt");
		assertTrue(conditionLogic.startsWith("("));
		assertTrue(conditionLogic.endsWith(")"));
	}

	@Test
	void testUserScenario_FileUploadWorkflow() {
		List<Edge> edges = createUserScenarioEdges();
		List<Node> nodes = createUserScenarioNodes();
		Map<String, String> varNames = Map.of("1753254648676", "startNode1", "1753254655201", "branchNode1",
				"1753254661836", "documentExtractorNode1", "1753254676813", "httpNode1", "1753326764865",
				"documentExtractorNode2");
		String result = generator.renderEdgeSections(edges, nodes, varNames);
		assertThat(result).contains("addConditionalEdges");
		assertThat(result).contains("branchNode1");
		assertThat(result).contains("Object.class");
		assertThat(result).doesNotContain("String.class");
		assertThat(result).contains(".docx");
		assertThat(result).contains(".md");
		assertThat(result).contains(".txt");
		assertThat(result).contains(".xlsx");
		assertThat(result).contains(".xls");
		assertThat(result).contains("String.valueOf");
		assertThat(result).contains("if (");
		assertThat(result).contains("return \"true\"");
		assertThat(result).contains("return \"01f4b6bc-e91a-4f90-942f-45da844f009b\"");
		assertThat(result).contains("return null;");
		assertThat(result).contains("||");

	}

	private Case createTestCase(String varType, String operator, String value) {
		return new Case().setId("test")
			.setLogicalOperator(LogicalOperatorType.AND)
			.setConditions(List.of(createCondition(varType, operator, value)));
	}

	private Case.Condition createCondition(String varType, String operator, String value) {
		ComparisonOperatorType operatorType;
		try {
			operatorType = ComparisonOperatorType.valueOf(operator.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			// Handle special cases for operator mapping
			switch (operator.toLowerCase()) {
				case "equals":
					operatorType = ComparisonOperatorType.IS;
					break;
				case "greater_than":
					operatorType = ComparisonOperatorType.GREATER_THAN;
					break;
				case "contains":
					operatorType = ComparisonOperatorType.CONTAINS;
					break;
				default:
					operatorType = ComparisonOperatorType.IS;
					break;
			}
		}

		return new Case.Condition().setVarType(varType)
			.setComparisonOperator(operatorType)
			.setValue(value)
			.setVariableSelector(new VariableSelector("testNode", "testVar"));
	}

	private Case.Condition createFileCondition(String extension) {
		return new Case.Condition().setVarType("file")
			.setComparisonOperator(ComparisonOperatorType.IS)
			.setValue(extension)
			.setVariableSelector(new VariableSelector("startNode", "upload_file"));
	}

	private List<Edge> createUserScenarioEdges() {
		Edge startToBranch = new Edge();
		startToBranch.setSource("1753254648676");
		startToBranch.setTarget("1753254655201");
		startToBranch.setSourceHandle("source");
		Map<String, Object> startData = new HashMap<>();
		startData.put("sourceType", "start");
		startData.put("targetType", "if-else");
		startToBranch.setData(startData);

		Edge branchToDoc1 = new Edge();
		branchToDoc1.setSource("1753254655201");
		branchToDoc1.setTarget("1753254661836");
		branchToDoc1.setSourceHandle("true");
		Map<String, Object> trueData = new HashMap<>();
		trueData.put("sourceType", "if-else");
		trueData.put("targetType", "document-extractor");
		branchToDoc1.setData(trueData);

		Edge branchToHttp = new Edge();
		branchToHttp.setSource("1753254655201");
		branchToHttp.setTarget("1753254676813");
		branchToHttp.setSourceHandle("false");
		Map<String, Object> falseData = new HashMap<>();
		falseData.put("sourceType", "if-else");
		falseData.put("targetType", "http-request");
		branchToHttp.setData(falseData);

		Edge branchToDoc2 = new Edge();
		branchToDoc2.setSource("1753254655201");
		branchToDoc2.setTarget("1753326764865");
		branchToDoc2.setSourceHandle("01f4b6bc-e91a-4f90-942f-45da844f009b");
		Map<String, Object> excelData = new HashMap<>();
		excelData.put("sourceType", "if-else");
		excelData.put("targetType", "document-extractor");
		branchToDoc2.setData(excelData);

		return List.of(startToBranch, branchToDoc1, branchToHttp, branchToDoc2);
	}

	private List<Node> createUserScenarioNodes() {
		Node startNode = new Node();
		startNode.setId("1753254648676");
		startNode.setType("start");
		StartNodeData startData = new StartNodeData();
		startNode.setData(startData);
		Node branchNode = new Node();
		branchNode.setId("1753254655201");
		branchNode.setType("if-else");

		BranchNodeData branchData = new BranchNodeData();

		Case trueCase = new Case().setId("true")
			.setLogicalOperator(LogicalOperatorType.OR)
			.setConditions(Arrays.asList(createFileExtensionCondition(".docx"), createFileExtensionCondition(".md"),
					createFileExtensionCondition(".txt")));

		Case excelCase = new Case().setId("01f4b6bc-e91a-4f90-942f-45da844f009b")
			.setLogicalOperator(LogicalOperatorType.OR)
			.setConditions(Arrays.asList(createFileExtensionCondition(".xlsx"), createFileExtensionCondition(".xls")));

		branchData.setCases(List.of(trueCase, excelCase));
		branchNode.setData(branchData);

		Node docNode1 = new Node();
		docNode1.setId("1753254661836");
		docNode1.setType("document-extractor");

		Node docNode2 = new Node();
		docNode2.setId("1753326764865");
		docNode2.setType("document-extractor");

		Node httpNode = new Node();
		httpNode.setId("1753254676813");
		httpNode.setType("http-request");

		return List.of(startNode, branchNode, docNode1, docNode2, httpNode);
	}

	private Case.Condition createFileExtensionCondition(String extension) {
		return new Case.Condition().setVarType("file")
			.setComparisonOperator(ComparisonOperatorType.IS)
			.setValue(extension)
			.setVariableSelector(new VariableSelector("1753254648676", "upload_file", "extension"));
	}

	private List<Edge> createTestEdgesForBranchNode() {
		Edge edge1 = new Edge();
		edge1.setSource("branchNode");
		edge1.setTarget("endNode");
		edge1.setSourceHandle("true");
		Map<String, Object> data1 = new HashMap<>();
		data1.put("sourceType", "if-else");
		data1.put("targetType", "end");
		edge1.setData(data1);

		return List.of(edge1);
	}

	private List<Node> createTestNodesWithBranchNode() {
		Node startNode = new Node();
		startNode.setId("startNode");
		startNode.setType("start");
		startNode.setData(new StartNodeData());

		Node branchNode = new Node();
		branchNode.setId("branchNode");
		branchNode.setType("if-else");

		BranchNodeData branchData = new BranchNodeData();
		branchData.setOutputKey("branch_output");

		Case fileCase = new Case().setId("true")
			.setLogicalOperator(LogicalOperatorType.OR)
			.setConditions(Arrays.asList(createFileCondition(".docx"), createFileCondition(".xlsx")));

		branchData.setCases(List.of(fileCase));
		branchNode.setData(branchData);

		Node endNode = new Node();
		endNode.setId("endNode");
		endNode.setType("end");

		return List.of(startNode, branchNode, endNode);
	}

}
