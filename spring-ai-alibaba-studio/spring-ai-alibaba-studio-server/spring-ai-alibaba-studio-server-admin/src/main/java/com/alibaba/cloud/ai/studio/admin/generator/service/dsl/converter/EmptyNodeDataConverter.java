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
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.EmptyNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.stereotype.Component;

/**
 * @author vlsmb
 * @since 2025/7/22
 */
@Component
public class EmptyNodeDataConverter extends AbstractNodeDataConverter<EmptyNodeData> {

	public enum EmptyNodeDialectConverter {

		ALL(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return true;
			}

			@Override
			public EmptyNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				return new EmptyNodeData();
			}

			@Override
			public Map<String, Object> dump(EmptyNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		});

		private final DialectConverter<EmptyNodeData> dialectConverter;

		public DialectConverter<EmptyNodeData> getDialectConverter() {
			return dialectConverter;
		}

		EmptyNodeDialectConverter(DialectConverter<EmptyNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	protected List<DialectConverter<EmptyNodeData>> getDialectConverters() {
		return Stream.of(EmptyNodeDialectConverter.values())
			.map(EmptyNodeDialectConverter::getDialectConverter)
			.toList();
	}

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.isEmpty(nodeType);
	}

	@Override
	public String generateVarName(int count) {
		return "emptyNode" + count;
	}

}
