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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/7/23 10:52
 */
public class AssignerNodeTest {

	@Test
	public void testBatchOverWrite() {
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("a", new ReplaceStrategy());
		state.registerKeyAndStrategy("b", new ReplaceStrategy());
		state.registerKeyAndStrategy("c", new ReplaceStrategy());
		state.updateState(Map.of("a", "hello", "b", "world", "c", 123));

		AssignerNode node = AssignerNode.builder()
			.addItem("x", "a", AssignerNode.WriteMode.OVER_WRITE)
			.addItem("y", "b", AssignerNode.WriteMode.OVER_WRITE)
			.addItem("z", "c", AssignerNode.WriteMode.OVER_WRITE)
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals("hello", result.get("x"));
		assertEquals("world", result.get("y"));
		assertEquals(123, result.get("z"));
	}

	@Test
	public void testBatchAppend() {
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("a", new ReplaceStrategy());
		state.registerKeyAndStrategy("b", new ReplaceStrategy());
		state.registerKeyAndStrategy("x", new ReplaceStrategy());
		state.registerKeyAndStrategy("y", new ReplaceStrategy());
		state.updateState(
				Map.of("a", "foo", "b", "bar", "x", new ArrayList<>(List.of("hello")), "y", new ArrayList<>()));

		AssignerNode node = AssignerNode.builder()
			.addItem("x", "a", AssignerNode.WriteMode.APPEND)
			.addItem("y", "b", AssignerNode.WriteMode.APPEND)
			.build();

		Map<String, Object> result = node.apply(state);
		List<?> xList = (List<?>) result.get("x");
		List<?> yList = (List<?>) result.get("y");

		assertEquals(List.of("hello", "foo"), xList);
		assertEquals(List.of("bar"), yList);
	}

	@Test
	public void testBatchClear() {
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("x", new ReplaceStrategy());
		state.registerKeyAndStrategy("y", new ReplaceStrategy());
		state.registerKeyAndStrategy("z", new ReplaceStrategy());
		state.updateState(Map.of("x", "something", "y", new ArrayList<>(List.of(1, 2, 3)), "z", 42));

		AssignerNode node = AssignerNode.builder()
			.addItem("x", null, AssignerNode.WriteMode.CLEAR)
			.addItem("y", null, AssignerNode.WriteMode.CLEAR)
			.addItem("z", null, AssignerNode.WriteMode.CLEAR)
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals("", result.get("x"));
		assertTrue(((List<?>) result.get("y")).isEmpty());
		assertEquals(0, result.get("z"));
	}

	@Test
	public void testMixBatch() {
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("a", new ReplaceStrategy());
		state.registerKeyAndStrategy("b", new ReplaceStrategy());
		state.registerKeyAndStrategy("c", new ReplaceStrategy());
		state.registerKeyAndStrategy("input1", new ReplaceStrategy());
		state.registerKeyAndStrategy("input2", new ReplaceStrategy());
		state.registerKeyAndStrategy("input3", new ReplaceStrategy());
		state.updateState(Map.of("input1", "A", "input2", "B", "input3", "C", "a", new ArrayList<>(List.of("a0")), "b",
				"to be cleared", "c", 999));

		AssignerNode node = AssignerNode.builder()
			.addItem("a", "input1", AssignerNode.WriteMode.APPEND)
			.addItem("b", null, AssignerNode.WriteMode.CLEAR)
			.addItem("c", "input3", AssignerNode.WriteMode.OVER_WRITE)
			.build();

		Map<String, Object> result = node.apply(state);
		assertEquals(List.of("a0", "A"), result.get("a"));
		assertEquals("", result.get("b"));
		assertEquals("C", result.get("c"));
	}

	@Test
	public void testCompatibleSingleVariable() {
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("input", new ReplaceStrategy());
		state.updateState(Map.of("input", "single"));

		AssignerNode node = new AssignerNode("output", "input", AssignerNode.WriteMode.OVER_WRITE);

		Map<String, Object> result = node.apply(state);
		assertEquals("single", result.get("output"));
	}

}
