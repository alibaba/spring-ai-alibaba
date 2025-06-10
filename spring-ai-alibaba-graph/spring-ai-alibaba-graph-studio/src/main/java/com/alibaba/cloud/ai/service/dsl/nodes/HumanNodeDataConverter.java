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
import com.alibaba.cloud.ai.model.workflow.nodedata.HumanNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class HumanNodeDataConverter extends AbstractNodeDataConverter<HumanNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.HUMAN.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<HumanNodeData>> getDialectConverters() {
		return Stream.of(HumanNodeDataConverter.HumanNodeConverter.values())
			.map(HumanNodeDataConverter.HumanNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum HumanNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public HumanNodeData parse(Map<String, Object> data) {
				HumanNodeData nd = new HumanNodeData();
				// interrupt_strategy
				nd.setInterruptStrategy((String) data.getOrDefault("interrupt_strategy", "always"));
				// interrupt_condition_key
				nd.setInterruptConditionKey((String) data.get("interrupt_condition_key"));
				// state_update_keys
				List<String> keys = (List<String>) data.get("state_update_keys");
				nd.setStateUpdateKeys(keys != null ? keys : Collections.emptyList());
				return nd;
			}

			@Override
			public Map<String, Object> dump(HumanNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();
				// interrupt_strategy
				if (nd.getInterruptStrategy() != null) {
					m.put("interrupt_strategy", nd.getInterruptStrategy());
				}
				// interrupt_condition_key
				if (nd.getInterruptConditionKey() != null) {
					m.put("interrupt_condition_key", nd.getInterruptConditionKey());
				}
				// state_update_keys
				if (nd.getStateUpdateKeys() != null && !nd.getStateUpdateKeys().isEmpty()) {
					m.put("state_update_keys", nd.getStateUpdateKeys());
				}
				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}), CUSTOM(defaultCustomDialectConverter(HumanNodeData.class));

		private final DialectConverter<HumanNodeData> converter;

		HumanNodeConverter(DialectConverter<HumanNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<HumanNodeData> dialectConverter() {
			return converter;
		}

	}

}
