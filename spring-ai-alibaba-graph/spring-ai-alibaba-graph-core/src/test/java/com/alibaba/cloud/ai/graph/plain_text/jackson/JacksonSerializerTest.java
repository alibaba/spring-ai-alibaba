/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.graph.plain_text.jackson;

import java.io.IOException;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JacksonSerializerTest {

	@Test
	public void serializeWithTypeInferenceTest() throws IOException, ClassNotFoundException {

		OverAllState state = new OverAllState(mapOf("prop1", "value1"));

		JacksonStateSerializer serializer = new JacksonStateSerializer(OverAllState::new) {
		};

		Class<?> type = serializer.getStateType();

		assertEquals(OverAllState.class, type);

		byte[] bytes = serializer.writeObject(state);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		OverAllState deserializedState = serializer.readObject(bytes);

		assertNotNull(deserializedState);
		assertEquals(1, deserializedState.data().size());
		assertEquals("value1", deserializedState.data().get("prop1"));
	}

}
