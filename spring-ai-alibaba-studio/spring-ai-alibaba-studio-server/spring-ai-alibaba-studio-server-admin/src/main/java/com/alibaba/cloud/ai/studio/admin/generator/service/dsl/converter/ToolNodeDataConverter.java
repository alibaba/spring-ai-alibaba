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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ToolNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import org.springframework.stereotype.Component;

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
		return Stream.of(ToolNodeConverter.values())
			.map(ToolNodeConverter::dialectConverter)
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

				// Handle tool_names - extract from tool_name if tool_names not present
				List<String> names = (List<String>) data.get("tool_names");
				if (names != null) {
					nd.setToolNames(names);
				}
				else {
					// Try to extract tool name from tool_name field (single tool)
					String toolName = (String) data.get("tool_name");
					if (toolName != null) {
						nd.setToolNames(Collections.singletonList(toolName));
					}
					else {
						nd.setToolNames(Collections.emptyList());
					}
				}

				// tool_callbacks
				List<String> callbacks = (List<String>) data.get("tool_callbacks");
				if (callbacks != null) {
					nd.setToolCallbacks(callbacks);
				}
				else {
					nd.setToolCallbacks(Collections.emptyList());
				}

				// Parse Dify-specific tool attributes
				nd.setToolName((String) data.get("tool_name"));
				nd.setToolDescription((String) data.get("tool_description"));
				nd.setToolLabel((String) data.get("tool_label"));
				nd.setProviderId((String) data.get("provider_id"));
				nd.setProviderName((String) data.get("provider_name"));
				nd.setProviderType((String) data.get("provider_type"));
				nd.setIsTeamAuthorization((Boolean) data.get("is_team_authorization"));
				nd.setOutputSchema(data.get("output_schema"));

				// Parse tool_parameters
				Map<String, Object> toolParameters = (Map<String, Object>) data.get("tool_parameters");
				if (toolParameters != null) {
					nd.setToolParameters(toolParameters);
				}

				// Parse tool_configurations
				Map<String, Object> toolConfigurations = (Map<String, Object>) data.get("tool_configurations");
				if (toolConfigurations != null) {
					nd.setToolConfigurations(toolConfigurations);
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

				// Export Dify-specific attributes
				if (nd.getToolName() != null) {
					m.put("tool_name", nd.getToolName());
				}
				if (nd.getToolDescription() != null) {
					m.put("tool_description", nd.getToolDescription());
				}
				if (nd.getToolLabel() != null) {
					m.put("tool_label", nd.getToolLabel());
				}
				if (nd.getProviderId() != null) {
					m.put("provider_id", nd.getProviderId());
				}
				if (nd.getProviderName() != null) {
					m.put("provider_name", nd.getProviderName());
				}
				if (nd.getProviderType() != null) {
					m.put("provider_type", nd.getProviderType());
				}
				if (nd.getIsTeamAuthorization() != null) {
					m.put("is_team_authorization", nd.getIsTeamAuthorization());
				}
				if (nd.getOutputSchema() != null) {
					m.put("output_schema", nd.getOutputSchema());
				}
				if (nd.getToolParameters() != null && !nd.getToolParameters().isEmpty()) {
					m.put("tool_parameters", nd.getToolParameters());
				}
				if (nd.getToolConfigurations() != null && !nd.getToolConfigurations().isEmpty()) {
					m.put("tool_configurations", nd.getToolConfigurations());
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

	@Override
	public String generateVarName(int count) {
		return "toolNode" + count;
	}

	@Override
	public BiConsumer<ToolNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen(((nodeData, map) -> {
				Variable output = ToolNodeData.getDefaultOutputSchema();
				nodeData.setOutputs(List.of(output));
				nodeData.setOutputKey(nodeData.getVarName() + "_" + output.getName());
			})).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
