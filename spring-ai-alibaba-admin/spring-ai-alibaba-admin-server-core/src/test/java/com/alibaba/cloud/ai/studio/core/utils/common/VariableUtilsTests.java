/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.core.utils.common;

import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link VariableUtils}.
 */
class VariableUtilsTests {

	// ---------------------------------------------------------------------
	// getExpressionFromBracket
	// ---------------------------------------------------------------------

	@Test
	void getExpressionFromBracketStripsVariableWrapper() {
		assertThat(VariableUtils.getExpressionFromBracket("${user.name}")).isEqualTo("user.name");
	}

	@Test
	void getExpressionFromBracketReturnsPlainExpressionUnchanged() {
		assertThat(VariableUtils.getExpressionFromBracket("user.name")).isEqualTo("user.name");
	}

	@Test
	void getExpressionFromBracketReturnsNullOnBlank() {
		assertThat(VariableUtils.getExpressionFromBracket(null)).isNull();
		assertThat(VariableUtils.getExpressionFromBracket("  ")).isNull();
	}

	// ---------------------------------------------------------------------
	// getValueFromPayload / getValueStringFromPayload
	// ---------------------------------------------------------------------

	@Test
	void getValueFromPayloadResolvesTopLevelKey() {
		Map<String, Object> payload = Map.of("name", "kafka");
		assertThat(VariableUtils.getValueFromPayload("name", payload)).isEqualTo("kafka");
	}

	@Test
	void getValueFromPayloadResolvesNestedPath() {
		Map<String, Object> payload = Map.of("user", Map.of("name", "Bob"));
		assertThat(VariableUtils.getValueFromPayload("user.name", payload)).isEqualTo("Bob");
	}

	@Test
	void getValueFromPayloadReturnsNullForMissingPath() {
		Map<String, Object> payload = Map.of("user", Map.of("name", "Bob"));
		assertThat(VariableUtils.getValueFromPayload("user.age", payload)).isNull();
		assertThat(VariableUtils.getValueFromPayload("missing.name", payload)).isNull();
	}

	@Test
	void getValueFromPayloadRejectsExpressionWithIllegalCharacters() {
		Map<String, Object> payload = Map.of("name", "kafka");
		assertThat(VariableUtils.getValueFromPayload("name || true", payload)).isNull();
		assertThat(VariableUtils.getValueFromPayload("${name}", payload)).isNull();
	}

	@Test
	void getValueFromPayloadReturnsNullOnBlankExpressionOrNullPayload() {
		assertThat(VariableUtils.getValueFromPayload(" ", Map.of("a", 1))).isNull();
		assertThat(VariableUtils.getValueFromPayload("a", null)).isNull();
	}

	@Test
	void getValueStringFromPayloadStringifiesScalars() {
		Map<String, Object> payload = Map.of("str", "text", "num", 42, "bool", true);
		assertThat(VariableUtils.getValueStringFromPayload("str", payload)).isEqualTo("text");
		assertThat(VariableUtils.getValueStringFromPayload("num", payload)).isEqualTo("42");
		assertThat(VariableUtils.getValueStringFromPayload("bool", payload)).isEqualTo("true");
	}

	@Test
	void getValueStringFromPayloadSerializesComplexValuesAsJson() {
		Map<String, Object> payload = Map.of("obj", Map.of("k", "v"));
		assertThat(VariableUtils.getValueStringFromPayload("obj", payload)).isEqualTo("{\"k\":\"v\"}");
	}

	@Test
	void getValueStringFromPayloadReturnsNullWhenValueMissing() {
		assertThat(VariableUtils.getValueStringFromPayload("missing", Map.of("a", 1))).isNull();
	}

	// ---------------------------------------------------------------------
	// setValueForPayload
	// ---------------------------------------------------------------------

	@Test
	void setValueForPayloadSetsTopLevelKey() {
		Map<String, Object> payload = new HashMap<>();
		assertThat(VariableUtils.setValueForPayload("name", payload, "kafka")).isTrue();
		assertThat(payload).containsEntry("name", "kafka");
	}

