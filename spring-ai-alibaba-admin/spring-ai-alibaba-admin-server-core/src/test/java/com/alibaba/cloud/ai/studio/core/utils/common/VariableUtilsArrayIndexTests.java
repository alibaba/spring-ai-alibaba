/*
 * Copyright 2025-2026 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VariableUtils} 数组下标表达式的测试。
 *
 * <p>OGNL 原生支持 {@code list[0]} 下标访问，因此 {@code node.result.list[0].name}
 * 这类下标表达式应正常从 payload 中解析出值，而不是静默返回 null。
 */
class VariableUtilsArrayIndexTests {

	private Map<String, Object> buildPayload() {
		Map<String, Object> firstItem = new HashMap<>();
		firstItem.put("name", "first-item");
		firstItem.put("tags", List.of("x", "y"));

		Map<String, Object> secondItem = new HashMap<>();
		secondItem.put("name", "second-item");
		secondItem.put("tags", List.of("z"));

		Map<String, Object> result = new HashMap<>();
		result.put("list", List.of(firstItem, secondItem));
		result.put("strList", List.of("first-item", "second-item"));

		Map<String, Object> payload = new HashMap<>();
		payload.put("result", result);
		return payload;
	}

	// ---------------------------------------------------------------------
	// getValueFromPayload
	// ---------------------------------------------------------------------

	@Test
	void getValueFromPayloadResolvesStringListIndex() {
		assertThat(VariableUtils.getValueFromPayload("result.strList[0]", buildPayload())).isEqualTo("first-item");
	}

	@Test
	void getValueFromPayloadResolvesObjectListIndex() {
		assertThat(VariableUtils.getValueFromPayload("result.list[0].name", buildPayload())).isEqualTo("first-item");
	}

	@Test
	void getValueFromPayloadResolvesNestedIndex() {
		assertThat(VariableUtils.getValueFromPayload("result.list[0].tags[1]", buildPayload())).isEqualTo("y");
	}

	@Test
	void getValueFromPayloadReturnsWholeListWhenNoIndex() {
		List<?> list = (List<?>) VariableUtils.getValueFromPayload("result.list", buildPayload());
		assertThat(list).hasSize(2);
	}

	@Test
	void getValueFromPayloadReturnsNullForOutOfRangeIndex() {
		assertThat(VariableUtils.getValueFromPayload("result.strList[9]", buildPayload())).isNull();
	}

	@Test
	void getValueFromPayloadResolvesIntegerKeyedMapIndex() {
		Map<Object, Object> map = new HashMap<>();
		map.put(0, "zero");

		Map<String, Object> payload = new HashMap<>();
		payload.put("intKeyMap", map);

		assertThat(VariableUtils.getValueFromPayload("intKeyMap[0]", payload)).isEqualTo("zero");
	}

	// ---------------------------------------------------------------------
	// setValueForPayload
	// ---------------------------------------------------------------------

	@Test
	void setValueForPayloadWritesListElement() {
		List<String> strList = new ArrayList<>(List.of("first-item", "second-item"));
		Map<String, Object> result = new HashMap<>();
		result.put("strList", strList);

		Map<String, Object> payload = new HashMap<>();
		payload.put("result", result);

		assertThat(VariableUtils.setValueForPayload("result.strList[0]", payload, "updated")).isTrue();
		assertThat(strList).containsExactly("updated", "second-item");
	}

}
