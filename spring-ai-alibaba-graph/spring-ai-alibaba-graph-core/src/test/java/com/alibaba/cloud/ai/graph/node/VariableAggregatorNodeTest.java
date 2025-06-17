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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableAggregatorNodeTest {

	private OverAllState mockState;

	@BeforeEach
	public void setUp() {
		mockState = mock(OverAllState.class);
	}

	// Mock state
	private Object getNestedValue(Map<String, Object> root, List<String> path) {
		Object current = root;
		for (String key : path) {
			if (current instanceof Map) {
				current = ((Map<?, ?>) current).get(key);
			}
			else {
				return null;
			}
		}
		return current;
	}

	@Test
	public void testApply_withListOutputType() throws Exception {
		List<List<String>> variables = Arrays.asList(Collections.singletonList("user"),
				Collections.singletonList("age"));

		when(mockState.value("user")).thenReturn(Optional.of("ricky"));
		when(mockState.value("age")).thenReturn(Optional.of(20));

		VariableAggregatorNode node = new VariableAggregatorNode(variables, "result", "list", null);

		Map<String, Object> result = node.apply(mockState);

		assertEquals(Arrays.asList("ricky", 20), result.get("result"));
	}

	@Test
	public void testApply_withStringOutputType() throws Exception {
		List<List<String>> variables = Arrays.asList(Collections.singletonList("product"),
				Collections.singletonList("price"));

		when(mockState.value("product")).thenReturn(Optional.of("Book"));
		when(mockState.value("price")).thenReturn(Optional.of("19.9"));

		VariableAggregatorNode node = new VariableAggregatorNode(variables, "result", "string", null);
		Map<String, Object> result = node.apply(mockState);

		assertEquals("Book\n19.9", result.get("result"));
	}

	@Test
	public void testApply_withGroupedOutput() throws Exception {
		VariableAggregatorNode.Group group1 = new VariableAggregatorNode.Group();
		group1.setGroupName("UserInfo");
		group1.setOutputType("list");
		group1.setVariables(Arrays.asList(Arrays.asList("user", "name"), Arrays.asList("user", "age")));

		VariableAggregatorNode.Group group2 = new VariableAggregatorNode.Group();
		group2.setGroupName("ProductInfo");
		group2.setOutputType("string");
		group2.setVariables(Collections.singletonList(Collections.singletonList("product")));

		VariableAggregatorNode.AdvancedSettings settings = new VariableAggregatorNode.AdvancedSettings();
		settings.setGroupEnabled(true);
		settings.setGroups(Arrays.asList(group1, group2));

		List<List<String>> variables = Arrays.asList(Arrays.asList("user", "name"), Arrays.asList("user", "age"),
				Collections.singletonList("product"));

		Map<String, Object> userMap = new HashMap<>();
		userMap.put("name", "Alice");
		userMap.put("age", 30);

		when(mockState.value("user")).thenReturn(Optional.of(userMap));
		when(mockState.value("product")).thenReturn(Optional.of("Laptop"));

		VariableAggregatorNode node = new VariableAggregatorNode(variables, "aggregatedData", "group", settings);
		Map<String, Object> result = node.apply(mockState);

		Map<String, Object> aggregated = (Map<String, Object>) result.get("aggregatedData");
		assertEquals(Arrays.asList("Alice", 30), aggregated.get("UserInfo"));
		assertEquals("Laptop", aggregated.get("ProductInfo"));
	}

	@Test
	public void testApply_withMissingKey() throws Exception {
		List<List<String>> variables = Collections.singletonList(Collections.singletonList("missing"));

		when(mockState.value("missing")).thenReturn(Optional.empty());

		VariableAggregatorNode node = new VariableAggregatorNode(variables, "result", "list", null);
		Map<String, Object> result = node.apply(mockState);

		assertEquals(Collections.emptyList(), result.get("result"));
	}

}
