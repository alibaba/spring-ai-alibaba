/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AnswerNode}.
 */
public class AnswerNodeTest {

	private static OverAllState stateOf(Map<String, Object> entries) {
		OverAllState state = new OverAllState();
		entries.forEach((k, v) -> {
			state.registerKeyAndStrategy(k, new ReplaceStrategy());
			state.updateState(Map.of(k, v));
		});
		return state;
	}

	@Test
	public void testSinglePlaceholderReplacement() {
		OverAllState state = stateOf(Map.of("name", "Alice"));

		AnswerNode node = AnswerNode.builder().answer("Hello, {name}!").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Hello, Alice!", result.get("answer"));
	}

	@Test
	public void testMultiplePlaceholders() {
		OverAllState state = stateOf(Map.of("city", "Beijing", "weather", "sunny"));

		AnswerNode node = AnswerNode.builder().answer("Today in {city} it is {weather}.").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Today in Beijing it is sunny.", result.get("answer"));
	}

	@Test
	public void testPlaceholderWithSpaces() {
		OverAllState state = stateOf(Map.of("topic", "Spring AI"));

		AnswerNode node = AnswerNode.builder().answer("Learning { topic } today.").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Learning Spring AI today.", result.get("answer"));
	}

	@Test
	public void testMissingKeyDefaultsToEmpty() {
		OverAllState state = new OverAllState();

		AnswerNode node = AnswerNode.builder().answer("Value is {missing}.").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Value is .", result.get("answer"));
	}

	@Test
	public void testNoPlaceholdersReturnStaticText() {
		OverAllState state = new OverAllState();

		AnswerNode node = AnswerNode.builder().answer("No placeholders here.").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("No placeholders here.", result.get("answer"));
	}

	@Test
	public void testCustomOutputKey() {
		OverAllState state = stateOf(Map.of("item", "coffee"));

		AnswerNode node = AnswerNode.builder().answer("I like {item}.").outputKey("response").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("I like coffee.", result.get("response"));
	}

	@Test
	public void testValueContainingDollarSign() {
		OverAllState state = stateOf(Map.of("price", "$100"));

		AnswerNode node = AnswerNode.builder().answer("Price: {price}").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Price: $100", result.get("answer"));
	}

	@Test
	public void testValueContainingBackslash() {
		OverAllState state = stateOf(Map.of("path", "C:\\Users\\test"));

		AnswerNode node = AnswerNode.builder().answer("Path: {path}").build();

		Map<String, Object> result = node.apply(state);
		assertEquals("Path: C:\\Users\\test", result.get("answer"));
	}

}
