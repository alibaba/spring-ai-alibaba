/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class VariableAggregatorNodeDataConverter extends AbstractNodeDataConverter<VariableAggregatorNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.AGGREGATOR.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<VariableAggregatorNodeData>> getDialectConverters() {
		return Stream.of(AggregatorNodeDialectConverter.values())
			.map(AggregatorNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum AggregatorNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public VariableAggregatorNodeData parse(Map<String, Object> data) {
				Map<String, Object> advanced_settings = (Map<String, Object>) data.get("advanced_settings");
				VariableAggregatorNodeData.AdvancedSettings advancedSettings = new VariableAggregatorNodeData.AdvancedSettings();
				advancedSettings.setGroupEnabled((Boolean) advanced_settings.get("group_enabled"));
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					String groups = objectMapper.writeValueAsString(advanced_settings.get("groups"));
					advancedSettings.setGroups(objectMapper.readValue(groups, new TypeReference<>() {
					}));
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to parse JSON", e);
				}
				return VariableAggregatorNodeData.builder()
					.variables((List<List<String>>) data.get("variables"))
					.outputType((String) data.get("output_type"))
					.advancedSettings(advancedSettings)
					.build();
			}

			@Override
			public Map<String, Object> dump(VariableAggregatorNodeData nodeData) {
				Map<String, Object> result = new HashMap<>();
				HashMap<Object, Object> advancedSettings = new HashMap<>();
				VariableAggregatorNodeData.AdvancedSettings advancedSettings1 = nodeData.getAdvancedSettings();
				advancedSettings.put("group_enabled", advancedSettings1.isGroupEnabled());
				List<VariableAggregatorNodeData.Groups> groups1 = advancedSettings1.getGroups();
				List<Map<String, Object>> groups = new ArrayList<>();
				for (VariableAggregatorNodeData.Groups group : groups1) {
					Map<String, Object> groupMap = new HashMap<>();
					groupMap.put("output_type", group.getOutputType());
					groupMap.put("variables", group.getVariables());
					groupMap.put("group_name", group.getGroupName());
					groupMap.put("groupId", group.getGroupId());
					groups.add(groupMap);
				}
				advancedSettings.put("groups", groups);
				result.put("variables", nodeData.getVariables());
				result.put("output_type", nodeData.getOutputType());
				result.put("advanced_settings", advancedSettings);
				return result;
			}
		}),

		CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(VariableAggregatorNodeData.class));

		private final DialectConverter<VariableAggregatorNodeData> dialectConverter;

		public DialectConverter<VariableAggregatorNodeData> dialectConverter() {
			return dialectConverter;
		}

		AggregatorNodeDialectConverter(DialectConverter<VariableAggregatorNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
