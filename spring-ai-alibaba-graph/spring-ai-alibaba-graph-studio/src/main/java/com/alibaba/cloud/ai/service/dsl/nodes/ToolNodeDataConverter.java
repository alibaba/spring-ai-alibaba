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
import com.alibaba.cloud.ai.model.workflow.nodedata.ToolNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert the ToolNode configuration in the Dify DSL to and from the ToolNodeData object.
 */
@Component
public class ToolNodeDataConverter extends AbstractNodeDataConverter<ToolNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.TOOL.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<ToolNodeData>> getDialectConverters() {
		return Stream.of(ToolNodeDataConverter.ToolNodeConverter.values())
			.map(ToolNodeDataConverter.ToolNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum ToolNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public ToolNodeData parse(Map<String, Object> data) {
				ToolNodeData nd = new ToolNodeData();

				// llm_response_key
				nd.setLlmResponseKey((String) data.get("llm_response_key"));

				// output_key
				nd.setOutputKey((String) data.get("output_key"));

				// tool_names
				List<String> names = (List<String>) data.get("tool_names");
				if (names != null) {
					nd.setToolNames(names);
				}
				else {
					nd.setToolNames(Collections.emptyList());
				}

				// tool_callbacks
				List<String> callbacks = (List<String>) data.get("tool_callbacks");
				if (callbacks != null) {
					nd.setToolCallbacks(callbacks);
				}
				else {
					nd.setToolCallbacks(Collections.emptyList());
				}

				return nd;
			}

			@Override
			public Map<String, Object> dump(ToolNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				if (nd.getLlmResponseKey() != null) {
					m.put("llm_response_key", nd.getLlmResponseKey());
				}
				if (nd.getOutputKey() != null) {
					m.put("output_key", nd.getOutputKey());
				}
				if (nd.getToolNames() != null && !nd.getToolNames().isEmpty()) {
					m.put("tool_names", nd.getToolNames());
				}
				if (nd.getToolCallbacks() != null && !nd.getToolCallbacks().isEmpty()) {
					m.put("tool_callbacks", nd.getToolCallbacks());
				}
				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}), CUSTOM(defaultCustomDialectConverter(ToolNodeData.class));

		private final DialectConverter<ToolNodeData> converter;

		ToolNodeConverter(DialectConverter<ToolNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<ToolNodeData> dialectConverter() {
			return converter;
		}

	}

}
