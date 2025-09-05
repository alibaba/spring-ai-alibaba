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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Component;

/**
 * Convert the LLM node configuration in the Dify DSL to and from the LLMNodeData object.
 */
@Component
public class LLMNodeDataConverter extends AbstractNodeDataConverter<LLMNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<LLMNodeData>> getDialectConverters() {
		return Stream.of(LLMNodeConverter.values())
			.map(LLMNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum LLMNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public LLMNodeData parse(Map<String, Object> data) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Map<String, Object> dump(LLMNodeData nd) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		})

		, STUDIO(new DialectConverter<LLMNodeData>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public LLMNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				LLMNodeData nodeData = new LLMNodeData();

				// 从data中提取必要信息
				Map<String, Object> modeConfigMap = MapReadUtil.safeCastToMapWithStringKey(
						MapReadUtil.getMapDeepValue(data, Map.class, "config", "node_param", "model_config"));
				String modeName = MapReadUtil.getMapDeepValue(modeConfigMap, String.class, "model_id");

				Map<String, Object> modeParams = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(modeConfigMap, List.class, "params")))
					.orElse(List.of())
					.stream()
					.filter(map -> Boolean.TRUE.equals(map.get("enable")))
					.filter(map -> map.containsKey("key") && map.containsKey("value"))
					.map(map -> Map.entry(map.get("key").toString(), map.get("value")))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));

				String systemPrompt = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"sys_prompt_content");
				String userPrompt = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"prompt_content");
				// MessageTemplate的keys字段将在postProcess中确定，所以这里先设置为空
				List<LLMNodeData.MessageTemplate> messageTemplates = List.of(
						new LLMNodeData.MessageTemplate(systemPrompt, List.of(), MessageType.SYSTEM),
						new LLMNodeData.MessageTemplate(userPrompt, List.of(), MessageType.USER));

				String memoryKey = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"short_memory", "param", "value");

				Boolean retryEnable = MapReadUtil.getMapDeepValue(data, Boolean.class, "config", "node_param",
						"retry_config", "retry_enabled");
				Integer maxRetryCount = Boolean.TRUE.equals(retryEnable) ? MapReadUtil.getMapDeepValue(data,
						Integer.class, "config", "node_param", "retry_config", "max_retries") : 1;
				Integer retryIntervalMs = Boolean.TRUE.equals(retryEnable) ? MapReadUtil.getMapDeepValue(data,
						Integer.class, "config", "node_param", "retry_config", "retry_interval") : 1000;

				String errorStrategy = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"try_catch_config", "strategy");
				String defaultOutput = null;
				String errorNextNode = null;
				List<Map<String, Object>> defaultOutputs = MapReadUtil.safeCastToListWithMap(MapReadUtil
					.getMapDeepValue(data, List.class, "config", "node_param", "try_catch_config", "default_values"));
				if (defaultOutputs != null && !defaultOutputs.isEmpty()) {
					defaultOutput = MapReadUtil.getMapDeepValue(defaultOutputs.get(0), String.class, "value");
				}

				// 设置nodeData
				nodeData.setChatModeName(modeName);
				nodeData.setModeParams(modeParams);
				nodeData.setMessageTemplates(messageTemplates);
				nodeData.setMemoryKey(memoryKey);
				nodeData.setMaxRetryCount(maxRetryCount);
				nodeData.setRetryIntervalMs(retryIntervalMs);
				nodeData.setDefaultOutput(defaultOutput);
				nodeData.setErrorNextNode(errorNextNode);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(LLMNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(defaultCustomDialectConverter(LLMNodeData.class));

		private final DialectConverter<LLMNodeData> converter;

		LLMNodeConverter(DialectConverter<LLMNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<LLMNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "LLMNode" + count;
	}

}
