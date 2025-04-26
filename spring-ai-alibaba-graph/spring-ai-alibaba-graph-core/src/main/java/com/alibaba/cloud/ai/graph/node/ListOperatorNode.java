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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ListOperatorNode<T extends ListOperatorNode.ListElement> implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(ListOperatorNode.class);

	private final String inputTextKey; // 默认“input”

	private final String outputTextKey; // 默认“output”

	// 过滤条件
	private final Predicate<T> filterChain;

	// 排序条件
	private final Comparator<T> comparatorChain;

	// 取前几个值
	private final Long limitNumber;

	private ListOperatorNode(String inputTextKey, String outputTextKey, Predicate<T> filterChain,
			Comparator<T> comparatorChain, Long limitNumber) {
		this.inputTextKey = inputTextKey;
		this.outputTextKey = outputTextKey;
		this.filterChain = filterChain;
		this.comparatorChain = comparatorChain;
		this.limitNumber = limitNumber;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String inputJsonString = (String) t.value(inputTextKey).orElse(null);
			if (inputJsonString == null) {
				throw new RuntimeException("inputJsonString is null");
			}
			List<T> listElements = objectMapper.readValue(inputJsonString, new TypeReference<List<T>>() {
			})
				.stream()
				.filter(filterChain)
				.sorted(comparatorChain)
				.limit(limitNumber != null && limitNumber > 0 ? limitNumber : Long.MAX_VALUE)
				.toList();
			return Map.of(outputTextKey, objectMapper.writeValueAsString(listElements));
		}
		catch (Exception e) {
			log.error("ListOperatorNode apply failed, message: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public interface ListElement {

	}

	public static class NumberElement implements ListElement {

		Number value;

		@JsonCreator
		public NumberElement(Number value) {
			this.value = value;
		}

		@JsonValue
		public Number getValue() {
			return value;
		}

		@Override
		public String toString() {
			return value.toString();
		}

		// 一些预定义的Predicate和Comparator，可以供用户直接使用
		public boolean isInteger() {
			return value instanceof Integer;
		}

		public boolean isDouble() {
			return value instanceof Double;
		}

		public boolean greater(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) > 0;
		}

		public boolean less(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) < 0;
		}

		public boolean noGreater(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) <= 0;
		}

		public boolean noLess(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) >= 0;
		}

		public boolean equal(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) == 0;
		}

		public boolean noEqual(Number val) {
			return Double.compare(value.doubleValue(), val.doubleValue()) != 0;
		}

		public int compareTo(NumberElement other) {
			return Double.compare(this.value.doubleValue(), other.value.doubleValue());
		}

		public int compareToReverse(NumberElement other) {
			return Double.compare(other.value.doubleValue(), this.value.doubleValue());
		}

	}

	public static class StringElement implements ListElement {

		String value;

		@JsonCreator
		public StringElement(String value) {
			this.value = value;
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FileElement implements ListElement {

		String type;

		Integer size;

		String name;

		String url;

		String extension;

		String mimeType;

		String transferMethod;

		@JsonCreator
		public FileElement(@JsonProperty("type") String type, @JsonProperty("size") Integer size,
				@JsonProperty("name") String name, @JsonProperty("url") String url,
				@JsonProperty("extension") String extension, @JsonProperty("mime_type") String mimeType,
				@JsonProperty("transfer_method") String transferMethod) {
			this.type = type;
			this.size = size;
			this.name = name;
			this.url = url;
			this.extension = extension;
			this.mimeType = mimeType;
			this.transferMethod = transferMethod;
		}

		@JsonProperty("type")
		public String getType() {
			return type;
		}

		@JsonProperty("size")
		public Integer getSize() {
			return size;
		}

		@JsonProperty("name")
		public String getName() {
			return name;
		}

		@JsonProperty("url")
		public String getUrl() {
			return url;
		}

		@JsonProperty("extension")
		public String getExtension() {
			return extension;
		}

		@JsonProperty("mime_type")
		public String getMimeType() {
			return mimeType;
		}

		@JsonProperty("transfer_method")
		public String getTransferMethod() {
			return transferMethod;
		}

		@Override
		public String toString() {
			return "FileElement{" + "type='" + type + '\'' + ", size=" + size + ", name='" + name + '\'' + ", url='"
					+ url + '\'' + ", extension='" + extension + '\'' + ", mimeType='" + mimeType + '\''
					+ ", transferMethod='" + transferMethod + '\'' + '}';
		}

		// 一些预定义的Predicate和Comparator，可以供用户直接使用
		public boolean includeExtension(String... extensions) {
			for (String extension : extensions) {
				if (this.extension.equals(extension)) {
					return true;
				}
			}
			return false;
		}

		public boolean excludeExtension(String... extensions) {
			for (String extension : extensions) {
				if (this.extension.equals(extension)) {
					return false;
				}
			}
			return true;
		}

		public boolean includeType(String... types) {
			for (String type : types) {
				if (this.type.equals(type)) {
					return true;
				}
			}
			return false;
		}

		public boolean excludeType(String... types) {
			for (String type : types) {
				if (this.type.equals(type)) {
					return false;
				}
			}
			return true;
		}

		public boolean sizeNoBiggerThan(Integer sizeLimit) {
			return this.size.compareTo(sizeLimit) <= 0;
		}

		public boolean sizeNoLessThan(Integer sizeLimit) {
			return this.size.compareTo(sizeLimit) >= 0;
		}

		public boolean nameStartWith(String prefix) {
			return this.name.startsWith(prefix);
		}

		public boolean nameEndWith(String suffix) {
			return this.name.endsWith(suffix);
		}

		public boolean nameContains(String sub) {
			return this.name.contains(sub);
		}

		public int compareType(FileElement other) {
			return this.type.compareTo(other.type);
		}

		public int compareTypeReverse(FileElement other) {
			return other.type.compareTo(this.type);
		}

		public int compareSize(FileElement other) {
			return this.size.compareTo(other.size);
		}

		public int compareSizeReverse(FileElement other) {
			return other.type.compareTo(this.type);
		}

		public int compareName(FileElement other) {
			return this.name.compareTo(other.name);
		}

		public int compareNameReverse(FileElement other) {
			return other.name.compareTo(this.name);
		}

		public int compareExtension(FileElement other) {
			return this.extension.compareTo(other.extension);
		}

		public int compareExtensionReverse(FileElement other) {
			return other.extension.compareTo(this.extension);
		}

	}

	public static class Builder<T extends ListElement> {

		private String inputTextKey;

		private String outputTextKey;

		private final List<Predicate<T>> filters;

		private final List<Comparator<T>> comparators;

		private Long limitNumber;

		private Builder() {
			inputTextKey = "input";
			outputTextKey = "output";
			filters = new ArrayList<>();
			comparators = new ArrayList<>();
			limitNumber = null;
		}

		public Builder<T> inputTextKey(String inputTextKey) {
			this.inputTextKey = inputTextKey;
			return this;
		}

		public Builder<T> outputTextKey(String outputTextKey) {
			this.outputTextKey = outputTextKey;
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

		public ListOperatorNode<T> build() {
			return new ListOperatorNode<T>(inputTextKey, outputTextKey,
					filters.stream().reduce(Predicate::and).orElse(t -> true),
					comparators.stream().reduce(Comparator::thenComparing).orElse((a, b) -> 0), limitNumber);
		}

	}

	public static <T extends ListElement> Builder<T> builder() {
		return new Builder<T>();
	}

}
