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

package com.alibaba.cloud.ai.vectorstore.analyticdb;

import java.util.List;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

/**
 * Converts {@link Filter.Expression} objects into the query syntax expected by the
 * AnalyticDB service.
 *
 * <p>
 * The AnalyticDB filter language only supports a subset of Spring AI operators, so we
 * map the supported ones (comparison, logical, {@code IN}/{@code NIN}) and raise an
 * exception for unsupported variants.
 * </p>
 *
 * @author saladday
 */
public class AdVectorFilterExpressionConverter extends AbstractFilterExpressionConverter {

	@Override
	protected void doExpression(Filter.Expression expression, StringBuilder context) {
		if (expression.type() == Filter.ExpressionType.IN) {
			handleIn(expression, context);
			return;
		}
		if (expression.type() == Filter.ExpressionType.NIN) {
			handleNotIn(expression, context);
			return;
		}

		convertOperand(expression.left(), context);
		context.append(operationSymbolFor(expression));
		convertOperand(expression.right(), context);
	}

	private void handleIn(Filter.Expression expression, StringBuilder context) {
		context.append("(");
		appendEqualityChain(expression, context);
		context.append(")");
	}

	private void handleNotIn(Filter.Expression expression, StringBuilder context) {
		context.append("!(");
		appendEqualityChain(expression, context);
		context.append(")");
	}

	private void appendEqualityChain(Filter.Expression expression, StringBuilder context) {
		Filter.Value right = (Filter.Value) expression.right();
		Object value = right.value();
		if (!(value instanceof List<?>)) {
			throw new IllegalArgumentException(
					"Expected a List value but got: " + value.getClass().getSimpleName());
		}

		List<?> values = (List<?>) value;
		for (int index = 0; index < values.size(); index++) {
			convertOperand(expression.left(), context);
			context.append(" == ");
			doSingleValue(values.get(index), context);
			if (index < values.size() - 1) {
				context.append(" || ");
			}
		}
	}

	private String operationSymbolFor(Filter.Expression expression) {
		switch (expression.type()) {
			case AND:
				return " && ";
			case OR:
				return " || ";
			case EQ:
				return " = ";
			case NE:
				return " != ";
			case LT:
				return " < ";
			case LTE:
				return " <= ";
			case GT:
				return " > ";
			case GTE:
				return " >= ";
			default:
				throw new IllegalArgumentException(
						"Unsupported expression type: " + expression.type());
		}
	}

	@Override
	protected void doKey(Filter.Key key, StringBuilder context) {
		context.append("$.").append(key.key());
	}

	@Override
	protected void doStartGroup(Filter.Group group, StringBuilder context) {
		context.append("(");
	}

	@Override
	protected void doEndGroup(Filter.Group group, StringBuilder context) {
		context.append(")");
	}

}
