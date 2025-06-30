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
package com.alibaba.cloud.ai.analyticdb;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.Group;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

import java.util.List;

/**
 * @author HeYQ
 */
public class AdVectorFilterExpressionConverter extends AbstractFilterExpressionConverter {

	@Override
	protected void doExpression(Expression expression, StringBuilder context) {
		if (expression.type() == Filter.ExpressionType.IN) {
			handleIn(expression, context);
		}
		else if (expression.type() == Filter.ExpressionType.NIN) {
			handleNotIn(expression, context);
		}
		else {
			this.convertOperand(expression.left(), context);
			context.append(getOperationSymbol(expression));
			this.convertOperand(expression.right(), context);
		}
	}

	private void handleIn(Expression expression, StringBuilder context) {
		context.append("(");
		convertToConditions(expression, context);
		context.append(")");
	}

	private void convertToConditions(Expression expression, StringBuilder context) {
		Filter.Value right = (Filter.Value) expression.right();
		Object value = right.value();
		if (!(value instanceof List)) {
			throw new IllegalArgumentException("Expected a List, but got: " + value.getClass().getSimpleName());
		}
		List<Object> values = (List) value;
		for (int i = 0; i < values.size(); i++) {
			this.convertOperand(expression.left(), context);
			context.append(" == ");
			this.doSingleValue(values.get(i), context);
			if (i < values.size() - 1) {
				context.append(" || ");
			}
		}
	}

	private void handleNotIn(Expression expression, StringBuilder context) {
		context.append("!(");
		convertToConditions(expression, context);
		context.append(")");
	}

	private String getOperationSymbol(Expression exp) {
		switch (exp.type()) {
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
			// case IN:
			// return "IN";
			// case NIN:
			// return " NOT IN";
			default:
				throw new RuntimeException("Not supported expression type: " + exp.type());
		}
	}

	@Override
	protected void doKey(Key key, StringBuilder context) {
		context.append("$." + key.key());
	}

	@Override
	protected void doStartGroup(Group group, StringBuilder context) {
		context.append("(");
	}

	@Override
	protected void doEndGroup(Group group, StringBuilder context) {
		context.append(")");
	}

}
