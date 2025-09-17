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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

@Component
public class CodeNodeDataConverter extends AbstractNodeDataConverter<CodeNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.CODE.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<CodeNodeData>> getDialectConverters() {
		return Stream.of(CodeNodeDialectConverter.values()).map(CodeNodeDialectConverter::dialectConverter).toList();
	}

	private enum CodeNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public CodeNodeData parse(Map<String, Object> data) {
				CodeNodeData nodeData = new CodeNodeData();

				// 提取必要信息
				String code = MapReadUtil.getMapDeepValue(data, String.class, "code");
				String lang = MapReadUtil.getMapDeepValue(data, String.class, "code_language");
				Boolean isRetry = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Boolean.class, "retry_config", "retry_enabled"))
					.orElse(false);
				int maxRetryCount = isRetry ? Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "retry_config", "max_retries"))
					.orElse(1) : 1;
				int retryIntervalMs = isRetry ? Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "retry_config", "retry_interval"))
					.orElse(1000) : 1000;

				List<Variable> outputParams = Optional
					.ofNullable(
							MapReadUtil.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "outputs")))
					.orElse(List.of())
					.stream()
					.map(Map::entrySet)
					.flatMap(Collection::stream)
					.map(Map.Entry::getKey)
					.map(k -> new Variable(k, VariableType.OBJECT))
					.toList();

				List<CodeNodeData.CodeParam> inputParams = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "variables")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("value_selector") && map.containsKey("variable"))
					.map(map -> {
						List<String> list = MapReadUtil.safeCastToList(map.get("value_selector"), String.class);
						// 先以Value的形式存储selector，在post阶段转换为正确的stateKey
						return new CodeNodeData.CodeParam(map.get("variable").toString(), list, list.get(0));
					})
					.toList();

				// 设置必要信息
				nodeData.setCodeStyle(CodeNodeData.CodeStyle.EXPLICIT_PARAMETERS);
				nodeData.setCode(code);
				nodeData.setCodeLanguage(lang);
				nodeData.setMaxRetryCount(maxRetryCount);
				nodeData.setRetryIntervalMs(retryIntervalMs);
				nodeData.setInputParams(inputParams);
				nodeData.setOutputs(outputParams);

				// 错误处理策略
				String errorStrategy = MapReadUtil.getMapDeepValue(data, String.class, "error_strategy");

				if (errorStrategy != null) {
					// 暂仅支持默认值
					List<Map<String, Object>> defaultValueList = MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "default_value"));
					if (defaultValueList != null) {
						Map<String, Object> defaultValue = defaultValueList.stream()
							.filter(map -> map.containsKey("key") && map.containsKey("value"))
							.collect(Collectors.toUnmodifiableMap(map -> map.get("key").toString(),
									map -> map.get("value"), (a, b) -> b));
						nodeData.setDefaultValue(defaultValue);
					}
				}

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(CodeNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}),

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public CodeNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				CodeNodeData nodeData = new CodeNodeData();

				// 获取基本信息
				String code = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "script_content");
				String lang = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "script_type");
				Boolean isRetry = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Boolean.class, "config", "node_param", "retry_config",
							"retry_enabled"))
					.orElse(false);
				int maxRetryCount = isRetry ? Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "config", "node_param", "retry_config",
							"max_retries"))
					.orElse(1) : 1;
				int retryIntervalMs = isRetry ? Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "config", "node_param", "retry_config",
							"retry_interval"))
					.orElse(1000) : 1000;

				List<Variable> outputParams = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "output_params")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("key"))
					.map(map -> new Variable(map.get("key").toString(), VariableType.OBJECT))
					.toList();
				List<CodeNodeData.CodeParam> inputParams = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "input_params")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("key") && map.containsKey("value") && map.containsKey("value_from"))
					.map(map -> {
						String key = map.get("key").toString();
						Object value = map.get("value");
						String valueFrom = map.get("value_from").toString();
						if ("input".equalsIgnoreCase(valueFrom)) {
							return CodeNodeData.CodeParam.withValue(key, value);
						}
						else {
							// 先以Value的形式存储selector，在post阶段转换为正确的stateKey
							VariableSelector selector = this.varTemplateToSelector(DSLDialectType.STUDIO,
									value.toString());
							List<String> list = List.of(selector.getNamespace(), selector.getName());
							return new CodeNodeData.CodeParam(key, list, value.toString());
						}
					})
					.toList();

				// 设置基本信息
				nodeData.setCodeStyle(CodeNodeData.CodeStyle.GLOBAL_DICTIONARY);
				nodeData.setCode(code);
				nodeData.setCodeLanguage(lang);
				nodeData.setMaxRetryCount(maxRetryCount);
				nodeData.setRetryIntervalMs(retryIntervalMs);
				nodeData.setInputParams(inputParams);
				nodeData.setOutputs(outputParams);

				// 设置错误策略
				String errorStrategy = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"try_catch_config", "strategy");
				if (errorStrategy != null) {
					// 暂仅支持默认值
					List<Map<String, Object>> defaultValueList = MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param",
								"try_catch_config", "default_values"));
					if (defaultValueList != null) {
						Map<String, Object> defaultValue = defaultValueList.stream()
							.filter(map -> map.containsKey("key") && map.containsKey("value"))
							.collect(Collectors.toUnmodifiableMap(map -> map.get("key").toString(),
									map -> map.get("value"), (a, b) -> b));
						nodeData.setDefaultValue(defaultValue);
					}
				}

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(CodeNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(CodeNodeData.class));

		private final DialectConverter<CodeNodeData> dialectConverter;

		public DialectConverter<CodeNodeData> dialectConverter() {
			return dialectConverter;
		}

		CodeNodeDialectConverter(DialectConverter<CodeNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "codeNode" + count;
	}

	@Override
	public BiConsumer<CodeNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> this.emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				// code节点将返回{"varName.output": {...}}的数据，之后拆包成若干输出数据
				nodeData.setOutputKey(nodeData.getVarName() + "_" + CodeNodeData.getDefaultOutputSchema().getName());
				// 输入Param的Key都格式化为varName_key
				nodeData.setInputParams(nodeData.getInputParams().stream().map(param -> {
					if (param.stateKey() == null) {
						return param;
					}
					@SuppressWarnings("unchecked")
					List<String> selector = (List<String>) param.value();
					return CodeNodeData.CodeParam.withKey(param.argName(),
							idToVarName.getOrDefault(selector.get(0), selector.get(0)) + "_" + selector.get(1));
				}).toList());
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
