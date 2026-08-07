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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Regression test for {@link SerializationUtils#deepCopyValue(Object)} to verify that
 * primitive arrays are deep copied correctly.
 * <p>
 * Previously the array branch cast every array to {@code Object[]}, which threw
 * {@code ClassCastException} for primitive arrays (e.g. {@code float[]} embeddings or
 * {@code byte[]} multimodal data) passed as graph inputs to
 * {@code CompiledGraph.invoke(...)}. It also silently narrowed reference arrays such as
 * {@code String[]} into {@code Object[]}. Both cases are covered below.
 * </p>
 *
 */
public class SerializationUtilsPrimitiveArrayTest {

    @Test
    void deepCopyIntArray() {
        int[] original = { 1, 2, 3 };
        Object copied = SerializationUtils.deepCopyValue(original);
        assertInstanceOf(int[].class, copied);
        assertArrayEquals(original, (int[]) copied);
        assertNotSame(original, copied);
    }

    @Test
    void deepCopyFloatArray() {
        float[] original = { 0.1f, 0.2f, 0.3f };
        Object copied = SerializationUtils.deepCopyValue(original);
        assertInstanceOf(float[].class, copied);
        assertArrayEquals(original, (float[]) copied, 1e-9f);
        assertNotSame(original, copied);
    }

    @Test
    void deepCopyDoubleArray() {
        double[] original = { 1.5, 2.5, 3.5 };
        Object copied = SerializationUtils.deepCopyValue(original);
        assertInstanceOf(double[].class, copied);
        assertArrayEquals(original, (double[]) copied, 1e-9);
    }

    @Test
    void deepCopyByteArray() {
        byte[] original = { 1, 2, 3, 4 };
        Object copied = SerializationUtils.deepCopyValue(original);
        assertInstanceOf(byte[].class, copied);
        assertArrayEquals(original, (byte[]) copied);
    }

    @Test
    void deepCopyLongAndShortAndCharAndBooleanArrays() {
        assertArrayEquals(new long[] { 1L, 2L }, (long[]) SerializationUtils.deepCopyValue(new long[] { 1L, 2L }));
        assertArrayEquals(new short[] { 1, 2 }, (short[]) SerializationUtils.deepCopyValue(new short[] { 1, 2 }));
        assertArrayEquals(new char[] { 'a', 'b' }, (char[]) SerializationUtils.deepCopyValue(new char[] { 'a', 'b' }));
        assertArrayEquals(new boolean[] { true, false },
                (boolean[]) SerializationUtils.deepCopyValue(new boolean[] { true, false }));
    }

    @Test
    void deepCopyEmptyPrimitiveArray() {
        int[] original = {};
        Object copied = SerializationUtils.deepCopyValue(original);
        assertInstanceOf(int[].class, copied);
        assertEquals(0, ((int[]) copied).length);
    }

    @Test
    void mutatingCopyDoesNotAffectOriginal() {
        int[] original = { 1, 2, 3 };
        int[] copied = (int[]) SerializationUtils.deepCopyValue(original);
        copied[0] = 999;
        assertEquals(1, original[0]);
    }

    @Test
    void objectArrayPreservesRuntimeComponentType() {
        String[] original = { "a", "b" };
        Object copied = SerializationUtils.deepCopyValue(original);
        // Must stay String[], not be narrowed to Object[].
        assertInstanceOf(String[].class, copied);
        assertArrayEquals(original, (String[]) copied);
    }

    @Test
    void objectArrayElementsAreDeepCopied() {
        Map<String, Object> element = new HashMap<>();
        element.put("k", "v");
        Map<String, Object>[] original = new Map[] { element };

        @SuppressWarnings("unchecked")
        Map<String, Object>[] copied = (Map<String, Object>[]) SerializationUtils.deepCopyValue(original);

        assertNotSame(original[0], copied[0]);
        copied[0].put("k", "changed");
        assertEquals("v", original[0].get("k"));
    }

    @Test
    void deepCopyMapContainingPrimitiveArrayValue() {
        Map<String, Object> data = new HashMap<>();
        data.put("embedding", new float[] { 0.1f, 0.2f, 0.3f });

        Map<String, Object> copied = SerializationUtils.deepCopyMap(data);

        assertInstanceOf(float[].class, copied.get("embedding"));
        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, (float[]) copied.get("embedding"), 1e-9f);
    }
}
