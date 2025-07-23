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
package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateTransformNodeDataTest {

	@Test
	public void testDefaultConstructor() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();

		assertNotNull(nodeData);
		assertNull(nodeData.getTemplate());
		assertNull(nodeData.getOutputKey());
		assertNotNull(nodeData.getInputs());
		assertNotNull(nodeData.getOutputs());
		assertTrue(nodeData.getInputs().isEmpty());
		assertTrue(nodeData.getOutputs().isEmpty());
	}

	@Test
	public void testConstructorWithInputsAndOutputs() {
		List<VariableSelector> inputs = new ArrayList<>();
		inputs.add(new VariableSelector("sourceNode", "output", "inputVar"));

		List<Variable> outputs = new ArrayList<>();
		outputs.add(new Variable("result", VariableType.STRING.value()));

		TemplateTransformNodeData nodeData = new TemplateTransformNodeData(inputs, outputs);

		assertNotNull(nodeData);
		assertEquals(1, nodeData.getInputs().size());
		assertEquals(1, nodeData.getOutputs().size());
		assertEquals("sourceNode", nodeData.getInputs().get(0).getNamespace());
		assertEquals("output", nodeData.getInputs().get(0).getName());
		assertEquals("inputVar", nodeData.getInputs().get(0).getLabel());
		assertEquals("result", nodeData.getOutputs().get(0).getName());
		assertEquals(VariableType.STRING.value(), nodeData.getOutputs().get(0).getValueType());
	}

	@Test
	public void testSetTemplate() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		String template = "Hello {{name}}, welcome to {{place}}!";

		TemplateTransformNodeData result = nodeData.setTemplate(template);

		assertEquals(template, nodeData.getTemplate());
		assertSame(nodeData, result); // 检查链式调用
	}

	@Test
	public void testSetOutputKey() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();
		String outputKey = "templateOutput";

		TemplateTransformNodeData result = nodeData.setOutputKey(outputKey);

		assertEquals(outputKey, nodeData.getOutputKey());
		assertSame(nodeData, result); // 检查链式调用
	}

	@Test
	public void testCompleteNodeDataSetup() {
		// 测试完整的节点数据设置
		List<VariableSelector> inputs = new ArrayList<>();
		inputs.add(new VariableSelector("user", "name", "userName"));
		inputs.add(new VariableSelector("system", "location", "userLocation"));

		List<Variable> outputs = new ArrayList<>();
		outputs.add(new Variable("transformedText", VariableType.STRING.value()));

		TemplateTransformNodeData nodeData = new TemplateTransformNodeData(inputs, outputs)
			.setTemplate("Dear {{userName}}, you are currently in {{userLocation}}.")
			.setOutputKey("greeting_output");

		// 验证所有属性
		assertEquals("Dear {{userName}}, you are currently in {{userLocation}}.", nodeData.getTemplate());
		assertEquals("greeting_output", nodeData.getOutputKey());
		assertEquals(2, nodeData.getInputs().size());
		assertEquals(1, nodeData.getOutputs().size());

		// 验证inputs
		assertEquals("user", nodeData.getInputs().get(0).getNamespace());
		assertEquals("name", nodeData.getInputs().get(0).getName());
		assertEquals("userName", nodeData.getInputs().get(0).getLabel());

		assertEquals("system", nodeData.getInputs().get(1).getNamespace());
		assertEquals("location", nodeData.getInputs().get(1).getName());
		assertEquals("userLocation", nodeData.getInputs().get(1).getLabel());

		// 验证outputs
		assertEquals("transformedText", nodeData.getOutputs().get(0).getName());
		assertEquals(VariableType.STRING.value(), nodeData.getOutputs().get(0).getValueType());
	}

	@Test
	public void testSetTemplateNull() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();

		nodeData.setTemplate(null);

		assertNull(nodeData.getTemplate());
	}

	@Test
	public void testSetOutputKeyNull() {
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData();

		nodeData.setOutputKey(null);

		assertNull(nodeData.getOutputKey());
	}

	@Test
	public void testChainedMethodCalls() {
		// 测试链式方法调用
		TemplateTransformNodeData nodeData = new TemplateTransformNodeData().setTemplate("Hello {{world}}")
			.setOutputKey("chain_output");

		assertEquals("Hello {{world}}", nodeData.getTemplate());
		assertEquals("chain_output", nodeData.getOutputKey());
	}

}
