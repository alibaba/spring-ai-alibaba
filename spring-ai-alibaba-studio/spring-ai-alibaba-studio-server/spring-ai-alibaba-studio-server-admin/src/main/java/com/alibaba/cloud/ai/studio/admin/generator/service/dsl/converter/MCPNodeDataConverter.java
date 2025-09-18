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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.MCPNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;

import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import org.springframework.stereotype.Component;

@Component
public class MCPNodeDataConverter extends AbstractNodeDataConverter<MCPNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.MCP.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<MCPNodeData>> getDialectConverters() {
		return Stream.of(MCPNodeConverter.values())
			.map(MCPNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum MCPNodeConverter {

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public MCPNodeData parse(Map<String, Object> data) {
				MCPNodeData nodeData = new MCPNodeData();

				// 获取基本信息
				String toolName = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "tool_name");
				String serverCode = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"server_code");
				String serverName = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"server_name");
				String inputJsonTemplate = MapReadUtil
					.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "input_params"))
					.stream()
					.map(map -> {
						String key = map.get("key").toString();
						String value = map.get("value").toString();
						VariableType type = VariableType.fromStudioValue(map.get("type").toString())
							.orElse(VariableType.OBJECT);

						if (VariableType.STRING.equals(type) && value != null) {
							value = "\"" + value + "\"";
						}
						return String.format("\"%s\": %s", key, value);
					})
					.collect(Collectors.joining(",\n"));
				inputJsonTemplate = "{" + inputJsonTemplate + "}";
				String outputKey = MapReadUtil
					.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "output_params"))
					.get(0)
					.get("key")
					.toString();

				// 设置节点数据
				nodeData.setToolName(toolName);
				nodeData.setServerCode(serverCode);
				nodeData.setServerName(serverName);
				nodeData.setInputJsonTemplate(inputJsonTemplate);
				nodeData.setOutputKey(outputKey);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(MCPNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(defaultCustomDialectConverter(MCPNodeData.class));

		private final DialectConverter<MCPNodeData> converter;

		MCPNodeConverter(DialectConverter<MCPNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<MCPNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "mcpNode" + count;
	}

	@Override
	public BiConsumer<MCPNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				nodeData.setOutputs(List.of(new Variable(nodeData.getOutputKey(), VariableType.STRING)));
				nodeData.setInputJsonTemplate(
						this.convertVarTemplate(dialectType, nodeData.getInputJsonTemplate(), idToVarName));
				nodeData.setInputKeys(this.getVarTemplateKeys(nodeData.getInputJsonTemplate()));
			})
				.andThen(super.postProcessConsumer(dialectType))
				.andThen((nodeData, idToVarName) -> nodeData.setOutputKey(nodeData.getOutputs().get(0).getName()));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
