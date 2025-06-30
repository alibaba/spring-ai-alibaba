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
package com.alibaba.cloud.ai.vectorstore.tablestore;

import com.aliyun.openservices.tablestore.agent.model.Document;
import com.aliyun.openservices.tablestore.agent.model.filter.Filters;
import com.aliyun.openservices.tablestore.agent.util.Exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.ai.vectorstore.filter.Filter.Expression;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import static org.springframework.ai.vectorstore.filter.Filter.Group;
import static org.springframework.ai.vectorstore.filter.Filter.Key;
import static org.springframework.ai.vectorstore.filter.Filter.Operand;
import static org.springframework.ai.vectorstore.filter.Filter.Value;

class TablestoreExpressionConverter {

	static com.aliyun.openservices.tablestore.agent.model.filter.Filter convertOperand(Operand operand,
			Set<String> tenantIds, boolean enableParseTenantIds) {
		if (operand == null) {
			return null;
		}
		if (operand instanceof Expression expression) {
			if (expression.type() == ExpressionType.NOT) {
				if (expression.left() instanceof Group group) {
					return Filters.not(convertOperand(group.content(), tenantIds, false));
				}
				if (expression.left() instanceof Expression leftExpression) {
					return Filters.not(convertOperand(leftExpression, tenantIds, false));
				}
				throw Exceptions.illegalArgument(
						"the left value must be Group or Expression when operand type is NOT, but got:%s",
						expression.left());
			}
			else if (expression.type() == ExpressionType.AND) {
				return Filters.and(convertOperand(expression.left(), tenantIds, enableParseTenantIds),
						convertOperand(expression.right(), tenantIds, enableParseTenantIds));
			}
			else if (expression.type() == ExpressionType.OR) {
				return Filters.or(convertOperand(expression.left(), tenantIds, false),
						convertOperand(expression.right(), tenantIds, false));
			}
			else {
				if (!(expression.left() instanceof Key && expression.right() instanceof Value)) {
					throw Exceptions.illegalArgument(
							"the left value and right value must be Key and Value when operand type is %s, but got left:%s, right:%s",
							expression.type(), expression.left(), expression.right());
				}
				return parseComparison((Key) expression.left(), (Value) expression.right(), expression, tenantIds,
						enableParseTenantIds);
			}
		}
		if (operand instanceof Group group) {
			return convertOperand(group.content(), tenantIds, enableParseTenantIds);
		}
		throw Exceptions.illegalArgument("unsupported operand type:%s in this layer", operand);
	}

	private static com.aliyun.openservices.tablestore.agent.model.filter.Filter parseComparison(Key key, Value value,
			Expression exp, Set<String> tenantIds, boolean enableParseTenantIds) {
		ExpressionType type = exp.type();
		switch (type) {
			case EQ:
				updateTenantIds(key, value, tenantIds, enableParseTenantIds);
				return Filters.eq(key.key(), value.value());
			case NE:
				return Filters.notEq(key.key(), value.value());
			case GT:
				return Filters.gt(key.key(), value.value());
			case GTE:
				return Filters.gte(key.key(), value.value());
			case LT:
				return Filters.lt(key.key(), value.value());
			case LTE:
				return Filters.lte(key.key(), value.value());
			case IN:
				updateTenantIds(key, value, tenantIds, enableParseTenantIds);
				return Filters.in(key.key(), parseList(key, value));
			case NIN:
				return Filters.notIn(key.key(), parseList(key, value));
			default:
				throw Exceptions.illegalArgument("unsupported expression type:%s for key:%s, value:%s", type, key.key(),
						value.value());
		}
	}

	private static List<Object> parseList(Key key, Value value) {
		if (value.value() instanceof List<?> valueList && !valueList.isEmpty()) {
			return new ArrayList<>(valueList);
		}
		throw Exceptions.illegalArgument("unsupported value type for key:%s, only supports list but got:%s", key.key(),
				value.value());
	}

	private static void updateTenantIds(Key key, Value value, Set<String> tenantIds, boolean enableParseTenantIds) {
		if (enableParseTenantIds && key.key().equals(Document.DOCUMENT_TENANT_ID)) {
			if (value.value() instanceof List<?> valueList) {
				if (!valueList.isEmpty()) {
					for (Object object : valueList) {
						if (object instanceof String str) {
							tenantIds.add(str);
						}
						else {
							throw Exceptions.illegalArgument("key:%s only supports list of string but got:%s",
									key.key(), value.value());
						}
					}
				}
			}
			else if (value.value() instanceof String str) {
				tenantIds.add(str);
			}
			else {
				throw Exceptions.illegalArgument("key:%s only supports single string or list of string but got:%s",
						key.key(), value.value());
			}
		}
	}

}
