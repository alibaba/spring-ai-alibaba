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
package com.alibaba.cloud.ai.graph.agent.renderer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.template.ValidationMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SaaStTemplateRenderer}.
 *
 * @author Spring AI Alibaba
 */
class SaaStTemplateRendererTest {

	@Test
	void testBasicCharDelimiter() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiterToken('{')
				.endDelimiterToken('}')
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello {name}!";
		Map<String, Object> variables = Map.of("name", "World");

		String result = renderer.apply(template, variables);
		assertEquals("Hello World!", result);
	}

	@Test
	void testStringDelimiter() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello {{name}}!";
		Map<String, Object> variables = Map.of("name", "World");

		String result = renderer.apply(template, variables);
		assertEquals("Hello World!", result);
	}

	@Test
	void testJsonContentWithStringDelimiter() {
		// 测试使用多字符 delimiter 来避免与 JSON 内容冲突
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				请处理以下 JSON 数据：{"name": "test", "value": 123}
				用户信息：{{userName}}
				数据内容：{{jsonData}}
				""";

		Map<String, Object> variables = Map.of(
				"userName", "张三",
				"jsonData", "{\"key\": \"value\"}"
		);

		String result = renderer.apply(template, variables);

		// 验证 JSON 中的 {} 没有被替换
		assertTrue(result.contains("{\"name\": \"test\", \"value\": 123}"));
		// 验证模板变量被正确替换
		assertTrue(result.contains("用户信息：张三"));
		assertTrue(result.contains("数据内容：{\"key\": \"value\"}"));
	}

	@Test
	void testJsonContentConflictWithSingleCharDelimiter() {
		// 测试单字符 delimiter 与 JSON 内容冲突的情况
		// 实现应该能够自动识别并保护 JSON 内容，避免与模板变量冲突
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiterToken('{')
				.endDelimiterToken('}')
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				请处理以下 JSON 数据：{"name": "test", "value": 123}
				用户信息：{userName}
				""";

		Map<String, Object> variables = Map.of("userName", "张三");

		// 实现应该能够识别 JSON 内容并保护它，同时正确替换模板变量
		String result = renderer.apply(template, variables);

		// 验证 JSON 内容被保护（没有被误替换）
		assertTrue(result.contains("{\"name\": \"test\", \"value\": 123}"));
		// 验证模板变量被正确替换
		assertTrue(result.contains("用户信息：张三"));
	}

	@Test
	void testComplexJsonWithStringDelimiter() {
		// 测试复杂的 JSON 内容与模板变量混合
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				{
				  "request": {
				    "user": "{{userName}}",
				    "data": {"key": "value", "count": {{count}}},
				    "metadata": {"type": "test", "nested": {"level": 2}}
				  },
				  "response": "{{responseText}}"
				}
				""";

		Map<String, Object> variables = Map.of(
				"userName", "Alice",
				"count", "42",
				"responseText", "Success"
		);

		String result = renderer.apply(template, variables);

		// 验证变量被正确替换
		assertTrue(result.contains("\"user\": \"Alice\""));
		assertTrue(result.contains("\"count\": 42"));
		assertTrue(result.contains("\"response\": \"Success\""));
		// 验证 JSON 结构中的普通 {} 没有被误替换
		assertTrue(result.contains("\"metadata\": {\"type\": \"test\""));
	}

	@Test
	void testNestedDelimiters() {
		// 测试嵌套的 delimiter
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Outer: {{outer}}, Inner JSON: {\"key\": \"{{value}}\"}";
		Map<String, Object> variables = Map.of(
				"outer", "OUTER_VALUE",
				"value", "VALUE"
		);

		String result = renderer.apply(template, variables);
		assertTrue(result.contains("Outer: OUTER_VALUE"));
		assertTrue(result.contains("\"key\": \"VALUE\""));
	}

	@Test
	void testPropertyAccess() {
		// 测试属性访问
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		Map<String, Object> user = Map.of("name", "John", "age", 30);
		String template = "User: {{user.name}}, Age: {{user.age}}";
		Map<String, Object> variables = Map.of("user", user);

		String result = renderer.apply(template, variables);
		assertTrue(result.contains("User: John"));
		assertTrue(result.contains("Age: 30"));
	}

	@Test
	void testMultipleVariables() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "{{greeting}} {{name}}, today is {{day}}";
		Map<String, Object> variables = Map.of(
				"greeting", "Hello",
				"name", "Alice",
				"day", "Monday"
		);

		String result = renderer.apply(template, variables);
		assertEquals("Hello Alice, today is Monday", result);
	}

	@Test
	void testValidationModeThrow() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.THROW)
				.build();

		String template = "Hello {{name}}, missing: {{missing}}";
		Map<String, Object> variables = Map.of("name", "World");

		assertThrows(IllegalStateException.class, () -> {
			renderer.apply(template, variables);
		});
	}

	@Test
	void testValidationModeWarn() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.WARN)
				.build();

		String template = "Hello {{name}}, missing: {{missing}}";
		Map<String, Object> variables = Map.of("name", "World");

		// Should not throw, but log warning
		String result = renderer.apply(template, variables);
		assertTrue(result.contains("Hello World"));
	}

	@Test
	void testValidationModeNone() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello {{name}}, missing: {{missing}}";
		Map<String, Object> variables = Map.of("name", "World");

		// Should not throw
		String result = renderer.apply(template, variables);
		assertTrue(result.contains("Hello World"));
	}

	@Test
	void testJsonArrayWithStringDelimiter() {
		// 测试包含 JSON 数组的情况
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				{
				  "items": [{"id": 1, "name": "item1"}, {"id": 2, "name": "item2"}],
				  "user": "{{userName}}",
				  "count": {{count}}
				}
				""";

		Map<String, Object> variables = Map.of(
				"userName", "Bob",
				"count", "10"
		);

		String result = renderer.apply(template, variables);

		// 验证 JSON 数组中的 {} 没有被误替换
		assertTrue(result.contains("\"items\": [{\"id\": 1"));
		// 验证模板变量被正确替换
		assertTrue(result.contains("\"user\": \"Bob\""));
		assertTrue(result.contains("\"count\": 10"));
	}

	@Test
	void testMixedContent() {
		// 测试混合内容：既有 JSON，又有普通文本和模板变量
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				用户 {{userName}} 提交了以下数据：
				{"type": "request", "data": {"key": "value"}}
				处理结果：{{result}}
				时间：{{timestamp}}
				""";

		Map<String, Object> variables = Map.of(
				"userName", "Charlie",
				"result", "Success",
				"timestamp", "2024-01-01"
		);

		String result = renderer.apply(template, variables);

		// 验证所有变量都被替换
		assertTrue(result.contains("用户 Charlie 提交了以下数据："));
		assertTrue(result.contains("处理结果：Success"));
		assertTrue(result.contains("时间：2024-01-01"));
		// 验证 JSON 内容保持不变
		assertTrue(result.contains("{\"type\": \"request\""));
	}

	@Test
	void testBuilderWithCharDelimiters() {
		// 测试 Builder 使用 char delimiter
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiterToken('<')
				.endDelimiterToken('>')
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello <name>!";
		Map<String, Object> variables = Map.of("name", "World");

		String result = renderer.apply(template, variables);
		assertEquals("Hello World!", result);
	}

	@Test
	void testBuilderWithStringDelimiters() {
		// 测试 Builder 使用 String delimiter
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("<<")
				.endDelimiter(">>")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello <<name>>!";
		Map<String, Object> variables = Map.of("name", "World");

		String result = renderer.apply(template, variables);
		assertEquals("Hello World!", result);
	}

	@Test
	void testEmptyTemplate() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "";
		Map<String, Object> variables = Map.of();

		assertThrows(IllegalArgumentException.class, () -> {
			renderer.apply(template, variables);
		});
	}

	@Test
	void testNullVariables() {
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = "Hello {{name}}!";

		assertThrows(IllegalArgumentException.class, () -> {
			renderer.apply(template, null);
		});
	}

	@Test
	void testComplexNestedJson() {
		// 测试复杂的嵌套 JSON 结构
		SaaStTemplateRenderer renderer = SaaStTemplateRenderer.builder()
				.startDelimiter("{{")
				.endDelimiter("}}")
				.validationMode(ValidationMode.NONE)
				.build();

		String template = """
				{
				  "level1": {
				    "level2": {
				      "level3": {
				        "value": "{{value}}",
				        "items": [{"a": 1}, {"b": 2}]
				      }
				    },
				    "user": "{{userName}}"
				  }
				}
				""";

		Map<String, Object> variables = Map.of(
				"value", "test",
				"userName", "User"
		);

		String result = renderer.apply(template, variables);

		// 验证深层嵌套的变量被替换
		assertTrue(result.contains("\"value\": \"test\""));
		assertTrue(result.contains("\"user\": \"User\""));
		// 验证 JSON 结构完整
		assertTrue(result.contains("\"level3\": {"));
		assertTrue(result.contains("\"items\": [{\"a\": 1}"));
	}

}

