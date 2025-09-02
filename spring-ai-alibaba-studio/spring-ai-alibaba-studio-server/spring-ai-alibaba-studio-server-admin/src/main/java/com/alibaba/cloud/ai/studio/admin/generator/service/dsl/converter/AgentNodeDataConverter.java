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
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.AgentNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Component
public class AgentNodeDataConverter extends AbstractNodeDataConverter<AgentNodeData> {

	@Override
	protected List<DialectConverter<AgentNodeData>> getDialectConverters() {
		return Stream.of(AgentNodeDialectConverter.values()).map(AgentNodeDialectConverter::dialectConverter).toList();
	}

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.AGENT.equals(nodeType);
	}

	private enum AgentNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public AgentNodeData parse(Map<String, Object> data) {
				AgentNodeData nodeData = new AgentNodeData();
				Map<String, Object> agentParametersMap = MapReadUtil
					.safeCastToMapWithStringKey(MapReadUtil.getMapDeepValue(data, Map.class, "agent_parameters"));
				if (agentParametersMap != null) {
					nodeData.setAgentParameterMap(agentParametersMap);
					nodeData.setInstructionPrompt(
							MapReadUtil.getMapDeepValue(agentParametersMap, String.class, "instruction", "value"));
					nodeData.setQueryPrompt(
							MapReadUtil.getMapDeepValue(agentParametersMap, String.class, "query", "value"));
					List<?> toolListValue = MapReadUtil.getMapDeepValue(agentParametersMap, List.class, "tools",
							"value");
					if (toolListValue != null) {
						List<AgentNodeData.ToolData> toolDataList = toolListValue.stream()
							.map(MapReadUtil::safeCastToMapWithStringKey)
							.filter(Objects::nonNull)
							.map(mp -> MapReadUtil.castMapToRecord(mp, AgentNodeData.ToolData.class))
							.filter(Objects::nonNull)
							.toList();
						nodeData.setToolList(toolDataList);
					}
				}
				String agentStrategyName = MapReadUtil.getMapDeepValue(data, String.class, "agent_strategy_name");
				nodeData.setAgentStrategyName(agentStrategyName);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(AgentNodeData nodeData) {
				return Map.of();
			}
		}),

		CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(AgentNodeData.class));

		private final DialectConverter<AgentNodeData> dialectConverter;

		public DialectConverter<AgentNodeData> dialectConverter() {
			return dialectConverter;
		}

		AgentNodeDialectConverter(DialectConverter<AgentNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "agentNode" + count;
	}

	@Override
	public BiConsumer<AgentNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				nodeData.setOutputs(List.of(AgentNodeData.getDefaultOutputSchema()));
				// 处理指令和查询的模板
				nodeData.setQueryPrompt(this.convertVarTemplate(dialectType, nodeData.getQueryPrompt(), idToVarName));
				nodeData.setInstructionPrompt(
						this.convertVarTemplate(dialectType, nodeData.getInstructionPrompt(), idToVarName));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
