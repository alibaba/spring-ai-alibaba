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
package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;

/**
 * AbstractNodeDataConverter defines the interface to convert node data using a
 * combination of dsl dialect and node types
 */
public abstract class AbstractNodeDataConverter<T extends NodeData> implements NodeDataConverter<T> {

	@Override
	public T parseMapData(Map<String, Object> data, DSLDialectType dialectType) {
		DialectConverter<T> converter = getDialectConverters().stream()
			.filter(c -> c.supportDialect(dialectType))
			.findFirst()
			.orElseThrow(() -> new NotImplementedException("Unsupported dialect type: " + dialectType.value()));
		return converter.parse(data);
	}

	@Override
	public Map<String, Object> dumpMapData(T nodeData, DSLDialectType dialectType) {
		DialectConverter<T> converter = getDialectConverters().stream()
			.filter(c -> c.supportDialect(dialectType))
			.findFirst()
			.orElseThrow(() -> new NotImplementedException("Unsupported dialect type: " + dialectType.value()));
		return converter.dump(nodeData);
	}

	/**
	 * DialectConverter defines the interface to convert node data in different dsl
	 * dialects.
	 */
	public interface DialectConverter<T> {

		Boolean supportDialect(DSLDialectType dialectType);

		T parse(Map<String, Object> data);

		Map<String, Object> dump(T nodeData);

	}

	public static <R> DialectConverter<R> defaultCustomDialectConverter(Class<R> clazz) {
		return new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.CUSTOM.equals(dialectType);
			}

			@Override
			public R parse(Map<String, Object> data) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
				return objectMapper.convertValue(data, clazz);
			}

			@Override
			public Map<String, Object> dump(R nodeData) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
				return objectMapper.convertValue(nodeData, new TypeReference<>() {
				});
			}
		};
	}

	protected abstract List<DialectConverter<T>> getDialectConverters();

}
