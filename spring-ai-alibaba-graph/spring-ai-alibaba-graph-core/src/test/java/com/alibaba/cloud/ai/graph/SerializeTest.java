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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.gson.GsonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.listOf;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializeTest {

	private final ObjectStreamStateSerializer<AgentState> stateSerializer = new ObjectStreamStateSerializer<>(
			AgentState::new);

	private byte[] serializeState(AgentState state) throws Exception {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ObjectOutputStream oas = new ObjectOutputStream(stream);
			stateSerializer.write(state, oas);
			oas.flush();
			return stream.toByteArray();
		}
	}

	private AgentState deserializeState(byte[] bytes) throws Exception {
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			ObjectInputStream ois = new ObjectInputStream(stream);
			return stateSerializer.read(ois);
		}
	}

	static class ValueWithNull {

		private final String name;

		public ValueWithNull(String name) {
			this.name = name;
		}

	}

	@Test
	@SuppressWarnings("unchecked")
	public void serializeStateTest() throws Exception {

		stateSerializer.mapper().register(ValueWithNull.class, new NullableObjectSerializer<ValueWithNull>() {

			@Override
			public void write(ValueWithNull object, ObjectOutput out) throws IOException {
				writeNullableUTF(object.name, out);
			}

			@Override
			public ValueWithNull read(ObjectInput in) throws IOException, ClassNotFoundException {
				return new ValueWithNull(readNullableUTF(in).orElse(null));
			}
		});
		AgentState state = stateSerializer.stateOf(
				mapOf("a", "b", "f", null, "c", 100, "e", new ValueWithNull(null), "list", listOf("aa", null, "cc", 200)

				));

		byte[] bytes = serializeState(state);

		assertNotNull(bytes);
		Map<String, Object> deserializeState = deserializeState(bytes).data();

		assertEquals(5, deserializeState.size());
		assertEquals("b", deserializeState.get("a"));
		assertEquals(100, deserializeState.get("c"));
		assertNull(deserializeState.get("f"));
		assertInstanceOf(ValueWithNull.class, deserializeState.get("e"));
		assertNull(((ValueWithNull) deserializeState.get("e")).name);
		assertInstanceOf(List.class, deserializeState.get("list"));
		List<String> list = (List<String>) deserializeState.get("list");
		assertEquals(4, list.size());
		assertEquals("aa", list.get(0));
		assertNull(list.get(1));
		assertEquals("cc", list.get(2));
		assertEquals(200, list.get(3));

	}

	public static class NonSerializableElement {

		String value;

		public NonSerializableElement() {
			this.value = "default";
		}

		public NonSerializableElement(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "NonSerializableElement{" + "value='" + value + '\'' + '}';
		}

	}

	@Test
	public void partiallySerializeStateTest() throws Exception {

		AgentState state = stateSerializer
			.stateOf(mapOf("a", "b", "f", new NonSerializableElement("I'M NOT SERIALIZABLE"), "c", "d"));

		assertThrows(NotSerializableException.class, () -> {
			serializeState(state);
		});

	}

	@Test
	public void customSerializeStateTest() throws Exception {

		stateSerializer.mapper().register(NonSerializableElement.class, new Serializer<NonSerializableElement>() {

			@Override
			public void write(NonSerializableElement object, ObjectOutput out) throws IOException {
				out.writeUTF(object.value);
			}

			@Override
			public NonSerializableElement read(ObjectInput in) throws IOException, ClassNotFoundException {
				return new NonSerializableElement(in.readUTF());
			}
		});

		AgentState state = stateSerializer
			.stateOf(mapOf("a", "b", "x", new NonSerializableElement("I'M NOT SERIALIZABLE"), "f", "H", "c", "d"));

		System.out.println(state);

		byte[] bytes = serializeState(state);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		Map<String, Object> deserializedData = deserializeState(bytes).data();

		assertNotNull(deserializedData);

		System.out.println(deserializedData.get("x").getClass());
		System.out.println(deserializedData);
	}

	static class JacksonSerializer extends JacksonStateSerializer {

		public JacksonSerializer() {
			super(OverAllState::new);
		}

		ObjectMapper getObjectMapper() {
			return objectMapper;
		}

	}

	@Test
	public void NodOutputJacksonSerializationTest() throws Exception {

		JacksonSerializer serializer = new JacksonSerializer();

		NodeOutput output = NodeOutput.of("node", null);
		output.setSubGraph(true);
		String json = serializer.getObjectMapper().writeValueAsString(output);

		assertEquals("{\"node\":\"node\",\"state\":null,\"subGraph\":true}", json);

		output.setSubGraph(false);
		json = serializer.getObjectMapper().writeValueAsString(output);

		assertEquals("{\"node\":\"node\",\"state\":null,\"subGraph\":false}", json);
	}

	static class GsonSerializer extends GsonStateSerializer {

		public GsonSerializer() {
			super(OverAllState::new, new GsonBuilder().serializeNulls().create());
		}

		Gson getGson() {
			return gson;
		}

	}

	@Test
	public void NodOutputJGsonSerializationTest() throws Exception {

		GsonSerializer serializer = new GsonSerializer();

		NodeOutput output = NodeOutput.of("node", null);
		output.setSubGraph(true);
		String json = serializer.getGson().toJson(output);

		assertEquals("{\"node\":\"node\",\"state\":null,\"subGraph\":true}", json);

		output.setSubGraph(false);
		json = serializer.getGson().toJson(output);

		assertEquals("{\"node\":\"node\",\"state\":null,\"subGraph\":false}", json);
	}

}
