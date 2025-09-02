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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.StringTemplateUtil;

import org.springframework.stereotype.Component;

@Component
public class AnswerNodeDataConverter extends AbstractNodeDataConverter<AnswerNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.ANSWER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<AnswerNodeData>> getDialectConverters() {
		return Stream.of(AnswerNodeDialectConverter.values())
			.map(AnswerNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum AnswerNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public AnswerNodeData parse(Map<String, Object> data) {
				String difyTmpl = (String) data.get("answer");
				List<String> variables = new ArrayList<>();
				List<VariableSelector> inputs = variables.stream().map(variable -> {
					String[] splits = variable.split("\\.", 2);
					return new VariableSelector(splits[0], splits[1]);
				}).toList();
				String outputKey = (String) data.get("output_key");

				AnswerNodeData nd = new AnswerNodeData(inputs, AnswerNodeData.getDefaultOutputs()).setAnswer(difyTmpl);
				nd.setOutputKey(outputKey);
				return nd;
			}

			@Override
			public Map<String, Object> dump(AnswerNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				String difyTmpl = StringTemplateUtil.toDifyTmpl(nodeData.getAnswer());
				data.put("answer", difyTmpl);
				return data;
			}
		}),

		CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(AnswerNodeData.class));

		private final DialectConverter<AnswerNodeData> dialectConverter;

		public DialectConverter<AnswerNodeData> dialectConverter() {
			return dialectConverter;
		}

		AnswerNodeDialectConverter(DialectConverter<AnswerNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "answerNode" + count;
	}

	@Override
	public BiConsumer<AnswerNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				nodeData
					.setOutputKey(nodeData.getVarName() + "_" + AnswerNodeData.getDefaultOutputs().get(0).getName());
				nodeData.setAnswer(this.convertVarTemplate(dialectType, nodeData.getAnswer(), idToVarName));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
