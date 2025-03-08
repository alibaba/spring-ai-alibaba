/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.alibaba.cloud.ai.service.dsl.nodes.EndNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.serialize.YamlSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class EndNodeDataConverterTest {

	private final NodeDataConverter<EndNodeData> nodeDataConverter;

	private final Serializer serializer;

	public EndNodeDataConverterTest() {
		this.nodeDataConverter = new EndNodeDataConverter();
		this.serializer = new YamlSerializer();
	}

	/**
	 * dify dsl test case: desc: "" outputs: - value_selector: - "1733474977788" - useLLM
	 * variable: useLLM selected: false title: End 2 type: end
	 */
	@Test
	public void testParseDifyDSL() {
		String difyAnswerNodeString = "desc: ''\n" + "outputs:\n" + "- value_selector:\n" + "  - '1733474977788'\n"
				+ "  - useLLM\n" + "  variable: useLLM\n" + "selected: false\n" + "title: End 2\n" + "type: end";
		Map<String, Object> difyAnswerNodeMap = serializer.load(difyAnswerNodeString);
		EndNodeData endNodeData = nodeDataConverter.parseMapData(difyAnswerNodeMap, DSLDialectType.DIFY);
		assertNotNull(endNodeData);
		assertEquals(endNodeData.getInputs().get(0).getNamespace(), "1733474977788");
		assertEquals(endNodeData.getInputs().get(0).getName(), "useLLM");
		log.info("endNodeData dify dsl parse: " + endNodeData);

	}

	@Test
	public void testDumpDifyDSL() {
		EndNodeData endNodeData = new EndNodeData(List.of(new VariableSelector("1733474977788", "useLLM", "useLLM")),
				List.of(new Variable("useLLM", VariableType.STRING.value())));
		Map<String, Object> difyAnswerNodeMap = nodeDataConverter.dumpMapData(endNodeData, DSLDialectType.DIFY);
		assertNotNull(difyAnswerNodeMap);
		List<Map<String, Object>> outputMaps = (List<Map<String, Object>>) difyAnswerNodeMap.get("outputs");
		assertNotNull(outputMaps);
		assertEquals(outputMaps.size(), 1);
		assertEquals(outputMaps.get(0).get("variable"), "useLLM");
		List<String> value_selector = (List<String>) outputMaps.get(0).get("value_selector");
		assertNotNull(value_selector);
		assertEquals(value_selector.size(), 2);
		assertEquals(value_selector.get(0), "1733474977788");
		assertEquals(value_selector.get(1), "useLLM");
		String endNodeDataDifyDSL = serializer.dump(difyAnswerNodeMap);
		log.info("endNodeData dify dsl dump: " + endNodeDataDifyDSL);
	}

	/**
	 * custom dsl test case: inputs: - namespace: "1733474977788" name: useLLM label:
	 * useLLM outputs: - name: output value: null valueType: String description: null
	 * extraProperties: null
	 */
	@Test
	public void testParseCustomDSL() {
		String customEndNodeString = "inputs:\n" + "- namespace: '1733474977788'\n" + "  name: useLLM\n"
				+ "  label: useLLM\n" + "outputs:\n" + "- name: output\n" + "  value: null\n" + "  valueType: String\n"
				+ "  description: null\n" + "  extraProperties: null";
		Map<String, Object> customAnswerNodeMap = serializer.load(customEndNodeString);
		EndNodeData endNodeData = nodeDataConverter.parseMapData(customAnswerNodeMap, DSLDialectType.CUSTOM);
		assertNotNull(endNodeData);
		assertEquals("output", endNodeData.getOutputs().get(0).getName());
		assertEquals("output", endNodeData.getOutputs().get(0).getName());
		log.info("endNodeData custom dsl parse: " + endNodeData);
	}

	/**
	 * test case: inputs: - namespace: "1733474977788" name: useLLM label: useLLM outputs:
	 * - name: output value: null valueType: String description: null extraProperties:
	 * null
	 */
	@Test
	public void testDumpCustomDSL() {
		EndNodeData endNodeData = new EndNodeData(List.of(new VariableSelector("1733474977788", "useLLM", "useLLM")),
				List.of(new Variable("output", VariableType.STRING.value())));
		Map<String, Object> customAnswerNodeMap = nodeDataConverter.dumpMapData(endNodeData, DSLDialectType.CUSTOM);
		assertNotNull(customAnswerNodeMap);
		List<Map<String, Object>> inputsMap = (List<Map<String, Object>>) customAnswerNodeMap.get("inputs");
		assertNotNull(inputsMap);
		assertEquals(inputsMap.size(), 1);
		assertEquals(inputsMap.get(0).get("namespace"), "1733474977788");
		assertEquals(inputsMap.get(0).get("label"), "useLLM");
		log.info("endNodeData custom dsl dump: " + serializer.dump(customAnswerNodeMap));

	}

}
