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

package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.ComparisonOperatorType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.LogicalOperatorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SaaBranchNodeDecisionTest {

	private Map<String, Node> nodeMap;

	@BeforeEach
	public void setUp() {
		nodeMap = new HashMap<>();

		Node fileReaderNode = new Node();
		fileReaderNode.setId("node-1725861677895");
		fileReaderNode.setTitle("File Reader");
		nodeMap.put("node-1725861677895", fileReaderNode);

		Node numberNode = new Node();
		numberNode.setId("node-1725861677896");
		numberNode.setTitle("Number Processor");
		nodeMap.put("node-1725861677896", numberNode);
	}

	@Nested
	@DisplayName("ComparisonOperatorType Tests")
	class ComparisonOperatorTypeTests {

		@Test
		@DisplayName("Test numeric comparison operators")
		public void testAllNumericComparisonOperators() {
			ComparisonOperatorType[] operators = { ComparisonOperatorType.EQUAL, ComparisonOperatorType.NOT_EQUAL,
					ComparisonOperatorType.GREATER_THAN, ComparisonOperatorType.LESS_THAN,
					ComparisonOperatorType.NOT_LESS_THAN, ComparisonOperatorType.NOT_GREATER_THAN };

			String[] expectedPatterns = { "(%s.doubleValue() == %s)", "(%s.doubleValue() != %s)",
					"(%s.doubleValue() > %s)", "(%s.doubleValue() < %s)", "(%s.doubleValue() >= %s)",
					"(%s.doubleValue() <= %s)" };

			for (int i = 0; i < operators.length; i++) {
				String result = operators[i].convert("numberVar", "42");
				String expected = String.format(expectedPatterns[i], "numberVar", "42");

				assertEquals(expected, result);
				assertTrue(result.contains(".doubleValue()"));
				assertFalse(result.matches(".*numberVar\\s*[><=!]+\\s*42.*"));
			}
		}

		@Test
		@DisplayName("Test string operators")
		public void testStringOperatorsUnaffected() {
			String containsResult = ComparisonOperatorType.CONTAINS.convert("stringVar", "\"test\"");
			assertEquals("(stringVar.contains(\"test\"))", containsResult);

			String equalsResult = ComparisonOperatorType.IS.convert("stringVar", "\"value\"");
			assertEquals("(stringVar.equals(\"value\"))", equalsResult);

			String startsWithResult = ComparisonOperatorType.START_WITH.convert("stringVar", "\"prefix\"");
			assertEquals("(stringVar.startsWith(\"prefix\"))", startsWithResult);
		}

		@Test
		@DisplayName("Test null check operators")
		public void testNullCheckOperators() {
			String nullResult = ComparisonOperatorType.NULL.convert("var", "null");
			assertEquals("(var == null)", nullResult);

			String notNullResult = ComparisonOperatorType.NOT_NULL.convert("var", "null");
			assertEquals("(var != null)", notNullResult);
		}

		@Test
		@DisplayName("Test edge cases")
		public void testEdgeCasesAndSpecialValues() {
			String negativeResult = ComparisonOperatorType.LESS_THAN.convert("num", "-5.5");
			assertEquals("(num.doubleValue() < -5.5)", negativeResult);

			String zeroResult = ComparisonOperatorType.EQUAL.convert("num", "0");
			assertEquals("(num.doubleValue() == 0)", zeroResult);
		}

	}

	@Nested
	@DisplayName("BranchNodeSection Tests")
	class BranchNodeSectionTests {

		@Test
		@DisplayName("Test file type variable access")
		public void testFileTypeVariableAccess() {
			Case.Condition condition = new Case.Condition();
			condition.setVarType("file");

			VariableSelector selector = new VariableSelector();
			selector.setNamespace("node-1725861677895");
			selector.setName("uploaded_file");
			condition.setVariableSelector(selector);

			assertEquals("file", condition.getVarType());
			assertEquals("node-1725861677895", condition.getVariableSelector().getNamespace());
			assertEquals("uploaded_file", condition.getVariableSelector().getName());
		}

		@Test
		@DisplayName("Test variable type identification")
		public void testVariableTypeIdentification() {
			String[] varTypes = { "string", "number", "boolean", "list", "object", "file" };

			for (String varType : varTypes) {
				Case.Condition condition = new Case.Condition();
				condition.setVarType(varType);
				assertEquals(varType, condition.getVarType());
			}
		}

		@Test
		@DisplayName("Test multi-condition logic")
		public void testMultiConditionLogicalCombination() {
			Case testCase = new Case();
			testCase.setLogicalOperator(LogicalOperatorType.AND);

			Case.Condition condition1 = new Case.Condition();
			condition1.setVarType("file");
			condition1.setComparisonOperator(ComparisonOperatorType.IS);

			Case.Condition condition2 = new Case.Condition();
			condition2.setVarType("number");
			condition2.setComparisonOperator(ComparisonOperatorType.LESS_THAN);

			testCase.setConditions(Arrays.asList(condition1, condition2));

			assertEquals(LogicalOperatorType.AND, testCase.getLogicalOperator());
			assertEquals(2, testCase.getConditions().size());
		}

	}

	@Test
	@DisplayName("Test file extension comparison")
	public void testOriginalErrorCase_FileExtensionComparison() {
		Case.Condition condition = new Case.Condition();
		condition.setVarType("file");

		VariableSelector selector = new VariableSelector();
		selector.setNamespace("node-1725861677895");
		selector.setName("extension");
		selector.setLabel("extension");
		condition.setVariableSelector(selector);

		condition.setComparisonOperator(ComparisonOperatorType.IS);

		String generatedExpression = ComparisonOperatorType.IS.convert("fileVar", "\".pdf\"");
		assertEquals("(fileVar.equals(\".pdf\"))", generatedExpression);
		assertFalse(generatedExpression.contains("=="));
	}

	@Test
	@DisplayName("Test number comparison fix")
	public void testOriginalErrorCase_NumberComparison() {
		String[] comparisonTests = { ComparisonOperatorType.EQUAL.convert("numberValue", "42"),
				ComparisonOperatorType.NOT_EQUAL.convert("numberValue", "42"),
				ComparisonOperatorType.GREATER_THAN.convert("numberValue", "42"),
				ComparisonOperatorType.LESS_THAN.convert("numberValue", "42"),
				ComparisonOperatorType.NOT_LESS_THAN.convert("numberValue", "42"),
				ComparisonOperatorType.NOT_GREATER_THAN.convert("numberValue", "42") };

		String[] expectedResults = { "(numberValue.doubleValue() == 42)", "(numberValue.doubleValue() != 42)",
				"(numberValue.doubleValue() > 42)", "(numberValue.doubleValue() < 42)",
				"(numberValue.doubleValue() >= 42)", "(numberValue.doubleValue() <= 42)" };

		for (int i = 0; i < comparisonTests.length; i++) {
			assertEquals(expectedResults[i], comparisonTests[i]);
			assertTrue(comparisonTests[i].contains(".doubleValue()"));
		}
	}

	@Test
	@DisplayName("Test fix comparison")
	public void testBeforeAfterFixComparison() {
		String numberComparison = ComparisonOperatorType.EQUAL.convert("numberValue", "42");
		assertFalse(numberComparison.equals("(numberValue == 42)"));
		assertTrue(numberComparison.contains(".doubleValue()"));

		String stringComparison = ComparisonOperatorType.IS.convert("stringVar", "\"test\"");
		assertEquals("(stringVar.equals(\"test\"))", stringComparison);
	}

	@Test
	@DisplayName("Test DSL variable selector parsing")
	public void testDSLVariableSelectorParsing() {
		VariableSelector selector = new VariableSelector();
		selector.setNamespace("node-1725861677895");
		selector.setName("extension");

		assertEquals("node-1725861677895", selector.getNamespace());
		assertEquals("extension", selector.getName());

		assertTrue(nodeMap.containsKey("node-1725861677895"));
		assertEquals("File Reader", nodeMap.get("node-1725861677895").getTitle());
	}

	@Test
	@DisplayName("Test multi-condition logical expression")
	public void testMultiConditionLogicalExpression() {
		Case testCase = new Case();
		testCase.setLogicalOperator(LogicalOperatorType.AND);

		Case.Condition fileCondition = new Case.Condition();
		fileCondition.setVarType("file");
		VariableSelector fileSelector = new VariableSelector();
		fileSelector.setNamespace("node-1725861677895");
		fileSelector.setName("uploaded_file");
		fileSelector.setLabel("extension");
		fileCondition.setVariableSelector(fileSelector);
		fileCondition.setComparisonOperator(ComparisonOperatorType.IS);

		Case.Condition numberCondition = new Case.Condition();
		numberCondition.setVarType("number");
		VariableSelector numberSelector = new VariableSelector();
		numberSelector.setNamespace("node-1725861677896");
		numberSelector.setName("file_size");
		numberCondition.setVariableSelector(numberSelector);
		numberCondition.setComparisonOperator(ComparisonOperatorType.LESS_THAN);

		testCase.setConditions(List.of(fileCondition, numberCondition));

		assertEquals(2, testCase.getConditions().size());
		assertEquals(LogicalOperatorType.AND, testCase.getLogicalOperator());

		String fileExpression = fileCondition.getComparisonOperator().convert("fileExtension", "\".pdf\"");
		String numberExpression = numberCondition.getComparisonOperator().convert("fileSize", "1048576");

		assertEquals("(fileExtension.equals(\".pdf\"))", fileExpression);
		assertEquals("(fileSize.doubleValue() < 1048576)", numberExpression);
		assertTrue(numberExpression.contains(".doubleValue()"));
	}

	@Test
	@DisplayName("Test fix resolves compilation errors")
	public void testFixResolvesCompilationErrors() {
		ComparisonOperatorType[] numberOperators = { ComparisonOperatorType.EQUAL, ComparisonOperatorType.NOT_EQUAL,
				ComparisonOperatorType.GREATER_THAN, ComparisonOperatorType.LESS_THAN,
				ComparisonOperatorType.NOT_LESS_THAN, ComparisonOperatorType.NOT_GREATER_THAN };

		for (ComparisonOperatorType operator : numberOperators) {
			String expression = operator.convert("numVar", "123");

			assertTrue(expression.startsWith("(") && expression.endsWith(")"));
			assertTrue(expression.contains(".doubleValue()"));
			assertFalse(expression.matches(".*numVar\\s*[><=!]+\\s*123.*"));
		}

		String stringExpression = ComparisonOperatorType.CONTAINS.convert("strVar", "\"test\"");
		assertEquals("(strVar.contains(\"test\"))", stringExpression);

		String nullExpression = ComparisonOperatorType.NULL.convert("var", "null");
		assertEquals("(var == null)", nullExpression);

		String notNullExpression = ComparisonOperatorType.NOT_NULL.convert("var", "null");
		assertEquals("(var != null)", notNullExpression);
	}

}
