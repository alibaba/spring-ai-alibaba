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
import java.util.function.BiConsumer;
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
			@Override
			public LLMNodeData parse(Map<String, Object> data) {
				LLMNodeData nodeData = new LLMNodeData();

				// 获取必要的信息
				String modeName = this.exactChatModelName(DSLDialectType.DIFY, data);
				Map<String, Object> modeParams = this.exactChatModelParam(DSLDialectType.DIFY, data);

				// MessageTemplate的keys字段将在postProcess中确定，所以这里先设置为空
				List<LLMNodeData.MessageTemplate> messageTemplates = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "prompt_template")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("role") && map.containsKey("text"))
					.map(map -> new LLMNodeData.MessageTemplate(map.get("text").toString(), List.of(),
							MessageType.fromValue(map.get("role").toString())))
					.toList();

				Boolean retryEnable = MapReadUtil.getMapDeepValue(data, Boolean.class, "retry_config", "retry_enabled");
				Integer maxRetryCount = Boolean.TRUE.equals(retryEnable)
						? MapReadUtil.getMapDeepValue(data, Integer.class, "retry_config", "max_retries") : 1;
				Integer retryIntervalMs = Boolean.TRUE.equals(retryEnable)
						? MapReadUtil.getMapDeepValue(data, Integer.class, "retry_config", "retry_interval") : 1000;

				String errorStrategy = MapReadUtil.getMapDeepValue(data, String.class, "error_strategy");
				List<Map<String, Object>> defaultValues = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "default_value")))
					.orElse(List.of());
				String defaultOutput = null;
				String errorNextNode = null;
				if (!defaultValues.isEmpty()) {
					defaultOutput = defaultValues.get(0).get("value").toString();
				}

				// 设置NodeData
				nodeData.setChatModeName(modeName);
				nodeData.setModeParams(modeParams);
				nodeData.setMessageTemplates(messageTemplates);
				nodeData.setMaxRetryCount(maxRetryCount);
				nodeData.setRetryIntervalMs(retryIntervalMs);
				nodeData.setDefaultOutput(defaultOutput);
				nodeData.setErrorNextNode(errorNextNode);
				return nodeData;
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

		, STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public LLMNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				LLMNodeData nodeData = new LLMNodeData();

				// 从data中提取必要信息
				String modeName = this.exactChatModelName(DSLDialectType.STUDIO, data);
				Map<String, Object> modeParams = this.exactChatModelParam(DSLDialectType.STUDIO, data);

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

	@Override
	public BiConsumer<LLMNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> this.emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				// 设置输出
				nodeData.setOutputs(LLMNodeData.getDefaultOutputSchemas(dialectType));
				nodeData.setOutputKeyPrefix(nodeData.getVarName().concat("_"));

				// 处理MessageTemplates
				List<LLMNodeData.MessageTemplate> messageTemplates = Optional.ofNullable(nodeData.getMessageTemplates())
					.orElse(List.of())
					.stream()
					.map(template -> {
						String newText = this.convertVarTemplate(dialectType, template.template(), idToVarName);
						List<String> keys = this.getVarTemplateKeys(newText);
						return new LLMNodeData.MessageTemplate(newText, keys, template.type());
					})
					.toList();
				nodeData.setMessageTemplates(messageTemplates);

				// 处理MemoryKey
				if (nodeData.getMemoryKey() != null) {
					String res = this.convertVarTemplate(dialectType, nodeData.getMemoryKey(), idToVarName);
					nodeData.setMemoryKey(res.substring(1, res.length() - 1));
				}

			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
