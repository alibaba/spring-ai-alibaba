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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;

/**
 * @author 北极星
 */
public class OpenSearchVectorStoreFilterExpressionConverter extends AbstractFilterExpressionConverter {

	private static final Pattern DATE_FORMAT_PATTERN = Pattern
		.compile("\\d{4}-\\d{2}-\\d{2}T\\d" + "{2}:\\d{2}:\\d{2}Z");

	private final SimpleDateFormat dateFormat;

	public OpenSearchVectorStoreFilterExpressionConverter() {
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		this.dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(8))));
	}

	@Override
	protected void doExpression(Expression expression, StringBuilder context) {
		if (expression.type() == Filter.ExpressionType.IN || expression.type() == Filter.ExpressionType.NIN) {
			context.append(getOperationSymbol(expression));
			context.append("(");
			this.convertOperand(expression.left(), context);
			this.convertOperand(expression.right(), context);
			context.append(")");
		}
		else {
			this.convertOperand(expression.left(), context);
			context.append(getOperationSymbol(expression));
			this.convertOperand(expression.right(), context);
		}
	}

	private String getOperationSymbol(Expression exp) {
		return switch (exp.type()) {
			case AND -> " AND ";
			case OR -> " OR ";
			case EQ, IN -> "";
			case NE -> " NOT ";
			case LT -> "<";
			case LTE -> "<=";
			case GT -> ">";
			case GTE -> ">=";
			case NIN -> "NOT ";
			default -> throw new RuntimeException("Not supported expression type: " + exp.type());
		};
	}

	public String withMetaPrefix(String identifier) {
		return "metadata." + identifier;
	}

	@Override
	protected void doAddValueRangeSpitter(Filter.Value listValue, StringBuilder context) {
		context.append(" OR ");
	}

	@Override
	public void doKey(Filter.Key key, StringBuilder context) {
		var identifier = hasOuterQuotes(key.key()) ? removeOuterQuotes(key.key()) : key.key();
		var prefixedIdentifier = withMetaPrefix(identifier);
		context.append(prefixedIdentifier.trim()).append(":");
	}

	@Override
	protected void doValue(Filter.Value filterValue, StringBuilder context) {
		if (filterValue.value() instanceof List list) {
			int c = 0;
			for (Object v : list) {
				context.append(v);
				if (c++ < list.size() - 1) {
					this.doAddValueRangeSpitter(filterValue, context);
				}
			}
		}
		else {
			this.doSingleValue(filterValue.value(), context);
		}
	}

	@Override
	protected void doSingleValue(Object value, StringBuilder context) {
		if (value instanceof Date date) {
			context.append(this.dateFormat.format(date));
		}
		else if (value instanceof String text) {
			if (DATE_FORMAT_PATTERN.matcher(text).matches()) {
				try {
					Date date = this.dateFormat.parse(text);
					context.append(this.dateFormat.format(date));
				}
				catch (ParseException e) {
					throw new IllegalArgumentException("Invalid date type:" + text, e);
				}
			}
			else {
				context.append(text);
			}
		}
		else {
			context.append(value);
		}
	}

	@Override
	public void doStartGroup(Filter.Group group, StringBuilder context) {
		context.append("(");
	}

	@Override
	public void doEndGroup(Filter.Group group, StringBuilder context) {
		context.append(")");
	}

}
