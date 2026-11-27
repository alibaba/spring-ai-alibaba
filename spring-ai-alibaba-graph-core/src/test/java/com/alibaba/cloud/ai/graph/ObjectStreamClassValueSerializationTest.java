/*
 * Copyright 2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStreamClassValueSerializationTest {

	static Stream<Class<?>> classValues() {
		return Stream.<Class<?>>of(String.class, int.class, void.class, String[].class, int[][].class, Runnable.class,
				Map.Entry.class, Class.class, Box.class);
	}

	@ParameterizedTest
	@MethodSource("classValues")
	void preservesClassAndFollowingValue(Class<?> type) throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("type", type);
		data.put("following", "value");
		assertEquals(data, serializer.dataFromBytes(serializer.dataToBytes(data)));
	}

	@Test
	void preservesNestedClassValues() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		Map<String, Object> data = Map.of("nested",
				List.of(String.class, Map.of("type", int.class), String.class, "following"));
		assertEquals(data, serializer.dataFromBytes(serializer.dataToBytes(data)));
	}

	@Test
	void distinguishesClassValueFromRegisteredObjectTag() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		serializer.mapper().register(Box.class, new Serializer<Box>() {
			@Override
			public void write(Box value, ObjectOutput out) throws IOException {
				out.writeUTF(value.value());
			}

			@Override
			public Box read(ObjectInput in) throws IOException {
				return new Box(in.readUTF());
			}
		});
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("type", Box.class);
		data.put("object", new Box("custom"));
		data.put("following", "value");
		assertEquals(data, serializer.dataFromBytes(serializer.dataToBytes(data)));
	}

	@Test
	void honorsExplicitClassSerializer() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		AtomicInteger writes = new AtomicInteger();
		AtomicInteger reads = new AtomicInteger();
		serializer.mapper().register(Class.class, new Serializer<Class<?>>() {
			@Override
			public void write(Class<?> value, ObjectOutput out) throws IOException {
				writes.incrementAndGet();
				out.writeUTF(value.getName());
			}

			@Override
			public Class<?> read(ObjectInput in) throws IOException, ClassNotFoundException {
				reads.incrementAndGet();
				return Class.forName(in.readUTF(), false, getClass().getClassLoader());
			}
		});
		Map<String, Object> data = Map.of("type", String.class);
		try (var input = getClass().getResourceAsStream("/serialization/custom-class-state.bin")) {
			byte[] legacy = input.readAllBytes();
			assertEquals(data, serializer.dataFromBytes(legacy));
			assertArrayEquals(legacy, serializer.dataToBytes(data));
		}
		assertEquals(1, writes.get());
		assertEquals(1, reads.get());
	}

	@Test
	void keepsLegacyOrdinaryEncodingAndNullValues() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("text", "legacy");
		data.put("number", 42);
		data.put("nullable", null);
		try (var input = getClass().getResourceAsStream("/serialization/ordinary-state.bin")) {
			byte[] legacy = input.readAllBytes();
			assertEquals(data, serializer.dataFromBytes(legacy));
			assertArrayEquals(legacy, serializer.dataToBytes(data));
		}
	}

	@Test
	void preservesOuterDeserializationFilter() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		byte[] bytes = serializer.dataToBytes(Map.of("type", Box.class));
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			input.setObjectInputFilter(info -> info.serialClass() == Box.class
					? java.io.ObjectInputFilter.Status.REJECTED : java.io.ObjectInputFilter.Status.UNDECIDED);
			assertThrows(java.io.InvalidClassException.class, () -> serializer.readData(input));
		}
	}

	@Test
	void rejectsTruncatedClassValueWithoutSwallowingTheError() throws Exception {
		ObjectStreamStateSerializer serializer = serializer();
		byte[] bytes = serializer.dataToBytes(Map.of("type", String.class));
		assertThrows(IOException.class, () -> serializer.dataFromBytes(Arrays.copyOf(bytes, bytes.length / 2)));
	}

	private static ObjectStreamStateSerializer serializer() {
		return new ObjectStreamStateSerializer(OverAllState::new);
	}

	private record Box(String value) {
	}

}
