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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTransformNodeTest {

	@Test
	void testBasicTemplateTransformation() {
		// Given: A template with a single variable
		TemplateTransformNode node = TemplateTransformNode.builder().template("Hello {{name}}!").build();

		OverAllState state = new OverAllState(Map.of("name", "World"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Template is processed correctly
		assertEquals("Hello World!", result.get("result"));
	}

	@Test
	void testMultipleVariablesInTemplate() {
		// Given: A template with multiple variables
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("{{greeting}} {{name}}, today is {{day}}")
			.build();

		OverAllState state = new OverAllState(Map.of("greeting", "Hello", "name", "Alice", "day", "Monday"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: All variables are replaced
		assertEquals("Hello Alice, today is Monday", result.get("result"));
	}

	@Test
	void testMissingVariablesKeptAsPlaceholders() {
		// Given: A template with missing variables
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Hello {{name}}, your score is {{score}}")
			.build();

		OverAllState state = new OverAllState(Map.of("name", "Bob"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Missing variables are kept as placeholders
		assertEquals("Hello Bob, your score is {{score}}", result.get("result"));
	}

	@Test
	void testVariableWithNullValue() {
		// Given: A template with a null-valued variable
		TemplateTransformNode node = TemplateTransformNode.builder().template("Status: {{status}}").build();

		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("status", null);
		OverAllState state = new OverAllState(dataMap);

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Null values are replaced with "null"
		assertEquals("Status: null", result.get("result"));
	}

	@Test
	void testDefaultOutputKey() {
		// Given: A node without custom output key
		TemplateTransformNode node = TemplateTransformNode.builder().template("Test").build();

		OverAllState state = new OverAllState(Map.of());

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Uses default "result" key
		assertTrue(result.containsKey("result"));
		assertEquals("Test", result.get("result"));
	}

	@Test
	void testTemplateWithoutVariables() {
		// Given: A template without any variables
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("This is a static text")
			.outputKey("output")
			.build();

		OverAllState state = new OverAllState(Map.of("irrelevant", "data"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Template is returned as-is
		assertEquals("This is a static text", result.get("output"));
	}

	@Test
	void testEmptyTemplate() {
		// Given: Empty template
		TemplateTransformNode node = TemplateTransformNode.builder().template("").build();

		OverAllState state = new OverAllState(Map.of("var", "value"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Returns empty string
		assertEquals("", result.get("result"));
	}

	@Test
	void testLargeTemplate() {
		// Given: A large template with many variables
		StringBuilder templateBuilder = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			templateBuilder.append("{{var").append(i).append("}} ");
		}

		TemplateTransformNode node = TemplateTransformNode.builder().template(templateBuilder.toString()).build();

		// Create state with values for first 50 variables
		Map<String, Object> stateData = new java.util.HashMap<>();
		for (int i = 0; i < 50; i++) {
			stateData.put("var" + i, "value" + i);
		}
		OverAllState state = new OverAllState(stateData);

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: First 50 variables are replaced, others kept as placeholders
		String output = (String) result.get("result");
		assertTrue(output.contains("value0"));
		assertTrue(output.contains("value49"));
		assertTrue(output.contains("{{var50}}"));
		assertTrue(output.contains("{{var99}}"));
	}

	@Test
	void testSpecialCharactersInVariables() {
		// Given: Template with special characters in variable values
		TemplateTransformNode node = TemplateTransformNode.builder().template("Message: {{content}}").build();

		OverAllState state = new OverAllState(Map.of("content", "Special chars: $100 \\backslash {braces}"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Special characters are properly escaped and handled
		assertEquals("Message: Special chars: $100 \\backslash {braces}", result.get("result"));
	}

	@Test
	void testComplexNestedVariables() {
		// Given: Template with nested-like patterns
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("{{outer}} contains {{inner}} and {{nested}}")
			.build();

		OverAllState state = new OverAllState(Map.of("outer", "Container", "inner", "item1", "nested", "item2"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: All variables are correctly replaced
		assertEquals("Container contains item1 and item2", result.get("result"));
	}

	@Test
	void testBuilderValidation() {
		// Given/When/Then: Builder validates required fields
		assertThrows(IllegalArgumentException.class, () -> TemplateTransformNode.builder().build());

		assertThrows(IllegalArgumentException.class, () -> TemplateTransformNode.builder().template(null).build());
	}

	@Test
	void testBuilderChaining() {
		// Given/When: Builder with chained calls
		TemplateTransformNode node = TemplateTransformNode.builder().template("{{test}}").outputKey("custom").build();

		OverAllState state = new OverAllState(Map.of("test", "success"));
		Map<String, Object> result = node.apply(state);

		// Then: Custom output key is used
		assertEquals("success", result.get("custom"));
	}

}
