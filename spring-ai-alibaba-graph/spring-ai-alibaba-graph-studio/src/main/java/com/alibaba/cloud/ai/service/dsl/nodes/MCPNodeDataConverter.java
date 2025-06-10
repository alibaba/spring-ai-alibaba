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
import com.alibaba.cloud.ai.model.workflow.nodedata.MCPNodeData;
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
 * Convert the MCP node configuration in the Dify DSL to and from the MCPNodeData object.
 */
@Component
public class MCPNodeDataConverter extends AbstractNodeDataConverter<MCPNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.MCP.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<MCPNodeData>> getDialectConverters() {
		return Stream.of(MCPNodeDataConverter.MCPNodeConverter.values())
			.map(MCPNodeDataConverter.MCPNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum MCPNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public MCPNodeData parse(Map<String, Object> data) {
				MCPNodeData nd = new MCPNodeData();

				// url
				nd.setUrl((String) data.get("url"));

				// tool
				nd.setTool((String) data.get("tool"));

				// headers (Map<String, String>)
				Map<String, String> hmap = (Map<String, String>) data.get("headers");
				if (hmap != null) {
					nd.setHeaders(new LinkedHashMap<>(hmap));
				}

				// params (Map<String, Object>)
				Map<String, Object> pmap = (Map<String, Object>) data.get("params");
				if (pmap != null) {
					nd.setParams(new LinkedHashMap<>(pmap));
				}

				// output_key
				nd.setOutputKey((String) data.get("output_key"));

				// input_param_keys (List<String>)
				List<String> ipk = (List<String>) data.get("input_param_keys");
				if (ipk != null) {
					nd.setInputParamKeys(ipk);
				}
				else {
					nd.setInputParamKeys(Collections.emptyList());
				}

				return nd;
			}

			@Override
			public Map<String, Object> dump(MCPNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				// url
				if (nd.getUrl() != null) {
					m.put("url", nd.getUrl());
				}

				// tool
				if (nd.getTool() != null) {
					m.put("tool", nd.getTool());
				}

				// headers
				if (nd.getHeaders() != null && !nd.getHeaders().isEmpty()) {
					m.put("headers", nd.getHeaders());
				}

				// params
				if (nd.getParams() != null && !nd.getParams().isEmpty()) {
					m.put("params", nd.getParams());
				}

				// output_key
				if (nd.getOutputKey() != null) {
					m.put("output_key", nd.getOutputKey());
				}

				// input_param_keys
				if (nd.getInputParamKeys() != null && !nd.getInputParamKeys().isEmpty()) {
					m.put("input_param_keys", nd.getInputParamKeys());
				}

				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}), CUSTOM(defaultCustomDialectConverter(MCPNodeData.class));

		private final DialectConverter<MCPNodeData> converter;

		MCPNodeConverter(DialectConverter<MCPNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<MCPNodeData> dialectConverter() {
			return converter;
		}

	}

}
