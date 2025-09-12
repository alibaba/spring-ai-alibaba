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
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.IterationNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.stereotype.Component;

/**
 * @author vlsmb
 * @since 2025/7/22
 */
@Component
public class IterationNodeDataConverter extends AbstractNodeDataConverter<IterationNodeData> {

	private enum IterationNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public IterationNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				IterationNodeData nodeData = new IterationNodeData();
				int parallelCount = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "parallel_nums"))
					.orElse(1);
				nodeData.setParallelCount(parallelCount);

				List<String> inputSelectorList = Optional
					.ofNullable(MapReadUtil.safeCastToList(
							MapReadUtil.getMapDeepValue(data, List.class, "iterator_selector"), String.class))
					.orElse(List.of("unknown", "unknown"));
				nodeData.setInputSelector(new VariableSelector(inputSelectorList.get(0), inputSelectorList.get(1)));

				List<String> outputSelectorList = Optional
					.ofNullable(MapReadUtil
						.safeCastToList(MapReadUtil.getMapDeepValue(data, List.class, "output_selector"), String.class))
					.orElse(List.of("unknown", "unknown"));
				nodeData.setResultSelector(new VariableSelector(outputSelectorList.get(0), outputSelectorList.get(1)));

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(IterationNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public IterationNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				IterationNodeData nodeData = new IterationNodeData();

				// 获取必要信息
				int parallelCount = Optional
					.ofNullable(
							MapReadUtil.getMapDeepValue(data, Integer.class, "config", "node_param", "concurrent_size"))
					.orElse(1);
				int maxIterationCount = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, Integer.class, "config", "node_param", "batch_size"))
					.orElse(1);
				int indexOffset = 1;

				List<Map<String, Object>> inputParamsList = Optional
					.ofNullable(MapReadUtil
						.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "input_params")))
					.orElse(List.of());
				List<Map<String, Object>> outputParamsList = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "output_params")))
					.orElse(List.of());
				String itemKey = Optional.ofNullable(inputParamsList.get(0).get("key").toString()).orElse("item");
				String outputKey = Optional.ofNullable(outputParamsList.get(0).get("key").toString()).orElse("output");
				VariableSelector inputSelector = this.varTemplateToSelector(DSLDialectType.STUDIO,
						inputParamsList.get(0).get("value").toString());
				VariableSelector resultSelector = this.varTemplateToSelector(DSLDialectType.STUDIO,
						outputParamsList.get(0).get("value").toString());

				// 设置必要信息
				nodeData.setParallelCount(parallelCount);
				nodeData.setMaxIterationCount(maxIterationCount);
				nodeData.setIndexOffset(indexOffset);
				nodeData.setItemKey(itemKey);
				nodeData.setOutputKey(outputKey);
				nodeData.setInputSelector(inputSelector);
				nodeData.setResultSelector(resultSelector);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(IterationNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(IterationNodeData.class));

		private final DialectConverter<IterationNodeData> dialectConverter;

		public DialectConverter<IterationNodeData> dialectConverter() {
			return dialectConverter;
		}

		IterationNodeDialectConverter(DialectConverter<IterationNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	protected List<DialectConverter<IterationNodeData>> getDialectConverters() {
		return Stream.of(IterationNodeDialectConverter.values())
			.map(IterationNodeDialectConverter::dialectConverter)
			.toList();
	}

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return nodeType.equals(NodeType.ITERATION);
	}

	@Override
	public String generateVarName(int count) {
		return "iteration" + count;
	}

	@Override
	public BiConsumer<IterationNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				nodeData.setInputs(List.of(nodeData.getInputSelector(), nodeData.getResultSelector()));

				nodeData.setOutputs(Stream
					.of(IterationNodeData.getDefaultOutputSchemas(),
							List.of(new Variable(nodeData.getItemKey(), VariableType.OBJECT),
									new Variable(nodeData.getOutputKey(), VariableType.ARRAY_OBJECT)))
					.flatMap(List::stream)
					.toList());
			}).andThen(super.postProcessConsumer(dialectType)).andThen((nodeData, idToVarName) -> {
				nodeData.setInputSelector(nodeData.getInputs().get(0));
				nodeData.setResultSelector(nodeData.getInputs().get(1));
				nodeData.setInputs(null);
				nodeData.setItemKey(nodeData.getVarName() + "_" + nodeData.getItemKey());
				nodeData.setOutputKey(nodeData.getVarName() + "_" + nodeData.getOutputKey());
			});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