	@Test
	void setValueForPayloadSetsNestedKey() {
		Map<String, Object> user = new HashMap<>();
		Map<String, Object> payload = new HashMap<>();
		payload.put("user", user);
		assertThat(VariableUtils.setValueForPayload("user.name", payload, "Bob")).isTrue();
		assertThat(user).containsEntry("name", "Bob");
	}

	@Test
	void setValueForPayloadReturnsFalseOnInvalidInput() {
		assertThat(VariableUtils.setValueForPayload(" ", new HashMap<>(), "v")).isFalse();
		assertThat(VariableUtils.setValueForPayload("name", null, "v")).isFalse();
		assertThat(VariableUtils.setValueForPayload("a b", new HashMap<>(), "v")).isFalse();
	}

	// ---------------------------------------------------------------------
	// getValueFromContext / getValueStringFromContext
	// ---------------------------------------------------------------------

	@Test
	void getValueFromContextReturnsNullOnNullArguments() {
		assertThat(VariableUtils.getValueFromContext((CommonParam) null, new WorkflowContext())).isNull();
		assertThat(VariableUtils.getValueFromContext(new CommonParam(), null)).isNull();
	}

	@Test
	void getValueFromContextReturnsRawValueWhenValueFromIsInput() {
		CommonParam param = new CommonParam();
		param.setValueFrom("input");
		param.setValue("${not.resolved}");
		assertThat(VariableUtils.getValueFromContext(param, new WorkflowContext())).isEqualTo("${not.resolved}");
	}

	@Test
	void getValueFromContextResolvesDirectVariableReference() {
		WorkflowContext context = new WorkflowContext();
		context.getVariablesMap().put("greeting", "hi");

		CommonParam param = new CommonParam();
		param.setValueFrom("refer");
		param.setValue("${greeting}");
		assertThat(VariableUtils.getValueFromContext(param, context)).isEqualTo("hi");
	}

	@Test
	void getValueFromContextDefaultsToReferWhenValueFromBlank() {
		WorkflowContext context = new WorkflowContext();
		context.getVariablesMap().put("greeting", "hi");

		CommonParam param = new CommonParam();
		param.setValue("${greeting}");
		assertThat(VariableUtils.getValueFromContext(param, context)).isEqualTo("hi");
	}

	@Test
	void getValueFromContextFallsBackToPayloadExpression() {
		WorkflowContext context = new WorkflowContext();
		context.getVariablesMap().put("user", Map.of("name", "Bob"));

		Node.InputParam param = new Node.InputParam();
		param.setValueFrom("refer");
		param.setValue("${user.name}");
		assertThat(VariableUtils.getValueFromContext(param, context)).isEqualTo("Bob");
	}

	@Test
	void getValueFromContextReturnsNullWhenValueIsNull() {
		CommonParam param = new CommonParam();
		param.setValueFrom("refer");
		assertThat(VariableUtils.getValueFromContext(param, new WorkflowContext())).isNull();
	}

	@Test
	void getValueStringFromContextStringifiesResolvedValues() {
		WorkflowContext context = new WorkflowContext();
		context.getVariablesMap().put("count", 42);
		context.getVariablesMap().put("obj", Map.of("k", "v"));

		Node.InputParam countParam = new Node.InputParam();
		countParam.setValueFrom("refer");
		countParam.setValue("${count}");
		assertThat(VariableUtils.getValueStringFromContext(countParam, context)).isEqualTo("42");

		Node.InputParam objParam = new Node.InputParam();
		objParam.setValueFrom("refer");
		objParam.setValue("${obj}");
		assertThat(VariableUtils.getValueStringFromContext(objParam, context)).isEqualTo("{\"k\":\"v\"}");
	}

	// ---------------------------------------------------------------------
	// convertValueByType
	// ---------------------------------------------------------------------

	@Test
	void convertValueByTypeReturnsValueWhenTypeIsNull() {
		assertThat(VariableUtils.convertValueByType("key", null, "raw")).isEqualTo("raw");
	}

