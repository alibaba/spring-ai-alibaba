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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 对某一个列表进行过滤、排序、获取数据
 *
 * @param <T> 列表中的元素
 */
public class ListOperatorNode<T> implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(ListOperatorNode.class);

	// 输入输出的类型
	public enum Mode {

		JSON_STR, // JSON字符串
		LIST, // 列表
		ARRAY // 数组

	}

	private final Mode mode;

	// default value "input"
	private final String inputKey;

	// default value "output"
	private final String outputKey;

	// filter conditions
	private final Predicate<T> filterChain;

	// sort conditions
	private final Comparator<T> comparatorChain;

	// limit value
	private final Long limitNumber;

	// Generic concrete class, JSON deserialization usage
	private final Class<T> type;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ListOperatorNode(Mode mode, String inputKey, String outputKey, Predicate<T> filterChain,
			Comparator<T> comparatorChain, Long limitNumber, Class<T> type) {
		this.mode = mode;
		this.inputKey = inputKey;
		this.outputKey = outputKey;
		this.filterChain = filterChain;
		this.comparatorChain = comparatorChain;
		this.limitNumber = limitNumber;
		this.type = type;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {
		try {
			Object inputObject = t.value(inputKey).orElse(null);
			List<T> inputList = switch (mode) {
				case JSON_STR -> {
					if (!(inputObject instanceof String)) {
						throw new RuntimeException("input is not String");
					}
					String inputJsonString = inputObject.toString();
					JavaType javaType = objectMapper.getTypeFactory().constructParametricType(List.class, type);
					yield objectMapper.readValue(inputJsonString, javaType);
				}
				case LIST -> {
					if (!(inputObject instanceof List)) {
						throw new RuntimeException("input is not List");
					}
					yield (List<T>) inputObject;
				}
				case ARRAY -> {
					if (inputObject == null || !inputObject.getClass().isArray()) {
						throw new RuntimeException("input is not Array");
					}
					yield Arrays.asList((T[]) inputObject);
				}
			};
			List<T> listElements = inputList.stream()
				.filter(filterChain)
				.sorted(comparatorChain)
				.limit(limitNumber != null && limitNumber > 0 ? limitNumber : Long.MAX_VALUE)
				.toList();

			Object output = switch (mode) {
				case JSON_STR -> objectMapper.writeValueAsString(listElements);
				case LIST -> listElements;
				case ARRAY -> listElements.toArray();
			};

			return Map.of(outputKey, output);
		}
		catch (Exception e) {
			log.error("ListOperatorNode apply failed, message: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static class Builder<T> {

		private Mode mode;

		private String inputKey;

		private String outputKey;

		private final List<Predicate<T>> filters;

		private final List<Comparator<T>> comparators;

		private Long limitNumber;

		private Class<T> type;

		private Builder() {
			mode = Mode.JSON_STR;
			inputKey = "input";
			outputKey = "output";
			filters = new ArrayList<>();
			comparators = new ArrayList<>();
			limitNumber = null;
		}

		public Builder<T> mode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder<T> inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

		public Builder<T> outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder<T> filter(Predicate<T> filter) {
			filters.add(filter);
			return this;
		}

		public Builder<T> comparator(Comparator<T> comparator) {
			comparators.add(comparator);
			return this;
		}

		public Builder<T> limitNumber(Long limitNumber) {
			this.limitNumber = limitNumber;
			return this;
		}

		public Builder<T> limitNumber(Integer limitNumber) {
			this.limitNumber = Long.valueOf(limitNumber);
			return this;
		}

		public Builder<T> elementClassType(Class<T> type) {
			this.type = type;
			return this;
		}

		public ListOperatorNode<T> build() {
			if (type == null && mode == Mode.JSON_STR) {
				throw new IllegalArgumentException("ElementClassType is required");
			}
			// Merge List<Predicate> and List<Comparator> into a single Predicate and a
			// single Comparator, respectively.
			return new ListOperatorNode<T>(mode, inputKey, outputKey,
					filters.stream().reduce(Predicate::and).orElse(t -> true),
					comparators.stream().reduce(Comparator::thenComparing).orElse((a, b) -> 0), limitNumber, type);
		}

	}

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

}
