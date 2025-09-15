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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.MiddleOutputNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Component
public class MiddleOutputNodeDataConverter extends AbstractNodeDataConverter<MiddleOutputNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.MIDDLE_OUTPUT.equals(nodeType);
	}

	@Override
	public String generateVarName(int count) {
		return "middleOutput" + count;
	}

	@Override
	public BiConsumer<MiddleOutputNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				// 设置输出键
				nodeData.setOutputs(MiddleOutputNodeData.getDefaultOutputSchemas(dialectType));
				nodeData.setOutputKey(nodeData.getVarName() + "_" + nodeData.getOutputs().get(0).getName());
				// 将输出模板进行处理
				nodeData
					.setOutputTemplate(this.convertVarTemplate(dialectType, nodeData.getOutputTemplate(), idToVarName));
				nodeData.setVarKeys(this.getVarTemplateKeys(nodeData.getOutputTemplate()));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

	private enum MiddleOutputNodeConverter {

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public MiddleOutputNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				MiddleOutputNodeData nodeData = new MiddleOutputNodeData();
				String outputTemplate = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"output");
				nodeData.setOutputTemplate(Optional.ofNullable(outputTemplate).orElse(""));
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(MiddleOutputNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}), CUSTOM(defaultCustomDialectConverter(MiddleOutputNodeData.class));

		private final DialectConverter<MiddleOutputNodeData> converter;

		MiddleOutputNodeConverter(DialectConverter<MiddleOutputNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<MiddleOutputNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	protected List<DialectConverter<MiddleOutputNodeData>> getDialectConverters() {
		return Stream.of(MiddleOutputNodeConverter.values()).map(MiddleOutputNodeConverter::dialectConverter).toList();
	}

}
