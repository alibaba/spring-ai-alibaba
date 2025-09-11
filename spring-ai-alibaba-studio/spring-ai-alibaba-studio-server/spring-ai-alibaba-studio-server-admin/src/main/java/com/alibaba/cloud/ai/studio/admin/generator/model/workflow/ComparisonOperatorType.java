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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow;

import java.util.List;
import java.util.function.BiFunction;

public enum ComparisonOperatorType {

	CONTAINS("contains", "contains", List.of(String.class, List.class),
			(objName, constVal) -> String.format("(%s.contains(%s))", objName, constVal)),
	NOT_CONTAINS("not_contains", "not contains", List.of(String.class, List.class),
			(objName, constVal) -> String.format("!(%s.contains(%s))", objName, constVal)),
	START_WITH("start_with", "start with", List.of(String.class),
			(objName, constVal) -> String.format("(%s.startsWith(%s))", objName, constVal)),
	END_WITH("end_with", "end with", List.of(String.class),
			(objName, constVal) -> String.format("(%s.endsWith(%s))", objName, constVal)),
	IS("is", "is", List.of(String.class, List.class),
			(objName, constVal) -> String.format("(%s.equals(%s))", objName, constVal)),
	IS_NOT("is_not", "is not", List.of(String.class, List.class),
			(objName, constVal) -> String.format("!(%s.equals(%s))", objName, constVal)),
	EMPTY("empty", "empty", List.of(String.class, List.class),
			(objName, constVal) -> String.format("(%s.isEmpty())", objName)),
	NOT_EMPTY("not empty", "not empty", List.of(String.class, List.class),
			(objName, constVal) -> String.format("!(%s.isEmpty())", objName)),
	IN("in", "in", List.of(String.class, List.class),
			(objName, constVal) -> String.format("(%s.contains(%s))", constVal, objName)),
	NOT_IN("not_in", "not in", List.of(String.class, List.class),
			(objName, constVal) -> String.format("!(%s.contains(%s))", constVal, objName)),
	ALL_OF("all_of", "all of", List.of(List.class),
			(objName, constVal) -> String.format("(%s.containsAll(%s))", objName, constVal)),
	EQUAL("equal", "=", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() == %s)", objName, constVal)),
	NOT_EQUAL("not_equal", "≠", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() != %s)", objName, constVal)),
	GREATER_THAN("greater_than", ">", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() > %s)", objName, constVal)),
	LESS_THAN("less_than", "<", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() < %s)", objName, constVal)),
	NOT_LESS_THAN("not_less_than", "≥", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() >= %s)", objName, constVal)),
	NOT_GREATER_THAN("not_greater_than", "≤", List.of(Number.class),
			(objName, constVal) -> String.format("(%s.doubleValue() <= %s)", objName, constVal)),
	NULL("null", "null", List.of(String.class, List.class, Number.class, Object.class),
			(objName, constVal) -> String.format("(%s == null)", objName)),
	NOT_NULL("not_null", "not null", List.of(String.class, List.class, Number.class, Object.class),
			(objName, constVal) -> String.format("(%s != null)", objName)),;

	private final String value;

	private final String difyValue;

	private final List<Class<?>> supportedClassList;

	private final BiFunction<String, String, String> toJavaExpression;

	ComparisonOperatorType(String value, String difyValue, List<Class<?>> supportedClassList,
			BiFunction<String, String, String> toJavaExpression) {
		this.value = value;
		this.difyValue = difyValue;
		this.supportedClassList = supportedClassList;
		this.toJavaExpression = toJavaExpression;
	}

	public static ComparisonOperatorType fromDifyValue(String DifyValue) {
		for (ComparisonOperatorType comparisonOperatorType : ComparisonOperatorType.values()) {
			if (comparisonOperatorType.difyValue.equals(DifyValue)) {
				return comparisonOperatorType;
			}
		}
		throw new IllegalArgumentException("Not support difyValue:" + DifyValue);
	}

	public String convert(String objName, String constValue) {
		return this.toJavaExpression.apply(objName, constValue);
	}

	public String getValue() {
		return value;
	}

	public String getDifyValue() {
		return difyValue;
	}

	public List<Class<?>> getSupportedClassList() {
		return supportedClassList;
	}

	public BiFunction<String, String, String> getToJavaExpression() {
		return toJavaExpression;
	}

	public boolean isSupported(Class<?> clazz) {
		return this.supportedClassList.contains(clazz);
	}

}
