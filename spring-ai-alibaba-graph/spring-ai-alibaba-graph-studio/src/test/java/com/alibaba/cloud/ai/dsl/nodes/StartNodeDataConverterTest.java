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

import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.alibaba.cloud.ai.service.dsl.nodes.StartNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.serialize.YamlSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StartNodeDataConverterTest {

	private static final Logger log = LoggerFactory.getLogger(StartNodeDataConverterTest.class);

	private final NodeDataConverter<StartNodeData> nodeDataConverter;

	private final Serializer serializer;

	public StartNodeDataConverterTest() {
		this.nodeDataConverter = new StartNodeDataConverter();
		this.serializer = new YamlSerializer();
	}

	/**
	 * dify test case: desc: "" selected: false title: Start type: start variables: -
	 * label: user_query max_length: 200 options: [] required: true type: paragraph
	 * variable: user_query - label: useLLM max_length: 48 options: - "yes" - "no"
	 * required: true type: select variable: useLLM
	 */
	@Test
	public void testParseDifyDSL() {
		String difyStartNodeString = "desc: \"\"\n" + "selected: false\n" + "title: Start\n" + "type: start\n"
				+ "variables:\n" + "  - label: user_query\n" + "    max_length: 200\n" + "    options: []\n"
				+ "    required: true\n" + "    type: paragraph\n" + "    variable: user_query\n"
				+ "  - label: useLLM\n" + "    max_length: 48\n" + "    options:\n" + "      - \"yes\"\n"
				+ "      - \"no\"\n" + "    required: true\n" + "    type: select\n" + "    variable: useLLM";
		Map<String, Object> difyStartNodeDataMap = serializer.load(difyStartNodeString);
		StartNodeData startNodeData = nodeDataConverter.parseMapData(difyStartNodeDataMap, DSLDialectType.DIFY);
		assertNotNull(startNodeData);
		assertNotNull(startNodeData.getStartInputs());
		List<StartNodeData.StartInput> startInputs = startNodeData.getStartInputs();
		assertEquals(2, startInputs.size());
		assertEquals("user_query", startInputs.get(0).getVariable());
		assertEquals("useLLM", startInputs.get(1).getVariable());
		assertEquals("paragraph", startInputs.get(0).getType());
		assertEquals("select", startInputs.get(1).getType());
		log.info("startNodeData dify dsl parse: " + startNodeData);
	}

	@Test
	public void testDumpDifyDSL() {
		StartNodeData startNodeData = new StartNodeData().setStartInputs(List.of(
				new StartNodeData.StartInput().setVariable("user_query")
					.setLabel("user_query")
					.setType("paragraph")
					.setMaxLength(200)
					.setRequired(true),
				new StartNodeData.StartInput().setVariable("useLLM")
					.setLabel("useLLM")
					.setType("select")
					.setOptions(List.of("yes", "no"))
					.setRequired(true)
					.setMaxLength(48)));
		Map<String, Object> difyStartNodeDataMap = nodeDataConverter.dumpMapData(startNodeData, DSLDialectType.DIFY);
		assertNotNull(difyStartNodeDataMap);
		List<Map<String, Object>> variables = (List<Map<String, Object>>) difyStartNodeDataMap.get("variables");
		assertEquals(2, variables.size());
		assertEquals("user_query", variables.get(0).get("variable"));
		assertEquals("useLLM", variables.get(1).get("variable"));
		assertEquals("paragraph", variables.get(0).get("type"));
		assertEquals("select", variables.get(1).get("type"));
		List<String> options = (List<String>) variables.get(1).get("options");
		assertEquals(2, options.size());
		assertEquals("yes", options.get(0));
		assertEquals("no", options.get(1));
		log.info("startNodeData dify dsl dump: " + serializer.dump(difyStartNodeDataMap));
	}

	/**
	 * custom dsl test case: inputs: [] outputs: - name: user_query value: null valueType:
	 * Object description: null extraProperties: null - name: useLLM value: null
	 * valueType: Object description: null extraProperties: null startInputs: - label:
	 * user_query type: paragraph variable: user_query maxLength: 200 options: []
	 * required: true - label: useLLM type: select variable: useLLM maxLength: 48 options:
	 * - "yes" - "no" required: true
	 */
	@Test
	public void testParseCustomDSL() {
		String customStartNodeString = "inputs: []\n" + "outputs:\n" + "  - name: user_query\n" + "    value: null\n"
				+ "    valueType: Object\n" + "    description: null\n" + "    extraProperties: null\n"
				+ "  - name: useLLM\n" + "    value: null\n" + "    valueType: Object\n" + "    description: null\n"
				+ "    extraProperties: null\n" + "startInputs:\n" + "  - label: user_query\n" + "    type: paragraph\n"
				+ "    variable: user_query\n" + "    maxLength: 200\n" + "    options: []\n" + "    required: true\n"
				+ "  - label: useLLM\n" + "    type: select\n" + "    variable: useLLM\n" + "    maxLength: 48\n"
				+ "    options:\n" + "      - \"yes\"\n" + "      - \"no\"\n" + "    required: true";
		Map<String, Object> customStartNodeDataMap = serializer.load(customStartNodeString);
		StartNodeData startNodeData = nodeDataConverter.parseMapData(customStartNodeDataMap, DSLDialectType.CUSTOM);
		assertNotNull(startNodeData);
		assertNotNull(startNodeData.getStartInputs());
		List<StartNodeData.StartInput> startInputs = startNodeData.getStartInputs();
		assertEquals(2, startInputs.size());
		assertEquals("user_query", startInputs.get(0).getVariable());
		assertEquals("useLLM", startInputs.get(1).getVariable());
		assertEquals("paragraph", startInputs.get(0).getType());
		assertEquals("select", startInputs.get(1).getType());
		assertEquals("yes", startInputs.get(1).getOptions().get(0));
		assertEquals("no", startInputs.get(1).getOptions().get(1));
		log.info("startNodeData custom dsl parse: " + startNodeData);
	}

	@Test
	public void testDumpCustomDSL() {
		StartNodeData startNodeData = new StartNodeData().setStartInputs(List.of(
				new StartNodeData.StartInput().setVariable("user_query")
					.setLabel("user_query")
					.setType("paragraph")
					.setMaxLength(200)
					.setRequired(true),
				new StartNodeData.StartInput().setVariable("useLLM")
					.setLabel("useLLM")
					.setType("select")
					.setOptions(List.of("yes", "no"))
					.setRequired(true)
					.setMaxLength(48)));
		Map<String, Object> customStartNodeDataMap = nodeDataConverter.dumpMapData(startNodeData,
				DSLDialectType.CUSTOM);
		assertNotNull(customStartNodeDataMap);
		List<Map<String, Object>> startInputMaps = (List<Map<String, Object>>) customStartNodeDataMap
			.get("startInputs");
		assertNotNull(startInputMaps);
		assertEquals(2, startInputMaps.size());
		assertEquals("user_query", startInputMaps.get(0).get("variable"));
		assertEquals("useLLM", startInputMaps.get(1).get("variable"));
		assertEquals("paragraph", startInputMaps.get(0).get("type"));
		assertEquals("select", startInputMaps.get(1).get("type"));
		List<String> options = (List<String>) startInputMaps.get(1).get("options");
		assertEquals(2, options.size());
		assertEquals("yes", options.get(0));
		assertEquals("no", options.get(1));
		log.info("startNodeData custom dsl dump: " + serializer.dump(customStartNodeDataMap));
	}

}
