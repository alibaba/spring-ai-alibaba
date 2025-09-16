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

package com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.regex.Pattern;

class ResponseTemplateParserTest {

	@Test
	void shouldReturnRawResponseWhenTemplateIsEmpty() {
		String rawResponse = "{\"status\": \"success\"}";
		String template = "";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals(rawResponse, result);
	}

	@Test
	void shouldReturnRawResponseWhenTemplateIsRootAccess() {
		String rawResponse = "{\"status\": \"success\"}";
		String template = "{{.}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals(rawResponse, result);
	}

	@Test
	void shouldExtractSingleLevelValue() {
		String rawResponse = "{\"status\": \"success\", \"code\": 200}";
		String template = "Status: {{.status}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Status: success", result);
	}

	@Test
	void shouldExtractNestedValue() {
		String rawResponse = "{\"location\": {\"province\": \"Zhejiang\", \"city\": \"Hangzhou\"}, \"code\": 200}";
		String template = "The city is {{.location.city}} and the code is {{.code}}.";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("The city is Hangzhou and the code is 200.", result);
	}

	@Test
	void shouldExtractDeeplyNestedValue() {
		String rawResponse = "{\"data\": {\"user\": {\"profile\": {\"name\": \"Alice\", \"age\": 30}}, \"timestamp\": 1234567890}}";
		String template = "User {{.data.user.profile.name}} is {{.data.user.profile.age}} years old.";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("User Alice is 30 years old.", result);
	}

	@Test
	void shouldHandleSpacesInTemplate() {
		String rawResponse = "{\"data\": {\"value\": \"test\"}}";
		String template = "The value is {{ .data.value }}.";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("The value is test.", result);
	}

	@Test
	void shouldHandleMultipleNestedReplacements() {
		String rawResponse = "{\"weather\": {\"temperature\": 25, \"humidity\": 60}, \"location\": {\"city\": \"Beijing\"}}";
		String template = "Temperature in {{.location.city}} is {{.weather.temperature}}°C with {{.weather.humidity}}% humidity.";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Temperature in Beijing is 25°C with 60% humidity.", result);
	}

	@Test
	void shouldHandleMissingNestedKey() {
		String rawResponse = "{\"location\": {\"city\": \"Hangzhou\"}}";
		String template = "City: {{.location.city}}, Country: {{.location.country}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("City: Hangzhou, Country: ", result);
	}

	@Test
	void shouldHandleArrayAccess() {
		String rawResponse = "{\"users\": [{\"name\": \"Alice\"}, {\"name\": \"Bob\"}]}";
		String template = "First user: {{.users.0.name}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("First user: Alice", result);
	}

	@Test
	void shouldWorkWithJsonPathWhenStartsWithDollar() {
		String rawResponse = "{\"location\": {\"city\": \"Hangzhou\"}}";
		String template = "$.location.city";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Hangzhou", result);
	}

	@Test
	void shouldFallbackToSimpleTemplateWhenNoMultiLevel() {
		String rawResponse = "{\"status\": \"success\", \"message\": \"OK\"}";
		String template = "{{.status}}: {{.message}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("success: OK", result);
	}

	@Test
	void shouldHandleNonJsonResponse() {
		String rawResponse = "Simple text response";
		String template = "Response: {{.}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Response: Simple text response", result);
	}

	@Test
	void shouldHandleComplexNestedStructure() {
		String rawResponse = "{\"api\": {\"response\": {\"data\": {\"items\": [{\"id\": 1, \"name\": \"Item1\"}, {\"id\": 2, \"name\": \"Item2\"}], \"total\": 2}, \"status\": {\"code\": 200, \"message\": \"success\"}}}}";
		String template = "Status: {{.api.response.status.message}}, Total items: {{.api.response.data.total}}, First item: {{.api.response.data.items.[0].name}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Status: success, Total items: 2, First item: Item1", result);
	}

	@Test
	void shouldHandleNumbersAndBooleans() {
		String rawResponse = "{\"config\": {\"enabled\": true, \"maxRetries\": 3, \"timeout\": 30.5}}";
		String template = "Enabled: {{.config.enabled}}, Max retries: {{.config.maxRetries}}, Timeout: {{.config.timeout}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		assertEquals("Enabled: true, Max retries: 3, Timeout: 30.5", result);
	}

	@Test
	void shouldHandleEmptyObjectsAndNulls() {
		String rawResponse = "{\"data\": null, \"empty\": {}, \"info\": {\"value\": \"test\"}}";
		String template = "Data: {{.data}}, Info: {{.info.value}}";

		String result = ResponseTemplateParser.parse(rawResponse, template);

		// Handlebars renders null values as empty strings, which is standard template
		// engine behavior
		assertEquals("Data: , Info: test", result);
	}

	// Regex pattern tests to validate MULTI_LEVEL_PATTERN detection
	@Test
	void shouldDetectMultiLevelPatternsCorrectly() {
		Pattern MULTI_LEVEL_PATTERN = Pattern.compile("\\{\\{\\s*\\.\\w+\\.[\\w\\.]+\\s*}}");

		// Should match multi-level patterns
		assertTrue(MULTI_LEVEL_PATTERN.matcher("{{.user.name}}").find());
		assertTrue(MULTI_LEVEL_PATTERN.matcher("{{ .location.city }}").find());
		assertTrue(MULTI_LEVEL_PATTERN.matcher("{{.data.user.profile.name}}").find());
		assertTrue(MULTI_LEVEL_PATTERN.matcher("{{.api.response.status.code}}").find());

		// Should NOT match single-level patterns
		assertFalse(MULTI_LEVEL_PATTERN.matcher("{{.}}").find());
		assertFalse(MULTI_LEVEL_PATTERN.matcher("{{.status}}").find());
		assertFalse(MULTI_LEVEL_PATTERN.matcher("{{.message}}").find());

		// Should NOT match non-template strings
		assertFalse(MULTI_LEVEL_PATTERN.matcher("plain text").find());
		assertFalse(MULTI_LEVEL_PATTERN.matcher("$.location.city").find());
	}

	@Test
	void shouldMatchTemplatePatternCorrectly() {
		Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w\\$\\[\\]\\.]*)\\s*}}", Pattern.DOTALL);

		// Should match all template patterns
		assertTrue(TEMPLATE_PATTERN.matcher("{{.}}").find());
		assertTrue(TEMPLATE_PATTERN.matcher("{{.status}}").find());
		assertTrue(TEMPLATE_PATTERN.matcher("{{.user.name}}").find());
		assertTrue(TEMPLATE_PATTERN.matcher("{{ .location.city }}").find());
		assertTrue(TEMPLATE_PATTERN.matcher("{{.users.[0].name}}").find());
		assertTrue(TEMPLATE_PATTERN.matcher("{{.data$}}").find());

		// Should NOT match non-template patterns
		assertFalse(TEMPLATE_PATTERN.matcher("$.location.city").find());
		assertFalse(TEMPLATE_PATTERN.matcher("plain text").find());
		assertFalse(TEMPLATE_PATTERN.matcher("{single brace}").find());
	}

}
