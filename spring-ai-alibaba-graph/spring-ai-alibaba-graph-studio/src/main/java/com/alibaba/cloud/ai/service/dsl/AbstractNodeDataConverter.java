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
