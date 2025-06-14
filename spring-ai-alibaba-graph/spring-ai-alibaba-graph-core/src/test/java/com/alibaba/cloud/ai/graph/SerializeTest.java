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
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.NullableObjectSerializer;
import com.alibaba.cloud.ai.graph.serializer.ObjectStreamStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.AgentState;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	// ObjectStreamStateSerializer instance for serializing and deserializing AgentState
	// objects
	private final ObjectStreamStateSerializer<AgentState> stateSerializer = new ObjectStreamStateSerializer<>(
			AgentState::new);

	// Serializes an AgentState object into a byte array
	private byte[] serializeState(AgentState state) throws Exception {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ObjectOutputStream oas = new ObjectOutputStream(stream);
			stateSerializer.write(state, oas);
			oas.flush();
			return stream.toByteArray();
		}
	}

	// Deserializes a byte array back into an AgentState object
	private AgentState deserializeState(byte[] bytes) throws Exception {
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			ObjectInputStream ois = new ObjectInputStream(stream);
			return stateSerializer.read(ois);
		}
	}

	// Test class containing a nullable field for serialization tests
	static class ValueWithNull {

		private final String name;

		public ValueWithNull(String name) {
			this.name = name;
		}

	}

	// Test case to verify serialization of a complete AgentState with various data types
	// including null values
	@Test
	@SuppressWarnings("unchecked")
	public void serializeStateTest() throws Exception {

		// Register custom serializer for ValueWithNull class to handle nullable fields
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

		// Create an AgentState with mixed data types including nulls and custom objects
		AgentState state = stateSerializer.stateOf(mapOf("a", "b", "f", null, "c", 100, "e", new ValueWithNull(null),
				"list", listOf("aa", null, "cc", 200)));

		// Perform serialization
		byte[] bytes = serializeState(state);

		// Validate serialization output
		assertNotNull(bytes);
		Map<String, Object> deserializeState = deserializeState(bytes).data();

		// Assert expected results after deserialization
		assertEquals(5, deserializeState.size());
		assertEquals("b", deserializeState.get("a"));
		assertEquals(100, deserializeState.get("c"));
		assertNull(deserializeState.get("f"));

		// Verify the custom object and its null field are correctly preserved
		assertInstanceOf(ValueWithNull.class, deserializeState.get("e"));
		assertNull(((ValueWithNull) deserializeState.get("e")).name);

		// Check list structure and contents including null entries
		assertInstanceOf(List.class, deserializeState.get("list"));
		List<String> list = (List<String>) deserializeState.get("list");
		assertEquals(4, list.size());
		assertEquals("aa", list.get(0));
		assertNull(list.get(1));
		assertEquals("cc", list.get(2));
		assertEquals(200, list.get(3));

	}

	// Non-serializable test class to simulate unsupported types during serialization
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

	// Test to ensure that non-serializable elements cause appropriate exceptions
	@Test
	public void partiallySerializeStateTest() throws Exception {

		// Create AgentState with a non-serializable element
		AgentState state = stateSerializer
			.stateOf(mapOf("a", "b", "f", new NonSerializableElement("I'M NOT SERIALIZABLE"), "c", "d"));

		// Expect NotSerializableException when attempting to serialize
		assertThrows(NotSerializableException.class, () -> {
			serializeState(state);
		});

	}

	// Test custom serialization support for previously non-serializable types
	@Test
	public void customSerializeStateTest() throws Exception {

		// Register custom serializer for NonSerializableElement
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

		// Create AgentState with custom serialized type included
		AgentState state = stateSerializer
			.stateOf(mapOf("a", "b", "x", new NonSerializableElement("I'M NOT SERIALIZABLE"), "f", "H", "c", "d"));

		System.out.println(state);

		// Perform and validate serialization round-trip
		byte[] bytes = serializeState(state);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		Map<String, Object> deserializedData = deserializeState(bytes).data();

		assertNotNull(deserializedData);

		System.out.println(deserializedData.get("x").getClass());
		System.out.println(deserializedData);
	}

	// Jackson-based StateSerializer for testing JSON serialization capabilities
	static class JacksonSerializer extends JacksonStateSerializer {

		public JacksonSerializer() {
			super(OverAllState::new);
		}

		ObjectMapper getObjectMapper() {
			return objectMapper;
		}

	}

	// Test NodeOutput serialization using Jackson JSON library
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

}
