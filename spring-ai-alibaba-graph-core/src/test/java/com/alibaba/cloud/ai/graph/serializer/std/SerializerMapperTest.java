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
package com.alibaba.cloud.ai.graph.serializer.std;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializerMapperTest {

	@Test
	void unregistersAllBindingsForTheSerializerImplementation() {
		var mapper = new SerializerMapper();
		mapper.register(List.class, new ObjectStreamStateSerializer.ListSerializer());
		mapper.register(Collection.class, new ObjectStreamStateSerializer.ListSerializer());
		mapper.register(Map.class, new ObjectStreamStateSerializer.MapSerializer());

		assertTrue(mapper.unregister(ObjectStreamStateSerializer.ListSerializer.class));
		assertFalse(mapper.getSerializer(List.class).isPresent());
		assertFalse(mapper.getSerializer(Collection.class.getName()).isPresent());
		assertTrue(mapper.getSerializer(Map.class).isPresent());
		assertFalse(mapper.unregister(ObjectStreamStateSerializer.ListSerializer.class));
	}

	@Test
	void rejectsNullSerializer() {
		var mapper = new SerializerMapper();
		assertThrows(NullPointerException.class, () -> mapper.register(String.class, null));
	}

}
