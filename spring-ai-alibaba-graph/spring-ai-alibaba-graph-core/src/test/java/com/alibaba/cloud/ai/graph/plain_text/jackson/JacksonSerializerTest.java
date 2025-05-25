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

	static class State extends AgentState {

		/**
		 * needed for Jackson deserialization unless use a custom deserializer
		 */
		protected State() {
			super(mapOf());
		}

		/**
		 * Constructs an AgentState with the given initial data.
		 * @param initData the initial data for the agent state
		 */
		public State(Map<String, Object> initData) {
			super(initData);
		}

	}

	@Test
	public void serializeWithTypeInferenceTest() throws IOException, ClassNotFoundException {

		OverAllState state = new OverAllState(mapOf("prop1", "value1"));

		JacksonStateSerializer serializer = new JacksonStateSerializer(OverAllState::new) {
		};

		Class<?> type = serializer.getStateType();

		assertEquals(State.class, type);

		byte[] bytes = serializer.writeObject(state);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		OverAllState deserializedState = serializer.readObject(bytes);

		assertNotNull(deserializedState);
		assertEquals(1, deserializedState.data().size());
		assertEquals("value1", deserializedState.data().get("prop1"));
	}

}
