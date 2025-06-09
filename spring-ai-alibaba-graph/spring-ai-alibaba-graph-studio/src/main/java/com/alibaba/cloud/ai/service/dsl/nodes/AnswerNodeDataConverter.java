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

import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AnswerNodeDataConverter extends AbstractNodeDataConverter<AnswerNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.ANSWER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<AnswerNodeData>> getDialectConverters() {
		return Stream.of(Converter.DIFY, Converter.CUSTOM)
			.map(Converter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum Converter {

		DIFY(new DialectConverter<>() {
			@Override
			public AnswerNodeData parse(Map<String, Object> data) {
				AnswerNodeData nd = new AnswerNodeData();
				nd.setAnswer((String) data.get("answer"));
                String nodeId = (String) data.get("id");
                String outputKey = (String) data.getOrDefault("output_key", nodeId + "_output");
				return nd;
			}

			@Override
			public Map<String, Object> dump(AnswerNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();
				if (nd.getAnswer() != null) {
					m.put("answer", nd.getAnswer());
				}
				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}),

		CUSTOM(defaultCustomDialectConverter(AnswerNodeData.class));

		private final DialectConverter<AnswerNodeData> converter;

		Converter(DialectConverter<AnswerNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<AnswerNodeData> dialectConverter() {
			return converter;
		}

	}

}
