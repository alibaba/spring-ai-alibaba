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
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.utils.StringTemplateUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class LLMNodeDataConverter extends AbstractNodeDataConverter<LLMNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<LLMNodeData>> getDialectConverters() {
		return Stream.of(LLMNodeDialectConverter.values()).map(LLMNodeDialectConverter::dialectConverter).toList();
	}

	private enum LLMNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public LLMNodeData parse(Map<String, Object> data) {
				List<VariableSelector> inputs = new ArrayList<>();
				// convert prompt template
				Map<String, Object> context = (Map<String, Object>) data.get("context");
				List<Map<String, Object>> difyTmplList;
				if (data.get("prompt_template") instanceof List<?>) {
					difyTmplList = (List<Map<String, Object>>) data.get("prompt_template");
				}
				else {
					difyTmplList = List.of((Map<String, Object>) data.get("prompt_template"));
				}
				List<LLMNodeData.PromptTemplate> tmplList = new ArrayList<>();
				if ((Boolean) context.get("enabled")) {
					List<String> variableSelector = (List<String>) context.get("variable_selector");
					String systemText = (String) difyTmplList.get(0).get("text");
					String replacement = systemText.replace("{{#context#}}",
							variableSelector.get(0) + "." + variableSelector.get(1));
					difyTmplList.get(0).put("text", replacement);
				}
				for (Map<String, Object> promptTmpl : difyTmplList) {
					List<String> variables = new ArrayList<>();
					String tmpl = StringTemplateUtil.fromDifyTmpl((String) promptTmpl.get("text"), variables);
					variables.forEach(variable -> {
						String[] splits = variable.split("\\.", 2);
						inputs.add(new VariableSelector(splits[0], splits[1]));
					});
					String role = promptTmpl.containsKey("role") ? (String) promptTmpl.get("role") : "system";
					tmplList.add(new LLMNodeData.PromptTemplate(role, tmpl));
				}
				// convert model config
				Map<String, Object> modelData = (Map<String, Object>) data.get("model");
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
				LLMNodeData.ModelConfig modelConfig = new LLMNodeData.ModelConfig()
					.setMode((String) modelData.get("mode"))
					.setName((String) modelData.get("name"))
					.setProvider((String) modelData.get("provider"))
					.setCompletionParams(objectMapper.convertValue(modelData.get("completion_params"),
							LLMNodeData.CompletionParams.class));

				LLMNodeData.MemoryConfig memoryConfig = new LLMNodeData.MemoryConfig();
				// convert memory config
				if (data.containsKey("memory")) {
					List<String> variables = new ArrayList<>();
					Map<String, Object> memoryData = (Map<String, Object>) data.get("memory");
					String lastMessageTemplate = (String) memoryData.get("query_prompt_template");
					lastMessageTemplate = StringTemplateUtil.fromDifyTmpl(lastMessageTemplate, variables);
					variables.forEach(variable -> {
						String[] splits = variable.split("\\.", 2);
						inputs.add(new VariableSelector(splits[0], splits[1]));
					});
					Map<String, Object> window = (Map<String, Object>) memoryData.get("window");
					Boolean windowEnabled = (Boolean) window.get("enabled");
					Integer windowSize = (Integer) window.get("size");
					memoryConfig.setEnabled(true)
						.setWindowEnabled(windowEnabled)
						.setWindowSize(windowSize)
						.setLastMessageTemplate(lastMessageTemplate)
						.setIncludeLastMessage(false);
				}

				return new LLMNodeData(inputs, List.of(LLMNodeData.DEFAULT_OUTPUT_SCHEMA)).setModel(modelConfig)
					.setPromptTemplate(tmplList)
					.setMemoryConfig(memoryConfig);
			}

			@Override
			public Map<String, Object> dump(LLMNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
				objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
				// put context
				data.put("context", Map.of("enabled", false, "variable_selector", new ArrayList<>()

		));
				// put memory
				LLMNodeData.MemoryConfig memory = nodeData.getMemoryConfig();
				if (memory != null) {
					data.put("memory",
							Map.of("query_prompt_template",
									StringTemplateUtil.toDifyTmpl(memory.getLastMessageTemplate()), "role_prefix",
									Map.of("assistant", "", "user", ""), "window",
									Map.of("enabled", memory.getWindowEnabled(), "size", memory.getWindowSize())));
				}
				// put model
				LLMNodeData.ModelConfig model = nodeData.getModel();
				data.put("model",
						Map.of("mode", model.getMode(), "name", model.getName(), "provider", model.getProvider(),
								"completion_params",
								objectMapper.convertValue(model.getCompletionParams(), Map.class)));
				// put prompt template
				List<LLMNodeData.PromptTemplate> tmplList = nodeData.getPromptTemplate();
				List<Map<String, String>> difyTmplList = tmplList.stream().map(tmpl -> {
					String difyTmpl = StringTemplateUtil.toDifyTmpl(tmpl.getText());
					return Map.of("role", tmpl.getRole(), "text", difyTmpl);
				}).toList();
				data.put("prompt_template", difyTmplList);
				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(LLMNodeData.class));

		private final DialectConverter<LLMNodeData> dialectConverter;

		public DialectConverter<LLMNodeData> dialectConverter() {
			return dialectConverter;
		}

		LLMNodeDialectConverter(DialectConverter<LLMNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
