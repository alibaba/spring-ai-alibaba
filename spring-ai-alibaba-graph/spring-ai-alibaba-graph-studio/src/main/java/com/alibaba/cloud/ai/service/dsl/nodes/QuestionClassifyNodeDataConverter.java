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
package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.HttpNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.utils.StringTemplateUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author HeYQ
 * @since 2024-12-12 23:54
 */
@Component
public class QuestionClassifyNodeDataConverter extends AbstractNodeDataConverter<QuestionClassifierNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<QuestionClassifierNodeData>> getDialectConverters() {
		return Stream.of(QuestionClassifyNodeDataConverter.QuestionClassifyNodeDialectConverter.values())
			.map(QuestionClassifyNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum QuestionClassifyNodeDialectConverter {

		DIFY(new DialectConverter<>() {

			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public QuestionClassifierNodeData parse(Map<String, Object> data) {
				List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("query_variable_selector"))
					.filter(CollectionUtils::isNotEmpty)
					.map(variables -> Collections
						.singletonList(new VariableSelector(variables.get(0), variables.get(1))))
					.orElse(Collections.emptyList());

				// convert model config
				Map<String, Object> modelData = (Map<String, Object>) data.get("model");
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
				QuestionClassifierNodeData.ModelConfig modelConfig = new QuestionClassifierNodeData.ModelConfig()
					.setMode((String) modelData.get("mode"))
					.setName((String) modelData.get("name"))
					.setProvider((String) modelData.get("provider"))
					.setCompletionParams(objectMapper.convertValue(modelData.get("completion_params"),
							QuestionClassifierNodeData.CompletionParams.class));

				QuestionClassifierNodeData nodeData = new QuestionClassifierNodeData(inputs,
						List.of(QuestionClassifierNodeData.DEFAULT_OUTPUT_SCHEMA))
					.setModel(modelConfig);

				// convert instructions
				String instruction = (String) data.get("instructions");
				if (instruction != null && !instruction.isBlank()) {
					nodeData.setInstruction(instruction);
				}

				// convert classes
				if (data.containsKey("classes")) {
					List<Map<String, Object>> classes = (List<Map<String, Object>>) data.get("classes");
					nodeData.setClasses(classes.stream()
						.map(item -> new QuestionClassifierNodeData.ClassConfig().setId((String) item.get("id"))
							.setText((String) item.get("name")))
						.toList());
				}

				// convert memory config
				if (data.containsKey("memory")) {
					Map<String, Object> memoryData = (Map<String, Object>) data.get("memory");
					String lastMessageTemplate = (String) memoryData.get("query_prompt_template");
					Map<String, Object> window = (Map<String, Object>) memoryData.get("window");
					Boolean windowEnabled = (Boolean) window.get("enabled");
					Integer windowSize = (Integer) window.get("size");
					QuestionClassifierNodeData.MemoryConfig memory = new QuestionClassifierNodeData.MemoryConfig()
						.setWindowEnabled(windowEnabled)
						.setWindowSize(windowSize)
						.setLastMessageTemplate(lastMessageTemplate)
						.setIncludeLastMessage(false);
					nodeData.setMemoryConfig(memory);
				}

				// output_key
				String nodeId = (String) data.get("id");
				String outputKey = (String) data.getOrDefault("output_key", HttpNodeData.defaultOutputKey(nodeId));
				nodeData.setOutputKey(outputKey);

				// input_text_key

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(QuestionClassifierNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
				objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

				// put memory
				QuestionClassifierNodeData.MemoryConfig memory = nodeData.getMemoryConfig();
				if (memory != null) {
					data.put("memory",
							Map.of("query_prompt_template",
									StringTemplateUtil.toDifyTmpl(memory.getLastMessageTemplate()), "role_prefix",
									Map.of("assistant", "", "user", ""), "window",
									Map.of("enabled", memory.getWindowEnabled(), "size", memory.getWindowSize())));
				}

				// put model
				QuestionClassifierNodeData.ModelConfig model = nodeData.getModel();
				data.put("model",
						Map.of("mode", model.getMode(), "name", model.getName(), "provider", model.getProvider(),
								"completion_params",
								objectMapper.convertValue(model.getCompletionParams(), Map.class)));

				// put query_variable_selector
				List<VariableSelector> inputs = nodeData.getInputs();
				Optional.ofNullable(inputs)
					.filter(CollectionUtils::isNotEmpty)
					.map(inputList -> inputList.stream()
						.findFirst()
						.map(input -> List.of(input.getNamespace(), input.getName()))
						.orElse(Collections.emptyList()))
					.ifPresent(variables -> data.put("query_variable_selector", variables));

				// put instructions
				data.put("instructions", nodeData.getInstruction() != null ? nodeData.getInstruction() : "");

				// put Classes
				if (!CollectionUtils.isEmpty(nodeData.getClasses())) {
					data.put("classes",
							nodeData.getClasses()
								.stream()
								.map(item -> Map.of("id", item.getId(), "text", item.getText()))
								.toList());
				}

				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(QuestionClassifierNodeData.class));

		private final DialectConverter<QuestionClassifierNodeData> dialectConverter;

		public DialectConverter<QuestionClassifierNodeData> dialectConverter() {
			return dialectConverter;
		}

		QuestionClassifyNodeDialectConverter(DialectConverter<QuestionClassifierNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