	@Test
	void convertValueByTypeReturnsNullWhenValueIsNull() {
		assertThat(VariableUtils.convertValueByType("key", "String", null)).isNull();
	}

	@Test
	void convertValueByTypeConvertsToString() {
		assertThat(VariableUtils.convertValueByType("key", "String", "text")).isEqualTo("text");
		assertThat(VariableUtils.convertValueByType("key", "String", 42)).isEqualTo("42");
		assertThat(VariableUtils.convertValueByType("key", "String", Map.of("k", "v"))).isEqualTo("{\"k\":\"v\"}");
	}

	@Test
	void convertValueByTypeConvertsToNumber() {
		assertThat(VariableUtils.convertValueByType("key", "Number", 7)).isEqualTo(7);
		assertThat(VariableUtils.convertValueByType("key", "Number", "42")).isEqualTo(42);
		assertThat(VariableUtils.convertValueByType("key", "Number", "3.14")).isEqualTo(3.14);
		assertThat(VariableUtils.convertValueByType("key", "Number", "3000000000")).isEqualTo(3000000000L);
	}

	@Test
	void convertValueByTypeConvertsToBoolean() {
		assertThat(VariableUtils.convertValueByType("key", "Boolean", Boolean.TRUE)).isEqualTo(true);
		assertThat(VariableUtils.convertValueByType("key", "Boolean", "true")).isEqualTo(true);
		assertThat(VariableUtils.convertValueByType("key", "Boolean", "FALSE")).isEqualTo(false);
	}

	@Test
	void convertValueByTypeConvertsToObject() {
		Map<String, Object> map = Map.of("a", 1);
		assertThat(VariableUtils.convertValueByType("key", "Object", map)).isSameAs(map);
		assertThat(VariableUtils.convertValueByType("key", "Object", "{\"a\":1}"))
			.isEqualTo(Map.of("a", 1));
	}

	@Test
	void convertValueByTypeConvertsToTypedArrays() {
		List<String> strings = List.of("a", "b");
		assertThat(VariableUtils.convertValueByType("key", "Array<String>", strings)).isSameAs(strings);
		assertThat(VariableUtils.convertValueByType("key", "Array<String>", "[\"a\",\"b\"]"))
			.isEqualTo(List.of("a", "b"));
		assertThat(VariableUtils.convertValueByType("key", "Array<Number>", "[1,2]")).isEqualTo(List.of(1, 2));
		assertThat(VariableUtils.convertValueByType("key", "Array<Boolean>", "[true,false]"))
			.isEqualTo(List.of(true, false));
	}

	@Test
	void convertValueByTypeThrowsBizExceptionOnUnconvertibleValue() {
		assertThatThrownBy(() -> VariableUtils.convertValueByType("key", "Number", "not-a-number"))
			.isInstanceOf(BizException.class);
		assertThatThrownBy(() -> VariableUtils.convertValueByType("key", "Boolean", "yes"))
			.isInstanceOf(BizException.class);
		assertThatThrownBy(() -> VariableUtils.convertValueByType("key", "Object", 42))
			.isInstanceOf(BizException.class);
	}

	// ---------------------------------------------------------------------
	// identifyVariableListFromText / identifyVariableSetFromText
	// ---------------------------------------------------------------------

	@Test
	void identifyVariableListFromTextKeepsOrderAndDuplicates() {
		String content = "Hi ${name}, meet ${user.name}. Bye ${name}!";
		assertThat(VariableUtils.identifyVariableListFromText(content))
			.containsExactly("name", "user.name", "name");
	}

	@Test
	void identifyVariableListFromTextReturnsEmptyListWhenNoVariables() {
		assertThat(VariableUtils.identifyVariableListFromText("plain text")).isEmpty();
		assertThat(VariableUtils.identifyVariableListFromText(" ")).isEmpty();
	}

	@Test
	void identifyVariableSetFromTextDeduplicatesVariables() {
		String content = "${name} and ${name} and ${other}";
		Set<String> variables = VariableUtils.identifyVariableSetFromText(content);
		assertThat(variables).containsExactlyInAnyOrder("name", "other");
	}

}
