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
package com.alibaba.cloud.ai.dsl.adapters;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import com.alibaba.cloud.ai.model.workflow.nodedata.ToolNodeData;
import com.alibaba.cloud.ai.service.dsl.adapters.DifyDSLAdapter;
import com.alibaba.cloud.ai.service.dsl.nodes.ToolNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DifyDSLAdapterTest {

	@Test
	public void testToolNodeDataParsingWithDifyAttributes() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Simulate Dify tool node data with all attributes
		Map<String, Object> difyToolData = new HashMap<>();
		difyToolData.put("tool_name", "tool_current_time");
		difyToolData.put("tool_description", "该工具用于获取当前时间及星期几");
		difyToolData.put("tool_label", "tool_current_time");
		difyToolData.put("provider_id", "dad1403e-c67a-4bb4-a7b1-8051a2e01e6a");
		difyToolData.put("provider_name", "自定义工具");
		difyToolData.put("provider_type", "api");
		difyToolData.put("is_team_authorization", true);
		difyToolData.put("output_schema", null);

		Map<String, Object> toolParams = new HashMap<>();
		toolParams.put("format", Map.of("type", "mixed", "value", ""));
		toolParams.put("timezone", Map.of("type", "mixed", "value", ""));
		difyToolData.put("tool_parameters", toolParams);

		Map<String, Object> toolConfigs = new HashMap<>();
		difyToolData.put("tool_configurations", toolConfigs);

		// Parse the data
		ToolNodeData parsedData = converter.parseMapData(difyToolData, DSLDialectType.DIFY);

		// Verify that all attributes are correctly parsed
		assertNotNull(parsedData);
		assertEquals("tool_current_time", parsedData.getToolName());
		assertEquals("该工具用于获取当前时间及星期几", parsedData.getToolDescription());
		assertEquals("tool_current_time", parsedData.getToolLabel());
		assertEquals("dad1403e-c67a-4bb4-a7b1-8051a2e01e6a", parsedData.getProviderId());
		assertEquals("自定义工具", parsedData.getProviderName());
		assertEquals("api", parsedData.getProviderType());
		assertTrue(parsedData.getIsTeamAuthorization());
		assertNull(parsedData.getOutputSchema());

		// Verify tool_names is populated from tool_name
		assertNotNull(parsedData.getToolNames());
		assertEquals(1, parsedData.getToolNames().size());
		assertEquals("tool_current_time", parsedData.getToolNames().get(0));

		// Verify tool parameters
		assertNotNull(parsedData.getToolParameters());
		assertTrue(parsedData.getToolParameters().containsKey("format"));
		assertTrue(parsedData.getToolParameters().containsKey("timezone"));

		// Verify dump functionality
		Map<String, Object> dumpedData = converter.dumpMapData(parsedData, DSLDialectType.DIFY);
		assertNotNull(dumpedData);
		assertEquals("tool_current_time", dumpedData.get("tool_name"));
		assertEquals("该工具用于获取当前时间及星期几", dumpedData.get("tool_description"));
		assertEquals("tool_current_time", dumpedData.get("tool_label"));
		assertEquals("dad1403e-c67a-4bb4-a7b1-8051a2e01e6a", dumpedData.get("provider_id"));
		assertEquals("自定义工具", dumpedData.get("provider_name"));
		assertEquals("api", dumpedData.get("provider_type"));
		assertTrue((Boolean) dumpedData.get("is_team_authorization"));

		System.out.println("✅ Tool node data parsing with Dify attributes test passed");
	}

	@Test
	public void testToolNodeDataParsingWithMinimalAttributes() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Test with minimal attributes
		Map<String, Object> minimalToolData = new HashMap<>();
		minimalToolData.put("tool_name", "simple_tool");

		// Parse the data
		ToolNodeData parsedData = converter.parseMapData(minimalToolData, DSLDialectType.DIFY);

		// Verify basic parsing
		assertNotNull(parsedData);
		assertEquals("simple_tool", parsedData.getToolName());
		assertNotNull(parsedData.getToolNames());
		assertEquals(1, parsedData.getToolNames().size());
		assertEquals("simple_tool", parsedData.getToolNames().get(0));

		// Verify null/empty values
		assertNull(parsedData.getToolDescription());
		assertNull(parsedData.getProviderId());
		assertNotNull(parsedData.getToolCallbacks());
		assertTrue(parsedData.getToolCallbacks().isEmpty());

		System.out.println(" Tool node data parsing with minimal attributes test passed");
	}

	@Test
	public void testToolNodeDataParsingWithToolNamesArray() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Test with tool_names array (should take precedence over tool_name)
		Map<String, Object> toolData = new HashMap<>();
		toolData.put("tool_name", "single_tool");
		toolData.put("tool_names", java.util.Arrays.asList("tool1", "tool2", "tool3"));
		toolData.put("tool_callbacks", java.util.Arrays.asList("callback1", "callback2"));

		// Parse the data
		ToolNodeData parsedData = converter.parseMapData(toolData, DSLDialectType.DIFY);

		// Verify that tool_names array is used instead of single tool_name
		assertNotNull(parsedData);
		assertEquals("single_tool", parsedData.getToolName());
		assertNotNull(parsedData.getToolNames());
		assertEquals(3, parsedData.getToolNames().size());
		assertTrue(parsedData.getToolNames().contains("tool1"));
		assertTrue(parsedData.getToolNames().contains("tool2"));
		assertTrue(parsedData.getToolNames().contains("tool3"));

		// Verify callbacks
		assertNotNull(parsedData.getToolCallbacks());
		assertEquals(2, parsedData.getToolCallbacks().size());
		assertTrue(parsedData.getToolCallbacks().contains("callback1"));
		assertTrue(parsedData.getToolCallbacks().contains("callback2"));

		System.out.println(" Tool node data parsing with tool_names array test passed");
	}

	@Test
	public void testToolNodeDataParsingWithComplexParameters() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Test with complex parameter schemas
		Map<String, Object> complexToolData = new HashMap<>();
		complexToolData.put("tool_name", "complex_tool");
		complexToolData.put("tool_description", "A complex tool with parameters");
		complexToolData.put("provider_type", "builtin");
		complexToolData.put("is_team_authorization", false);

		// Complex parameter structure
		Map<String, Object> paramSchemas = new HashMap<>();
		Map<String, Object> stringParam = new HashMap<>();
		stringParam.put("type", "string");
		stringParam.put("required", true);
		stringParam.put("default", "default_value");
		stringParam.put("description", "A string parameter");
		paramSchemas.put("string_param", stringParam);

		Map<String, Object> numberParam = new HashMap<>();
		numberParam.put("type", "number");
		numberParam.put("required", false);
		numberParam.put("min", 0);
		numberParam.put("max", 100);
		paramSchemas.put("number_param", numberParam);

		complexToolData.put("tool_parameters", paramSchemas);

		// Complex output schema
		Map<String, Object> outputSchema = new HashMap<>();
		outputSchema.put("type", "object");
		outputSchema.put("properties", Map.of("result", Map.of("type", "string"), "status", Map.of("type", "boolean")));
		complexToolData.put("output_schema", outputSchema);

		// Parse the data
		ToolNodeData parsedData = converter.parseMapData(complexToolData, DSLDialectType.DIFY);

		// Verify complex attributes
		assertNotNull(parsedData);
		assertEquals("complex_tool", parsedData.getToolName());
		assertEquals("A complex tool with parameters", parsedData.getToolDescription());
		assertEquals("builtin", parsedData.getProviderType());
		assertFalse(parsedData.getIsTeamAuthorization());

		// Verify complex parameters
		assertNotNull(parsedData.getToolParameters());
		assertTrue(parsedData.getToolParameters().containsKey("string_param"));
		assertTrue(parsedData.getToolParameters().containsKey("number_param"));

		@SuppressWarnings("unchecked")
		Map<String, Object> stringParamParsed = (Map<String, Object>) parsedData.getToolParameters()
			.get("string_param");
		assertEquals("string", stringParamParsed.get("type"));
		assertEquals(true, stringParamParsed.get("required"));

		// Verify output schema
		assertNotNull(parsedData.getOutputSchema());
		@SuppressWarnings("unchecked")
		Map<String, Object> outputSchemaParsed = (Map<String, Object>) parsedData.getOutputSchema();
		assertEquals("object", outputSchemaParsed.get("type"));

		System.out.println(" Tool node data parsing with complex parameters test passed");
	}

	@Test
	public void testToolNodeDataRoundTripConversion() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Create comprehensive tool data
		Map<String, Object> originalData = new HashMap<>();
		originalData.put("tool_name", "roundtrip_tool");
		originalData.put("tool_description", "Test roundtrip conversion");
		originalData.put("tool_label", "Roundtrip Tool");
		originalData.put("provider_id", "test-provider-123");
		originalData.put("provider_name", "Test Provider");
		originalData.put("provider_type", "custom");
		originalData.put("is_team_authorization", true);
		originalData.put("llm_response_key", "llm_output");
		originalData.put("output_key", "tool_result");

		Map<String, Object> toolParams = new HashMap<>();
		toolParams.put("param1", Map.of("type", "string", "value", "test"));
		toolParams.put("param2", Map.of("type", "number", "value", 42));
		originalData.put("tool_parameters", toolParams);

		Map<String, Object> toolConfigs = new HashMap<>();
		toolConfigs.put("timeout", 30);
		toolConfigs.put("retry_count", 3);
		originalData.put("tool_configurations", toolConfigs);

		// Parse and dump back
		ToolNodeData parsedData = converter.parseMapData(originalData, DSLDialectType.DIFY);
		Map<String, Object> dumpedData = converter.dumpMapData(parsedData, DSLDialectType.DIFY);

		// Verify roundtrip conversion preserves all data
		assertEquals(originalData.get("tool_name"), dumpedData.get("tool_name"));
		assertEquals(originalData.get("tool_description"), dumpedData.get("tool_description"));
		assertEquals(originalData.get("tool_label"), dumpedData.get("tool_label"));
		assertEquals(originalData.get("provider_id"), dumpedData.get("provider_id"));
		assertEquals(originalData.get("provider_name"), dumpedData.get("provider_name"));
		assertEquals(originalData.get("provider_type"), dumpedData.get("provider_type"));
		assertEquals(originalData.get("is_team_authorization"), dumpedData.get("is_team_authorization"));
		assertEquals(originalData.get("llm_response_key"), dumpedData.get("llm_response_key"));
		assertEquals(originalData.get("output_key"), dumpedData.get("output_key"));

		// Verify complex objects are preserved
		assertNotNull(dumpedData.get("tool_parameters"));
		assertNotNull(dumpedData.get("tool_configurations"));

		System.out.println(" Tool node data roundtrip conversion test passed");
	}

	@Test
	public void testToolNodeDataParsingWithNullValues() {
		ToolNodeDataConverter converter = new ToolNodeDataConverter();

		// Test with explicit null values
		Map<String, Object> nullValueData = new HashMap<>();
		nullValueData.put("tool_name", "null_test_tool");
		nullValueData.put("tool_description", null);
		nullValueData.put("provider_id", null);
		nullValueData.put("is_team_authorization", null);
		nullValueData.put("output_schema", null);
		nullValueData.put("tool_parameters", null);
		nullValueData.put("tool_configurations", null);

		// Parse the data
		ToolNodeData parsedData = converter.parseMapData(nullValueData, DSLDialectType.DIFY);

		// Verify null handling
		assertNotNull(parsedData);
		assertEquals("null_test_tool", parsedData.getToolName());
		assertNull(parsedData.getToolDescription());
		assertNull(parsedData.getProviderId());
		assertNull(parsedData.getIsTeamAuthorization());
		assertNull(parsedData.getOutputSchema());
		assertNull(parsedData.getToolParameters());
		assertNull(parsedData.getToolConfigurations());

		// Verify tool_names is still populated from tool_name
		assertNotNull(parsedData.getToolNames());
		assertEquals(1, parsedData.getToolNames().size());
		assertEquals("null_test_tool", parsedData.getToolNames().get(0));

		System.out.println(" Tool node data parsing with null values test passed");
	}

	@Test
	public void testVariableConversionWithArrayValue() {
		DifyDSLAdapter adapter = new DifyDSLAdapter(List.of(), null);

		// Construct workflow data containing array values, simulating actual Dify DSL
		Map<String, Object> workflowData = new HashMap<>();

		// Create conversation_variables that include array-type values
		List<Map<String, Object>> conversationVars = new ArrayList<>();

		// First variable: array-type tags
		Map<String, Object> tagsVar = new HashMap<>();
		tagsVar.put("description", "可打标签列表");
		tagsVar.put("id", "4f0e8dbe-d4f3-48d3-9632-ec9bf331db69");
		tagsVar.put("name", "tags");
		tagsVar.put("selector", List.of("conversation", "tags"));
		tagsVar.put("value", List.of()); // Empty array
		tagsVar.put("value_type", "array[object]");
		conversationVars.add(tagsVar);

		// Second variable: string-type special_rules
		Map<String, Object> rulesVar = new HashMap<>();
		rulesVar.put("description", "回答规则");
		rulesVar.put("id", "82778541-b684-4044-8edd-d0832b6fd002");
		rulesVar.put("name", "special_rules");
		rulesVar.put("selector", List.of("conversation", "special_rules"));
		rulesVar.put("value", "");
		rulesVar.put("value_type", "string");
		conversationVars.add(rulesVar);

		// Third variable: array containing complex objects
		Map<String, Object> complexVar = new HashMap<>();
		complexVar.put("description", "复杂对象数组");
		complexVar.put("id", "test-complex-array");
		complexVar.put("name", "complex_array");
		complexVar.put("selector", List.of("conversation", "complex"));
		complexVar.put("value", List.of(Map.of("key1", "value1", "nested", Map.of("prop", 123)),
				Map.of("key2", "value2", "array", List.of(1, 2, 3))));
		complexVar.put("value_type", "array[object]");
		conversationVars.add(complexVar);

		workflowData.put("conversation_variables", conversationVars);

		// Add an empty graph to meet basic requirements
		Map<String, Object> graph = new HashMap<>();
		graph.put("nodes", List.of());
		graph.put("edges", List.of());
		workflowData.put("graph", graph);

		Map<String, Object> dslData = new HashMap<>();
		dslData.put("workflow", workflowData);

		// Call mapToWorkflow method, this should not throw an exception
		assertDoesNotThrow(() -> {
			Workflow workflow = adapter.mapToWorkflow(dslData);
			assertNotNull(workflow);

			// Verify variables are correctly parsed
			List<Variable> vars = workflow.getWorkflowVars();
			assertNotNull(vars);
			assertEquals(3, vars.size());

			// Check first variable (array type)
			Variable tagsVariable = vars.stream().filter(v -> "tags".equals(v.getName())).findFirst().orElse(null);
			assertNotNull(tagsVariable);
			assertEquals("可打标签列表", tagsVariable.getDescription());
			assertEquals("array[object]", tagsVariable.getValueType());
			assertEquals("[]", tagsVariable.getValue()); // Array should be serialized as
															// JSON string

			// Check second variable (string type)
			Variable rulesVariable = vars.stream()
				.filter(v -> "special_rules".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(rulesVariable);
			assertEquals("回答规则", rulesVariable.getDescription());
			assertEquals("string", rulesVariable.getValueType());
			assertEquals("", rulesVariable.getValue());

			// Check third variable (complex array type)
			Variable complexVariable = vars.stream()
				.filter(v -> "complex_array".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(complexVariable);
			assertEquals("复杂对象数组", complexVariable.getDescription());
			assertEquals("array[object]", complexVariable.getValueType());
			// Value should be JSON string representation of the array
			String value = complexVariable.getValue();
			assertNotNull(value);
			assertTrue(value.startsWith("["));
			assertTrue(value.endsWith("]"));
			assertTrue(value.contains("key1"));
			assertTrue(value.contains("value1"));
		});

		System.out.println("Variable conversion with array value test passed");
	}

	@Test
	public void testSpecificErrorScenarioFromIssue() {
		DifyDSLAdapter adapter = new DifyDSLAdapter(List.of(), null);

		// This test simulates the exact error scenario reported by the user
		Map<String, Object> workflowData = new HashMap<>();

		// Create conversation_variables structure identical to the reported issue
		List<Map<String, Object>> conversationVars = new ArrayList<>();

		// First variable: tags, value is empty array, value_type is array[object]
		Map<String, Object> tagsVar = new HashMap<>();
		tagsVar.put("description", "可打标签列表");
		tagsVar.put("id", "4f0e8dbe-d4f3-48d3-9632-ec9bf331db69");
		tagsVar.put("name", "tags");
		tagsVar.put("selector", List.of("conversation", "tags"));
		tagsVar.put("value", List.of()); // This is exactly what caused the error!
		tagsVar.put("value_type", "array[object]");
		conversationVars.add(tagsVar);

		// Second variable: special_rules, value is empty string
		Map<String, Object> rulesVar = new HashMap<>();
		rulesVar.put("description", "回答规则");
		rulesVar.put("id", "82778541-b684-4044-8edd-d0832b6fd002");
		rulesVar.put("name", "special_rules");
		rulesVar.put("selector", List.of("conversation", "special_rules"));
		rulesVar.put("value", "");
		rulesVar.put("value_type", "string");
		conversationVars.add(rulesVar);

		// Third variable: conclusion, value is empty string
		Map<String, Object> conclusionVar = new HashMap<>();
		conclusionVar.put("description", "结束语");
		conclusionVar.put("id", "d53189c0-31bd-47eb-be1b-a0f61b11f492");
		conclusionVar.put("name", "conclusion");
		conclusionVar.put("selector", List.of("conversation", "conclusion"));
		conclusionVar.put("value", "");
		conclusionVar.put("value_type", "string");
		conversationVars.add(conclusionVar);

		workflowData.put("conversation_variables", conversationVars);

		// Add empty graph
		Map<String, Object> graph = new HashMap<>();
		graph.put("nodes", List.of());
		graph.put("edges", List.of());
		workflowData.put("graph", graph);

		Map<String, Object> dslData = new HashMap<>();
		dslData.put("workflow", workflowData);

		// This call would previously throw IllegalArgumentException:
		// "Cannot deserialize value of type java.lang.String from Array value"
		// Now it should work properly
		assertDoesNotThrow(() -> {
			Workflow workflow = adapter.mapToWorkflow(dslData);
			assertNotNull(workflow);

			// Verify variables are parsed correctly
			List<Variable> vars = workflow.getWorkflowVars();
			assertNotNull(vars);
			assertEquals(3, vars.size());

			// Verify tags variable (the one that originally caused the issue)
			Variable tagsVariable = vars.stream().filter(v -> "tags".equals(v.getName())).findFirst().orElse(null);
			assertNotNull(tagsVariable, "tags variable should be parsed correctly");
			assertEquals("可打标签列表", tagsVariable.getDescription());
			assertEquals("array[object]", tagsVariable.getValueType());
			assertEquals("[]", tagsVariable.getValue()); // Empty array serialized as "[]"

			// Verify other variables still work normally
			Variable rulesVariable = vars.stream()
				.filter(v -> "special_rules".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(rulesVariable, "special_rules variable should be parsed correctly");
			assertEquals("", rulesVariable.getValue());

			Variable conclusionVariable = vars.stream()
				.filter(v -> "conclusion".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(conclusionVariable, "conclusion variable should be parsed correctly");
			assertEquals("", conclusionVariable.getValue());
		});

		System.out.println("Specific error scenario from issue #2017 test passed");
	}

	@Test
	public void testVariableValueTypesEdgeCases() {
		DifyDSLAdapter adapter = new DifyDSLAdapter(List.of(), null);

		Map<String, Object> workflowData = new HashMap<>();
		List<Map<String, Object>> conversationVars = new ArrayList<>();

		// Test various types of value values

		// 1. null value
		Map<String, Object> nullVar = new HashMap<>();
		nullVar.put("name", "null_value");
		nullVar.put("value", null);
		nullVar.put("value_type", "string");
		conversationVars.add(nullVar);

		// 2. Number value
		Map<String, Object> numberVar = new HashMap<>();
		numberVar.put("name", "number_value");
		numberVar.put("value", 42);
		numberVar.put("value_type", "number");
		conversationVars.add(numberVar);

		// 3. Boolean value
		Map<String, Object> boolVar = new HashMap<>();
		boolVar.put("name", "bool_value");
		boolVar.put("value", true);
		boolVar.put("value_type", "boolean");
		conversationVars.add(boolVar);

		// 4. Nested object
		Map<String, Object> objectVar = new HashMap<>();
		objectVar.put("name", "object_value");
		objectVar.put("value", Map.of("nested", Map.of("deep", "value"), "array", List.of(1, 2, 3)));
		objectVar.put("value_type", "object");
		conversationVars.add(objectVar);

		// 5. Array containing different types of elements
		Map<String, Object> mixedArrayVar = new HashMap<>();
		mixedArrayVar.put("name", "mixed_array");
		List<Object> mixedList = new ArrayList<>();
		mixedList.add("string");
		mixedList.add(123);
		mixedList.add(true);
		mixedList.add(Map.of("key", "value"));
		mixedList.add(null);
		mixedArrayVar.put("value", mixedList);
		mixedArrayVar.put("value_type", "array[mixed]");
		conversationVars.add(mixedArrayVar);

		workflowData.put("conversation_variables", conversationVars);

		// Add empty graph
		Map<String, Object> graph = new HashMap<>();
		graph.put("nodes", List.of());
		graph.put("edges", List.of());
		workflowData.put("graph", graph);

		Map<String, Object> dslData = new HashMap<>();
		dslData.put("workflow", workflowData);

		// All these edge cases should be handled correctly
		assertDoesNotThrow(() -> {
			Workflow workflow = adapter.mapToWorkflow(dslData);
			assertNotNull(workflow);

			List<Variable> vars = workflow.getWorkflowVars();
			assertNotNull(vars);
			assertEquals(5, vars.size());

			// Verify null value variable
			Variable nullVariable = vars.stream()
				.filter(v -> "null_value".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(nullVariable);
			assertNull(nullVariable.getValue()); // null value should remain null

			// Verify number value variable
			Variable numberVariable = vars.stream()
				.filter(v -> "number_value".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(numberVariable);
			assertEquals("42", numberVariable.getValue());

			// Verify boolean value variable
			Variable boolVariable = vars.stream()
				.filter(v -> "bool_value".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(boolVariable);
			assertEquals("true", boolVariable.getValue());

			// Verify object value variable (should be serialized as JSON)
			Variable objectVariable = vars.stream()
				.filter(v -> "object_value".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(objectVariable);
			String objectValue = objectVariable.getValue();
			assertTrue(objectValue.contains("nested"));
			assertTrue(objectValue.contains("array"));

			// Verify mixed array variable
			Variable mixedArrayVariable = vars.stream()
				.filter(v -> "mixed_array".equals(v.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(mixedArrayVariable);
			String mixedArrayValue = mixedArrayVariable.getValue();
			assertTrue(mixedArrayValue.startsWith("["));
			assertTrue(mixedArrayValue.endsWith("]"));
			assertTrue(mixedArrayValue.contains("string"));
			assertTrue(mixedArrayValue.contains("123"));
		});

		System.out.println(" Variable value types edge cases test passed");
	}

}
