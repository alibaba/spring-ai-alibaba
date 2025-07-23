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
package com.alibaba.cloud.ai.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.TemplateTransformNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.nodes.TemplateTransformNodeDataConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateTransformNodeDataConverterTest {

	private TemplateTransformNodeDataConverter converter;

	@BeforeEach
	public void setUp() {
		converter = new TemplateTransformNodeDataConverter();
	}

	@Test
	public void testSupportNodeType() {
		assertTrue(converter.supportNodeType(NodeType.TEMPLATE_TRANSFORM));
		assertFalse(converter.supportNodeType(NodeType.CODE));
		assertFalse(converter.supportNodeType(NodeType.LLM));
		assertFalse(converter.supportNodeType(NodeType.HTTP));
	}

	@Test
	public void testGenerateVarName() {
		assertEquals("templateTransformNode1", converter.generateVarName(1));
		assertEquals("templateTransformNode5", converter.generateVarName(5));
		assertEquals("templateTransformNode10", converter.generateVarName(10));
	}

	@Test
	public void testPostProcess() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		String varName = "testNode";

		converter.postProcess(nodeData, varName);

		assertEquals("testNode_output", nodeData.getOutputKey());
		assertEquals(1, nodeData.getOutputs().size());
		assertEquals("testNode_output", nodeData.getOutputs().get(0).getName());
		assertEquals(VariableType.STRING.value(), nodeData.getOutputs().get(0).getValueType());
	}

	@Test
	public void testPostProcessWithExistingOutputKey() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		nodeData.setOutputKey("existing_output");
		String varName = "testNode";

		converter.postProcess(nodeData, varName);

		// 如果已有outputKey，不应该改变
		assertEquals("existing_output", nodeData.getOutputKey());
		assertEquals(1, nodeData.getOutputs().size());
		assertEquals("existing_output", nodeData.getOutputs().get(0).getName());
		assertEquals(VariableType.STRING.value(), nodeData.getOutputs().get(0).getValueType());
	}

	@Test
	public void testParseMapDataDify() {
		// 创建Dify格式的测试数据
		Map<String, Object> data = new HashMap<>();
		data.put("template", "Hello {{name}}, welcome to {{place}}!");

		List<Map<String, Object>> variables = new ArrayList<>();
		Map<String, Object> var1 = new HashMap<>();
		var1.put("variable", "name");
		var1.put("value_selector", List.of("user", "name"));
		variables.add(var1);

		Map<String, Object> var2 = new HashMap<>();
		var2.put("variable", "place");
		var2.put("value_selector", List.of("system", "location"));
		variables.add(var2);

		data.put("variables", variables);

		TemplateTransformNodeData result = converter.parseMapData(data, DSLDialectType.DIFY);

		assertNotNull(result);
		assertEquals("Hello {{name}}, welcome to {{place}}!", result.getTemplate());
		assertEquals(2, result.getInputs().size());
		assertEquals(1, result.getOutputs().size());

		// 验证第一个输入变量
		VariableSelector input1 = result.getInputs().get(0);
		assertEquals("user", input1.getNamespace());
		assertEquals("name", input1.getName());
		assertEquals("name", input1.getLabel());

		// 验证第二个输入变量
		VariableSelector input2 = result.getInputs().get(1);
		assertEquals("system", input2.getNamespace());
		assertEquals("location", input2.getName());
		assertEquals("place", input2.getLabel());

		// 验证输出变量
		Variable output = result.getOutputs().get(0);
		assertEquals("result", output.getName());
		assertEquals(VariableType.STRING.value(), output.getValueType());
	}

	@Test
	public void testParseMapDataDifyWithoutVariables() {
		// 测试没有变量的情况
		Map<String, Object> data = new HashMap<>();
		data.put("template", "Static template without variables");

		TemplateTransformNodeData result = converter.parseMapData(data, DSLDialectType.DIFY);

		assertNotNull(result);
		assertEquals("Static template without variables", result.getTemplate());
		assertTrue(result.getInputs().isEmpty());
		assertEquals(1, result.getOutputs().size());
		assertEquals("result", result.getOutputs().get(0).getName());
	}

	@Test
	public void testDumpMapDataDify() {
		// 创建测试的NodeData
		List<VariableSelector> inputs = new ArrayList<>();
		inputs.add(new VariableSelector("user", "name", "userName"));
		inputs.add(new VariableSelector("system", "place", "location"));

		List<Variable> outputs = new ArrayList<>();
		outputs.add(new Variable("result", VariableType.STRING.value()));

		TemplateTransformNodeData nodeData = new TemplateTransformNodeData(inputs, outputs);
		nodeData.setTemplate("Hello {{userName}}, welcome to {{location}}!");

		Map<String, Object> result = converter.dumpMapData(nodeData, DSLDialectType.DIFY);

		assertNotNull(result);
		assertEquals("Hello {{userName}}, welcome to {{location}}!", result.get("template"));

		// 验证variables
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> variables = (List<Map<String, Object>>) result.get("variables");
		assertNotNull(variables);
		assertEquals(2, variables.size());

		Map<String, Object> var1 = variables.get(0);
		assertEquals("userName", var1.get("variable"));
		assertEquals(List.of("user", "name"), var1.get("value_selector"));

		Map<String, Object> var2 = variables.get(1);
		assertEquals("location", var2.get("variable"));
		assertEquals(List.of("system", "place"), var2.get("value_selector"));

		// 验证outputs
		@SuppressWarnings("unchecked")
		Map<String, Object> outputs_map = (Map<String, Object>) result.get("outputs");
		assertNotNull(outputs_map);

		@SuppressWarnings("unchecked")
		Map<String, Object> resultOutput = (Map<String, Object>) outputs_map.get("result");
		assertEquals(VariableType.STRING.difyValue(), resultOutput.get("type"));
	}

	@Test
	public void testParseMapDataCustom() {
		// 测试CUSTOM方言的解析
		Map<String, Object> data = new HashMap<>();
		data.put("template", "Custom template");
		data.put("outputKey", "custom_output");

		TemplateTransformNodeData result = converter.parseMapData(data, DSLDialectType.CUSTOM);

		assertNotNull(result);
		assertEquals("Custom template", result.getTemplate());
		assertEquals("custom_output", result.getOutputKey());
	}

	@Test
	public void testDumpMapDataCustom() {
		// 测试CUSTOM方言的导出
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		nodeData.setTemplate("Custom template");
		nodeData.setOutputKey("custom_output");

		Map<String, Object> result = converter.dumpMapData(nodeData, DSLDialectType.CUSTOM);

		assertNotNull(result);
		assertEquals("Custom template", result.get("template"));
		assertEquals("custom_output", result.get("outputKey"));
	}

	@Test
	public void testRoundTripDify() {
		// 测试Dify格式的往返转换
		Map<String, Object> originalData = new HashMap<>();
		originalData.put("template", "Round trip test: {{input}}");

		List<Map<String, Object>> variables = new ArrayList<>();
		Map<String, Object> var1 = new HashMap<>();
		var1.put("variable", "input");
		var1.put("value_selector", List.of("source", "data"));
		variables.add(var1);
		originalData.put("variables", variables);

		// 解析
		TemplateTransformNodeData nodeData = converter.parseMapData(originalData, DSLDialectType.DIFY);

		// 导出
		Map<String, Object> exportedData = converter.dumpMapData(nodeData, DSLDialectType.DIFY);

		// 验证往返转换的正确性
		assertEquals(originalData.get("template"), exportedData.get("template"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> exportedVars = (List<Map<String, Object>>) exportedData.get("variables");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> originalVars = (List<Map<String, Object>>) originalData.get("variables");

		assertEquals(originalVars.size(), exportedVars.size());
		assertEquals(originalVars.get(0).get("variable"), exportedVars.get(0).get("variable"));
		assertEquals(originalVars.get(0).get("value_selector"), exportedVars.get(0).get("value_selector"));
	}

}
