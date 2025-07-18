/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "Licens	@Test
	void testEmptyTemplate() {
		// Given: Empty template
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("   ")  // whitespace template should be allowed but trimmed
			.build();

		OverAllState state = new OverAllState(Map.of("var", "value"));

		// When: Apply transformation
		Map<String, Object> result = node.apply(state);

		// Then: Returns empty string
		assertEquals("   ", result.get("result"));
	}u may not use this file except in compliance with the License.
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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TemplateTransformNodeTest {

	@Test
	void testBasicTemplateTransformation() {
	
		String template = "Hello {{name}}, welcome to {{platform}}!";
		TemplateTransformNode node = TemplateTransformNode.builder().template(template).outputKey("greeting").build();

		OverAllState state = new OverAllState(Map.of("name", "Alice", "platform", "Spring AI Alibaba"));

		
		Map<String, Object> result = node.apply(state);

		assertNotNull(result);
		assertEquals("Hello Alice, welcome to Spring AI Alibaba!", result.get("greeting"));
	}

	@Test
	void testDefaultOutputKey() {

		TemplateTransformNode node = TemplateTransformNode.builder().template("Result: {{value}}").build();

		OverAllState state = new OverAllState(Map.of("value", "success"));

	
		Map<String, Object> result = node.apply(state);

		
		assertEquals("Result: success", result.get("result"));
	}

	@Test
	void testMultipleVariablesInTemplate() {
	
		String template = "{{user}} logged in at {{time}}. User {{user}} has {{count}} messages.";
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(template)
			.outputKey("log_message")
			.build();

		OverAllState state = new OverAllState(Map.of("user", "john_doe", "time", "2024-01-15 10:30:00", "count", 5));

	
		Map<String, Object> result = node.apply(state);

	
		String expected = "john_doe logged in at 2024-01-15 10:30:00. User john_doe has 5 messages.";
		assertEquals(expected, result.get("log_message"));
	}

	@Test
	void testComplexNestedVariables() {
	
		String template = "Order #{{order.id}} for {{customer.name}} ({{customer.email}}) - Total: ${{order.total}} - Items: {{order.itemCount}}";
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(template)
			.outputKey("order_summary")
			.build();

		OverAllState state = new OverAllState(Map.of("order.id", "ORD-12345", "customer.name", "Jane Smith",
				"customer.email", "jane@example.com", "order.total", 99.99, "order.itemCount", 3));

		Map<String, Object> result = node.apply(state);

		String expected = "Order #ORD-12345 for Jane Smith (jane@example.com) - Total: $99.99 - Items: 3";
		assertEquals(expected, result.get("order_summary"));
	}

	@Test
	void testMissingVariablesKeptAsPlaceholders() {
	
		String template = "Available: {{available_var}}, Missing: {{missing_var}}";
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(template)
			.outputKey("partial_result")
			.build();

		OverAllState state = new OverAllState(Map.of("available_var", "found"));

		Map<String, Object> result = node.apply(state);


		assertEquals("Available: found, Missing: {{missing_var}}", result.get("partial_result"));
	}

	@Test
	void testEmptyTemplate() {
	
		TemplateTransformNode node = TemplateTransformNode.builder().template("").build();

		OverAllState state = new OverAllState(Map.of("var", "value"));
		Map<String, Object> result = node.apply(state);

		assertEquals("", result.get("result"));
	}

	@Test
	void testTemplateWithoutVariables() {
		String staticText = "This is a static message without any variables.";
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(staticText)
			.outputKey("static_message")
			.build();

		OverAllState state = new OverAllState(Map.of("unused", "value"));

		
		Map<String, Object> result = node.apply(state);

		assertEquals(staticText, result.get("static_message"));
	}

	@Test
	void testSpecialCharactersInVariables() {
	
		String template = "Message: {{msg}} | Symbols: {{symbols}} | Numbers: {{numbers}}";
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(template)
			.outputKey("special_output")
			.build();

		OverAllState state = new OverAllState(
				Map.of("msg", "Hello & Goodbye!", "symbols", "@#$%^&*()", "numbers", "123-456-7890"));

		Map<String, Object> result = node.apply(state);

		String expected = "Message: Hello & Goodbye! | Symbols: @#$%^&*() | Numbers: 123-456-7890";
		assertEquals(expected, result.get("special_output"));
	}

	@Test
	void testBuilderValidation() {
	
		TemplateTransformNode.Builder builder = TemplateTransformNode.builder();

		
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testBuilderWithNullTemplate() {
		
		TemplateTransformNode.Builder builder = TemplateTransformNode.builder();

	
		assertThrows(IllegalArgumentException.class, () -> builder.template(null));
	}

	@Test
	void testBuilderChaining() {
	
		TemplateTransformNode.Builder builder = TemplateTransformNode.builder();

		TemplateTransformNode node = builder.template("Test {{var}}").outputKey("test_output").build();

		assertNotNull(node);

		OverAllState state = new OverAllState(Map.of("var", "value"));
		Map<String, Object> result = node.apply(state);
		assertEquals("Test value", result.get("test_output"));
	}

	@Test
	void testLargeTemplate() {
	
		StringBuilder templateBuilder = new StringBuilder();
		Map<String, Object> variables = Map.of("title", "Annual Report", "year", 2024, "company", "Alibaba Cloud",
				"revenue", "1.2B", "growth", "15%");

		templateBuilder.append("{{title}} {{year}}\n")
			.append("Company: {{company}}\n")
			.append("Revenue: ${{revenue}}\n")
			.append("Growth: {{growth}}\n")
			.append("Generated for {{company}} in {{year}} showing {{growth}} growth.");

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template(templateBuilder.toString())
			.outputKey("report")
			.build();

		OverAllState state = new OverAllState(variables);

		
		Map<String, Object> result = node.apply(state);

		
		String output = (String) result.get("report");
		assertNotNull(output);
		assertTrue(output.contains("Annual Report 2024"));
		assertTrue(output.contains("Company: Alibaba Cloud"));
		assertTrue(output.contains("Revenue: $1.2B"));
		assertTrue(output.contains("Growth: 15%"));
	}

	@Test
	void testVariableWithNullValue() {
	
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Value: {{nullVar}}, Other: {{validVar}}")
			.build();

		Map<String, Object> variables = new HashMap<>();
		variables.put("nullVar", null);
		variables.put("validVar", "valid");
		OverAllState state = new OverAllState(variables);

		
		Map<String, Object> result = node.apply(state);

		
		assertEquals("Value: null, Other: valid", result.get("result"));
	}

}
