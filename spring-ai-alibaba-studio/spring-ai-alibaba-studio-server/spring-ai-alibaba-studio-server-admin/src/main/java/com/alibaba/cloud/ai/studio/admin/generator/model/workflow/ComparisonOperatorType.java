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

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum ComparisonOperatorType {

	CONTAINS("contains", type -> switch (type) {
		case DIFY, STUDIO -> "contains";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("(%s.contains(%s))", objName, constVal)),
	NOT_CONTAINS("not_contains", type -> switch (type) {
		case DIFY -> "not contains";
		case STUDIO -> "notContains";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("!(%s.contains(%s))", objName, constVal)),
	START_WITH("start_with", type -> switch (type) {
		case DIFY -> "start with";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.startsWith(%s))", objName, constVal)),
	END_WITH("end_with", type -> switch (type) {
		case DIFY -> "end with";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.endsWith(%s))", objName, constVal)),
	IS("is", type -> switch (type) {
		case DIFY -> "is";
		case STUDIO -> "equals";
		default -> "unknown";
	}, VariableType.except(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.equals(%s))", objName, constVal)),
	IS_NOT("is_not", type -> switch (type) {
		case DIFY -> "is not";
		case STUDIO -> "notEquals";
		default -> "unknown";
	}, VariableType.except(VariableType.NUMBER),
			(objName, constVal) -> String.format("!(%s.equals(%s))", objName, constVal)),
	EMPTY("empty", type -> switch (type) {
		case DIFY -> "empty";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("(%s.isEmpty())", objName)),
	NOT_EMPTY("not_empty", type -> switch (type) {
		case DIFY -> "not empty";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("!(%s.isEmpty())", objName)),
	IN("in", type -> switch (type) {
		case DIFY -> "in";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("(%s.contains(%s))", constVal, objName)),
	NOT_IN("not_in", type -> switch (type) {
		case DIFY -> "not in";
		default -> "unknown";
	}, VariableType.arraysWithOther(VariableType.STRING),
			(objName, constVal) -> String.format("!(%s.contains(%s))", constVal, objName)),
	ALL_OF("all_of", type -> switch (type) {
		case DIFY -> "all of";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.containsAll(%s))", objName, constVal)),
	EQUAL("equal", type -> switch (type) {
		case DIFY -> "=";
		case STUDIO -> "equals";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() == %s)", objName, constVal)),
	NOT_EQUAL("not_equal", type -> switch (type) {
		case DIFY -> "≠";
		case STUDIO -> "notEquals";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() != %s)", objName, constVal)),
	GREATER_THAN("greater_than", type -> switch (type) {
		case DIFY -> ">";
		case STUDIO -> "greater";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() > %s)", objName, constVal)),
	LESS_THAN("less_than", type -> switch (type) {
		case DIFY -> "<";
		case STUDIO -> "less";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() < %s)", objName, constVal)),
	NOT_LESS_THAN("not_less_than", type -> switch (type) {
		case DIFY -> "≥";
		case STUDIO -> "greaterAndEqual";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() >= %s)", objName, constVal)),
	NOT_GREATER_THAN("not_greater_than", type -> switch (type) {
		case DIFY -> "≤";
		case STUDIO -> "lessAndEqual";
		default -> "unknown";
	}, List.of(VariableType.NUMBER),
			(objName, constVal) -> String.format("(%s.doubleValue() <= %s)", objName, constVal)),
	NULL("null", type -> switch (type) {
		case DIFY -> "null";
		case STUDIO -> "isNull";
		default -> "unknown";
	}, VariableType.all(), (objName, constVal) -> String.format("(%s == null)", objName)),
	NOT_NULL("not_null", type -> switch (type) {
		case DIFY -> "not null";
		case STUDIO -> "isNotNull";
		default -> "unknown";
	}, VariableType.all(), (objName, constVal) -> String.format("(%s != null)", objName)),
	LENGTH_EQUAL("length_equal", type -> switch (type) {
		case STUDIO -> "lengthEquals";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.size() == %s)", objName, constVal)),
	LENGTH_GREATER_THAN("length_greater_than", type -> switch (type) {
		case STUDIO -> "lengthGreater";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.size() > %s)", objName, constVal)),
	LENGTH_NOT_LESS_THAN("length_not_less_than", type -> switch (type) {
		case STUDIO -> "lengthGreaterAndEqual";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.size() >= %s)", objName, constVal)),
	LENGTH_LESS_THAN("length_less_than", type -> switch (type) {
		case STUDIO -> "lengthLess";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.size() < %s)", objName, constVal)),
	LENGTH_NOT_GREATER_THAN("length_not_greater_than", type -> switch (type) {
		case STUDIO -> "lengthLessAndEqual";
		default -> "unknown";
	}, VariableType.arrays(), (objName, constVal) -> String.format("(%s.size() <= %s)", objName, constVal)),

	STR_LENGTH_EQUAL("str_length_equal", type -> switch (type) {
		case STUDIO -> "lengthEquals";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.length() == %s)", objName, constVal)),
	STR_LENGTH_GREATER_THAN("str_length_greater_than", type -> switch (type) {
		case STUDIO -> "lengthGreater";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.length() > %s)", objName, constVal)),
	STR_LENGTH_NOT_LESS_THAN("str_length_not_less_than", type -> switch (type) {
		case STUDIO -> "lengthGreaterAndEqual";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.length() >= %s)", objName, constVal)),
	STR_LENGTH_LESS_THAN("str_length_less_than", type -> switch (type) {
		case STUDIO -> "lengthLess";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.length() < %s)", objName, constVal)),
	STR_LENGTH_NOT_GREATER_THAN("str_length_not_greater_than", type -> switch (type) {
		case STUDIO -> "lengthLessAndEqual";
		default -> "unknown";
	}, List.of(VariableType.STRING), (objName, constVal) -> String.format("(%s.length() <= %s)", objName, constVal)),

	IS_TRUE("is_true", type -> switch (type) {
		case STUDIO -> "isTrue";
		default -> "unknown";
	}, List.of(VariableType.BOOLEAN), (objName, constVal) -> String.format("(%s)", objName)),
	IS_FALSE("is_false", type -> switch (type) {
		case STUDIO -> "isFalse";
		default -> "unknown";
	}, List.of(VariableType.BOOLEAN), (objName, constVal) -> String.format("!(%s)", objName));

	private final String value;

	private final Function<DSLDialectType, String> dslValueFunc;

	private final List<VariableType> supportedTypes;

	private final BiFunction<String, String, String> toJavaExpression;

	ComparisonOperatorType(String value, Function<DSLDialectType, String> dslValueFunc,
			List<VariableType> supportedTypes, BiFunction<String, String, String> toJavaExpression) {
		this.value = value;
		this.dslValueFunc = dslValueFunc;
		this.supportedTypes = supportedTypes;
		this.toJavaExpression = toJavaExpression;
	}

	public static ComparisonOperatorType fromDslValue(DSLDialectType dialectType, String dslValue,
			VariableType variableType) {
		for (ComparisonOperatorType comparisonOperatorType : ComparisonOperatorType.values()) {
			if (comparisonOperatorType.dslValueFunc.apply(dialectType).equals(dslValue)
					&& comparisonOperatorType.supportedTypes.contains(variableType)) {
				return comparisonOperatorType;
			}
		}
		throw new IllegalArgumentException(
				"Not support dslValue: [" + dslValue + "] for type: [" + variableType.value() + "]");
	}

	public String convert(String objName, String constValue) {
		return this.toJavaExpression.apply(objName, constValue);
	}

	public String getValue() {
		return value;
	}

	public String getDslValue(DSLDialectType dialectType) {
		return dslValueFunc.apply(dialectType);
	}

}
