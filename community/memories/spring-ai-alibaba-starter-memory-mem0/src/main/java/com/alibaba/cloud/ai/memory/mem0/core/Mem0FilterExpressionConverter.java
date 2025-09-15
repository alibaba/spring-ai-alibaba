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
package com.alibaba.cloud.ai.memory.mem0.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mem0 Filter Converter
 *
 * Converts Spring AI's Filter.Expression into a Map format supported by the Mem0 API.
 * Reference: https://docs.mem0.ai/api-reference/memory/v2-search-memories
 */
public class Mem0FilterExpressionConverter extends AbstractFilterExpressionConverter {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertExpression(Filter.Expression expression) {
		switch (expression.type()) {
			case AND:
			case OR:
			case NOT:
				return convertLogicalOperator(expression);
			case EQ:
			case NE:
			case GT:
			case GTE:
			case LT:
			case LTE:
			case IN:
			case NIN:
				return convertComparisonOperator(expression);
			default:
				throw new IllegalArgumentException("Unsupported operator: " + expression.type());
		}
	}

	private String convertLogicalOperator(Filter.Expression expression) {
		Map<String, Object> result = new HashMap<>();
		switch (expression.type()) {
			case AND:
			case OR: {
				List<Object> expressionsList = new ArrayList<>();
				if (expression.left() instanceof Filter.Expression) {
					expressionsList.add(convertExpression((Filter.Expression) expression.left()));
				}
				if (expression.right() instanceof Filter.Expression) {
					expressionsList.add(convertExpression((Filter.Expression) expression.right()));
				}
				result.put(expression.type().name(), expressionsList);
				break;
			}
			case NOT: {
				if (expression.left() instanceof Filter.Expression) {
					result.put("NOT", convertExpression((Filter.Expression) expression.left()));
				}
				break;
			}
		}
		return mapToJson(result);
	}

	private String convertComparisonOperator(Filter.Expression expression) {
		Map<String, Object> result = new HashMap<>();
		String fieldName = null;
		Object value = null;
		if (expression.left() instanceof Filter.Key) {
			fieldName = ((Filter.Key) expression.left()).key();
		}
		if (expression.right() instanceof Filter.Value) {
			value = ((Filter.Value) expression.right()).value();
		}
		if (fieldName == null || value == null) {
			throw new IllegalArgumentException("Invalid expression structure for comparison operator");
		}
		switch (expression.type()) {
			case EQ:
				result.put(fieldName, value);
				break;
			case NE:
				result.put(fieldName, Map.of("ne", value));
				break;
			case GT:
				result.put(fieldName, Map.of("gt", value));
				break;
			case GTE:
				result.put(fieldName, Map.of("gte", value));
				break;
			case LT:
				result.put(fieldName, Map.of("lt", value));
				break;
			case LTE:
				result.put(fieldName, Map.of("lte", value));
				break;
			case IN:
				result.put(fieldName, Map.of("in", value));
				break;
			case NIN:
				result.put(fieldName, Map.of("nin", value));
				break;
			default:
				throw new IllegalArgumentException("Unsupported comparison operator: " + expression.type());
		}
		return mapToJson(result);
	}

	private String mapToJson(Map<String, Object> map) {
		try {
			return objectMapper.writeValueAsString(map);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to convert map to JSON", e);
		}
	}

	@Override
	protected void doExpression(Filter.Expression expression, StringBuilder context) {
		// Optional: Implement string expression conversion
	}

	@Override
	protected void doKey(Filter.Key filterKey, StringBuilder context) {
		// Optional: Implement key-to-string conversion
	}

	// Convenience method - Create expression
	public static Filter.Expression eq(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression ne(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.NE, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression gt(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.GT, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression gte(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.GTE, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression lt(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.LT, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression lte(String field, Object value) {
		return new Filter.Expression(Filter.ExpressionType.LTE, new Filter.Key(field), new Filter.Value(value));
	}

	public static Filter.Expression in(String field, List<Object> values) {
		return new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key(field), new Filter.Value(values));
	}

	public static Filter.Expression nin(String field, List<Object> values) {
		return new Filter.Expression(Filter.ExpressionType.NIN, new Filter.Key(field), new Filter.Value(values));
	}

	public static Filter.Expression and(Filter.Expression left, Filter.Expression right) {
		return new Filter.Expression(Filter.ExpressionType.AND, left, right);
	}

	public static Filter.Expression or(Filter.Expression left, Filter.Expression right) {
		return new Filter.Expression(Filter.ExpressionType.OR, left, right);
	}

	public static Filter.Expression not(Filter.Expression expression) {
		return new Filter.Expression(Filter.ExpressionType.NOT, expression);
	}

	/**
	 * Creates an icontains filter (Mem0 specific) Note: This is not a standard Spring AI
	 * operator and requires direct construction of the Mem0 format
	 */
	public static Map<String, Object> icontains(String field, String value) {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> condition = new HashMap<>();
		condition.put("icontains", value);
		result.put(field, condition);
		return result;
	}

	/**
	 * Creates a wildcard filter
	 */
	public static Map<String, Object> wildcard(String field) {
		Map<String, Object> result = new HashMap<>();
		result.put(field, "*");
		return result;
	}

	/**
	 * Creates a wildcard expression (using Spring AI's EQ operator)
	 */
	public static Filter.Expression wildcardExpression(String field) {
		return new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(field), new Filter.Value("*"));
	}

}
