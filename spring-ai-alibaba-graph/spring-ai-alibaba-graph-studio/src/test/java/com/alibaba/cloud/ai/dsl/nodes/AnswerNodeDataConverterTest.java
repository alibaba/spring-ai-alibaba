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
import com.alibaba.cloud.ai.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.alibaba.cloud.ai.service.dsl.nodes.AnswerNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.serialize.YamlSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AnswerNodeDataConverterTest {

	private static final Logger log = LoggerFactory.getLogger(AnswerNodeDataConverterTest.class);

	private final NodeDataConverter<AnswerNodeData> nodeDataConverter;

	private final Serializer serializer;

	public AnswerNodeDataConverterTest() {
		this.nodeDataConverter = new AnswerNodeDataConverter();
		this.serializer = new YamlSerializer();
	}

	/**
	 * dify dsl test case: answer: '答案是：{{#1733282983968.output#}}' desc: '' selected:
	 * false title: Answer type: answer variables: []
	 */
	@Test
	public void testParseDifyDSL() {
		String difyAnswerNodeString = "answer: '{{#1733282983968.output#}}'\n" + "desc: ''\n" + "selected: false\n"
				+ "title: Answer\n" + "type: answer\n" + "variables: []";
		Map<String, Object> difyAnswerNodeMap = serializer.load(difyAnswerNodeString);
		AnswerNodeData answerNodeData = nodeDataConverter.parseMapData(difyAnswerNodeMap, DSLDialectType.DIFY);
		assertNotNull(answerNodeData);
		assertEquals("{1733282983968.output}", answerNodeData.getAnswer());
		List<VariableSelector> inputs = answerNodeData.getInputs();
		assertEquals(1, inputs.size());
		assertEquals("1733282983968", inputs.get(0).getNamespace());
		assertEquals("output", inputs.get(0).getName());
		log.info("answer node dify dsl parse: " + answerNodeData);
	}

	@Test
	public void testDumpDifyDSL() {
		AnswerNodeData answerNodeData = new AnswerNodeData(List.of(new VariableSelector("1733282983968", "output")),
				List.of(new Variable("answer", VariableType.STRING.value())))
			.setAnswer("答案是：{1733282983968.output}");
		Map<String, Object> answerNodeDataMap = nodeDataConverter.dumpMapData(answerNodeData, DSLDialectType.DIFY);
		assertNotNull(answerNodeDataMap);
		assertEquals("答案是：{{#1733282983968.output#}}", answerNodeDataMap.get("answer"));
		String answerNodeDataString = serializer.dump(answerNodeDataMap);
		log.info("answer node dify dsl dump: " + answerNodeDataString);
	}

	/**
	 * custom dsl test case: inputs: - namespace: llm name: text label: null outputs: -
	 * name: answer value: null valueType: String description: null extraProperties: null
	 * answer: 您好，{llm.text}
	 */
	@Test
	public void testParseCustomDSL() {
		String customAnswerNodeString = "inputs:\n" + "- namespace: llm\n" + "  name: text\n" + "  label: null\n"
				+ "outputs:\n" + "- name: answer\n" + "  value: null\n" + "  valueType: String\n"
				+ "  description: null\n" + "  extraProperties: null\n" + "answer: 您好，{llm.text}";
		Map<String, Object> customAnswerNodeMap = serializer.load(customAnswerNodeString);
		AnswerNodeData answerNodeData = nodeDataConverter.parseMapData(customAnswerNodeMap, DSLDialectType.CUSTOM);
		assertNotNull(answerNodeData);
		assertEquals("您好，{llm.text}", answerNodeData.getAnswer());
		List<VariableSelector> inputs = answerNodeData.getInputs();
		assertEquals(1, inputs.size());
		assertEquals("llm", inputs.get(0).getNamespace());
		assertEquals("text", inputs.get(0).getName());
		log.info("answer node custom dsl parse: " + answerNodeData);
	}

	@Test
	public void testDumpCustomDSL() {
		AnswerNodeData answerNodeData = new AnswerNodeData(List.of(new VariableSelector("llm", "text")),
				List.of(new Variable("answer", VariableType.STRING.value())))
			.setAnswer("你好，{llm.text}");
		Map<String, Object> answerNodeDataMap = nodeDataConverter.dumpMapData(answerNodeData, DSLDialectType.CUSTOM);
		assertNotNull(answerNodeDataMap);
		assertEquals("你好，{llm.text}", answerNodeDataMap.get("answer"));
		assertNotNull(answerNodeDataMap.get("inputs"));
		List<Map<String, Object>> inputMaps = (List<Map<String, Object>>) answerNodeDataMap.get("inputs");
		assertEquals(1, inputMaps.size());
		assertEquals("llm", inputMaps.get(0).get("namespace"));
		assertEquals("text", inputMaps.get(0).get("name"));
		String answerNodeDataString = serializer.dump(answerNodeDataMap);
		log.info("answer node custom dsl dump: " + answerNodeDataString);
	}

}
