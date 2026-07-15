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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.utils.SerializationUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link SerializationUtils#deepCopyValue(Object)} to verify that array values,
 * including primitive arrays, are deep copied without throwing a {@link ClassCastException}.
 * <p>
 * This is a regression test for issue #4812 where primitive arrays (e.g. {@code float[]}
 * embeddings or {@code byte[]} audio) passed as graph input caused a
 * {@code ClassCastException} because every array was cast to {@code Object[]}.
 * </p>
 *
 * @author Spring AI Alibaba
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4812">Issue #4812</a>
 */
public class SerializationUtilsArrayTest {

	@Test
	public void testDeepCopyFloatArray() {
		float[] original = { 1.0f, 2.5f, -3.75f };

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(float[].class, copied, "float[] should stay a float[] after deep copy");
		assertNotSame(original, copied, "float[] should be copied, not reused");
		assertArrayEquals(original, (float[]) copied);
	}

	@Test
	public void testDeepCopyByteArray() {
		byte[] original = { 0, 1, 2, (byte) 0xFF };

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(byte[].class, copied);
		assertNotSame(original, copied);
		assertArrayEquals(original, (byte[]) copied);
	}

	@Test
	public void testDeepCopyIntArray() {
		int[] original = { 10, 20, 30 };

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(int[].class, copied);
		assertNotSame(original, copied);
		assertArrayEquals(original, (int[]) copied);
	}

	@Test
	public void testDeepCopyObjectArrayPreservesComponentType() {
		Integer[] original = { 1, 2, 3 };

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(Integer[].class, copied, "Component type should be preserved instead of widened to Object[]");
		assertNotSame(original, copied);
		assertArrayEquals(original, (Integer[]) copied);
	}

	@Test
	public void testDeepCopyMapWithPrimitiveArrayValue() {
		Map<String, Object> original = new HashMap<>();
		original.put("embedding", new float[] { 0.1f, 0.2f, 0.3f });

		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		Object copiedEmbedding = copied.get("embedding");
		assertInstanceOf(float[].class, copiedEmbedding);
		assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, (float[]) copiedEmbedding);
	}

	@Test
	public void testGraphInvokeWithPrimitiveArrayInput() throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("embedding", (o, n) -> n);
			strategies.put("result", (o, n) -> n);
			return strategies;
		};

		CompiledGraph graph = new StateGraph(keyStrategyFactory)
			.addNode("echo", node_async(state -> Map.of("result", "ok")))
			.addEdge(START, "echo")
			.addEdge("echo", END)
			.compile();

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("embedding", new float[] { 1.0f, 2.0f, 3.0f });

		OverAllState finalState = graph.invoke(inputs)
			.orElseThrow(() -> new AssertionError("Graph should produce a final state"));

		assertEquals("ok", finalState.value("result").orElse(null));
		Object embedding = finalState.value("embedding").orElse(null);
		assertInstanceOf(float[].class, embedding);
		assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, (float[]) embedding);
	}

}
