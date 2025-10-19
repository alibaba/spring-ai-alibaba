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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		Map<String, Object> stateData = new HashMap<>();
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

	@Test
	void testNestedObjectAccess() {
		Map<String, Object> httpResponse = new HashMap<>();
		httpResponse.put("status", 200);
		httpResponse.put("body", "HTTP response content");
		
		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		httpResponse.put("headers", headers);
		
		OverAllState state = new OverAllState(Map.of("http_response", httpResponse));
		
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("{{http_response.body}}")
			.outputKey("extracted_body")
			.build();
		
		Map<String, Object> result = node.apply(state);
		
		assertEquals("HTTP response content", result.get("extracted_body"));
	}

	@Test
	void testMultipleNestedAccess() {
		Map<String, Object> httpResponse = new HashMap<>();
		httpResponse.put("status", 200);
		httpResponse.put("body", "Success");
		
		Map<String, Object> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		httpResponse.put("headers", headers);
		
		OverAllState state = new OverAllState(Map.of("http_response", httpResponse));
		
		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Status: {{http_response.status}}, Body: {{http_response.body}}, Type: {{http_response.headers.Content-Type}}")
			.build();
		
		Map<String, Object> result = node.apply(state);
		assertEquals("Status: 200, Body: Success, Type: application/json", result.get("result"));
	}

	@Test
	void testHttpNodeToLlmNodeDataTypeConversion() {
		Map<String, Object> httpResponse = new HashMap<>();
		httpResponse.put("status", 200);
		httpResponse.put("headers", Map.of("Content-Type", "application/json"));
		httpResponse.put("body", "This is the actual HTTP response body content that LlmNode needs as String");
		
		Map<String, Object> httpNodeOutput = new HashMap<>();
		httpNodeOutput.put("messages", httpResponse);
		httpNodeOutput.put("http_response", httpResponse);
		
		OverAllState state = new OverAllState(httpNodeOutput);
		
		TemplateTransformNode transformer = TemplateTransformNode.builder()
			.template("{{http_response.body}}")
			.outputKey("llm_input")
			.build();
		
		Map<String, Object> result = transformer.apply(state);
		
		String llmInput = (String) result.get("llm_input");
		assertEquals("This is the actual HTTP response body content that LlmNode needs as String", llmInput);
		
		assertTrue(state.data().get("http_response") instanceof Map);
	}


	@Test
	void testArrayIndexAccess() {
		List<Map<String, Object>> users = new ArrayList<>();
		Map<String, Object> user1 = new HashMap<>();
		user1.put("name", "Alice");
		user1.put("age", 25);
		users.add(user1);

		Map<String, Object> user2 = new HashMap<>();
		user2.put("name", "Bob");
		user2.put("age", 30);
		users.add(user2);

		OverAllState state = new OverAllState(Map.of("users", users));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("First user: {{users[0].name}}, Second user: {{users[1].name}}")
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals("First user: Alice, Second user: Bob", result.get("result"));
	}

	@Test
	void testArrayIndexAccessWithNestedObjects() {
		List<Map<String, Object>> products = new ArrayList<>();

		Map<String, Object> product1 = new HashMap<>();
		product1.put("name", "Laptop");
		Map<String, Object> price1 = new HashMap<>();
		price1.put("amount", 1200);
		price1.put("currency", "USD");
		product1.put("price", price1);
		products.add(product1);

		Map<String, Object> product2 = new HashMap<>();
		product2.put("name", "Mouse");
		Map<String, Object> price2 = new HashMap<>();
		price2.put("amount", 25);
		price2.put("currency", "USD");
		product2.put("price", price2);
		products.add(product2);

		OverAllState state = new OverAllState(Map.of("products", products));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Product: {{products[0].name}} costs {{products[0].price.amount}} {{products[0].price.currency}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Product: Laptop costs 1200 USD", result.get("result"));
	}

	@Test
	void testArrayIndexOutOfBounds() {
		List<String> items = List.of("item1", "item2");
		OverAllState state = new OverAllState(Map.of("items", items));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Item: {{items[5].name}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Item: {{items[5].name}}", result.get("result"));
	}

	@Test
	void testRootArrayAccess() {
		List<String> colors = List.of("red", "green", "blue");
		OverAllState state = new OverAllState(Map.of("colors", colors));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Color: {{colors[1]}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Color: green", result.get("result"));
	}

	@Test
	void testPojoReflectionAccess() {
		TestUser user = new TestUser("Charlie", "charlie@example.com", 28);
		OverAllState state = new OverAllState(Map.of("user", user));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("User: {{user.name}} ({{user.email}}), Age: {{user.age}}")
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals("User: Charlie (charlie@example.com), Age: 28", result.get("result"));
	}

	@Test
	void testPojoWithNestedPojo() {
		TestAddress address = new TestAddress("123 Main St", "New York", "10001");
		TestUser user = new TestUser("David", "david@example.com", 35);
		user.setAddress(address);

		OverAllState state = new OverAllState(Map.of("user", user));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("{{user.name}} lives in {{user.address.city}}, {{user.address.zipCode}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("David lives in New York, 10001", result.get("result"));
	}

	@Test
	void testPojoListAccess() {
		List<TestUser> users = new ArrayList<>();
		users.add(new TestUser("Eve", "eve@example.com", 22));
		users.add(new TestUser("Frank", "frank@example.com", 45));

		OverAllState state = new OverAllState(Map.of("users", users));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Users: {{users[0].name}} and {{users[1].name}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Users: Eve and Frank", result.get("result"));
	}

	@Test
	void testJsonStringParsing() {
		String jsonString = "{\"status\": 200, \"message\": \"Success\", \"data\": {\"id\": 123, \"name\": \"Product\"}}";
		OverAllState state = new OverAllState(Map.of("response", jsonString));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Status: {{response.status}}, Message: {{response.message}}, Product: {{response.data.name}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Status: 200, Message: Success, Product: Product", result.get("result"));
	}

	@Test
	void testJsonArrayStringParsing() {
		String jsonArrayString = "[{\"name\": \"Apple\", \"price\": 1.2}, {\"name\": \"Banana\", \"price\": 0.8}]";
		OverAllState state = new OverAllState(Map.of("fruits", jsonArrayString));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("First: {{fruits[0].name}} (${{fruits[0].price}}), Second: {{fruits[1].name}} (${{fruits[1].price}})")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("First: Apple ($1.2), Second: Banana ($0.8)", result.get("result"));
	}

	@Test
	void testMixedNestedAccessWithJsonAndPojo() {
		String jsonData = "{\"product\": \"Keyboard\", \"quantity\": 5}";
		TestUser user = new TestUser("Grace", "grace@example.com", 29);

		Map<String, Object> order = new HashMap<>();
		order.put("user", user);
		order.put("data", jsonData);

		OverAllState state = new OverAllState(Map.of("order", order));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Order: {{order.data.product}} x {{order.data.quantity}} for {{order.user.name}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Order: Keyboard x 5 for Grace", result.get("result"));
	}

	@Test
	void testInvalidJsonStringKeepsPlaceholder() {
		String invalidJson = "{invalid json}";
		OverAllState state = new OverAllState(Map.of("data", invalidJson));

		TemplateTransformNode node = TemplateTransformNode.builder().template("Value: {{data.field}}").build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Value: {{data.field}}", result.get("result"));
	}


	@Test
	void testElvisOperatorWithNullValue() {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("name", null);
		OverAllState state = new OverAllState(dataMap);

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Username: {{name ?: 'Anonymous'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Username: Anonymous", result.get("result"));
	}

	@Test
	void testElvisOperatorWithMissingKey() {
		OverAllState state = new OverAllState(Map.of("other", "value"));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Status: {{status ?: 'unknown'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Status: unknown", result.get("result"));
	}

	@Test
	void testElvisOperatorWithExistingValue() {
		OverAllState state = new OverAllState(Map.of("name", "Alice"));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Hello {{name ?: 'Guest'}}!")
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Hello Alice!", result.get("result"));
	}

	@Test
	void testElvisOperatorWithDoubleQuotes() {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("email", null);
		OverAllState state = new OverAllState(dataMap);

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Email: {{email ?: \"not provided\"}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Email: not provided", result.get("result"));
	}

	@Test
	void testElvisOperatorWithNestedAccess() {
		Map<String, Object> user = new HashMap<>();
		user.put("profile", null);
		OverAllState state = new OverAllState(Map.of("user", user));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("City: {{user.profile.city ?: 'Not specified'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("City: Not specified", result.get("result"));
	}

	@Test
	void testElvisOperatorWithArrayAccess() {
		List<String> items = List.of("item1");
		OverAllState state = new OverAllState(Map.of("items", items));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Second item: {{items[1] ?: 'N/A'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Second item: N/A", result.get("result"));
	}

	@Test
	void testMultipleElvisOperators() {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("name", "Bob");
		dataMap.put("age", null);
		OverAllState state = new OverAllState(dataMap);

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("User: {{name ?: 'Unknown'}}, Age: {{age ?: '18'}}, City: {{city ?: 'N/A'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("User: Bob, Age: 18, City: N/A", result.get("result"));
	}

	@Test
	void testElvisOperatorWithEmptyStringDefault() {
		OverAllState state = new OverAllState(Map.of("other", "value"));

		TemplateTransformNode node = TemplateTransformNode.builder().template("Value: {{missing ?: ''}}").build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Value: ", result.get("result"));
	}

	@Test
	void testElvisOperatorWithSpaces() {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("value", null);
		OverAllState state = new OverAllState(dataMap);

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Test: {{value?:'default'}} and {{value  ?:  'spaced'}}")
			.build();
		Map<String, Object> result = node.apply(state);

		assertEquals("Test: default and spaced", result.get("result"));
	}

	@Test
	void testElvisOperatorWithNumericDefault() {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("count", null);
		OverAllState state = new OverAllState(dataMap);

		TemplateTransformNode node = TemplateTransformNode.builder().template("Count: {{count ?: 0}}").build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Count: 0", result.get("result"));
	}

	@Test
	void testElvisOperatorWithPojoNullField() {
		TestUser user = new TestUser("Charlie", null, 28);
		OverAllState state = new OverAllState(Map.of("user", user));

		TemplateTransformNode node = TemplateTransformNode.builder()
			.template("Email: {{user.email ?: 'no-email@example.com'}}")
			.build();

		Map<String, Object> result = node.apply(state);

		assertEquals("Email: no-email@example.com", result.get("result"));
	}


	private static class TestUser {

		private String name;

		private String email;

		private int age;

		private TestAddress address;

		public TestUser(String name, String email, int age) {
			this.name = name;
			this.email = email;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public String getEmail() {
			return email;
		}

		public int getAge() {
			return age;
		}

		public TestAddress getAddress() {
			return address;
		}

		public void setAddress(TestAddress address) {
			this.address = address;
		}

	}

	private static class TestAddress {

		private String street;

		private String city;

		private String zipCode;

		public TestAddress(String street, String city, String zipCode) {
			this.street = street;
			this.city = city;
			this.zipCode = zipCode;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}

		public String getZipCode() {
			return zipCode;
		}

	}

}
