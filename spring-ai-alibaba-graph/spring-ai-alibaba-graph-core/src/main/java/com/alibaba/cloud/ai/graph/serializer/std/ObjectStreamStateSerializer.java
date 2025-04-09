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
package com.alibaba.cloud.ai.graph.serializer.std;

import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectStreamStateSerializer<State extends AgentState> extends StateSerializer<State> {

	private static final Logger log = LoggerFactory.getLogger(ObjectStreamStateSerializer.class);

	static class ListSerializer implements NullableObjectSerializer<List<Object>> {

		@Override
		public void write(List<Object> object, ObjectOutput out) throws IOException {
			out.writeInt(object.size());

			for (Object value : object) {
				try {
					writeNullableObject(value, out);
				}
				catch (IOException ex) {
					log.error("Error writing collection value", ex);
					throw ex;
				}
			}

			out.flush();

		}

		@Override
		public List<Object> read(ObjectInput in) throws IOException, ClassNotFoundException {
			List<Object> result = new ArrayList<>();

			int size = in.readInt();

			for (int i = 0; i < size; i++) {

				Object value = readNullableObject(in).orElse(null);

				result.add(value);

			}

			return result;
		}

	}

	static class MapSerializer implements NullableObjectSerializer<Map<String, Object>> {

		@Override
		public void write(Map<String, Object> object, ObjectOutput out) throws IOException {
			out.writeInt(object.size());

			for (Map.Entry<String, Object> e : object.entrySet()) {
				try {
					out.writeUTF(e.getKey());

					writeNullableObject(e.getValue(), out);

				}
				catch (IOException ex) {
					log.error("Error writing map key '{}'", e.getKey(), ex);
					throw ex;
				}
			}

			out.flush();

		}

		@Override
		public Map<String, Object> read(ObjectInput in) throws IOException, ClassNotFoundException {
			Map<String, Object> result = new HashMap<>();

			int size = in.readInt();

			for (int i = 0; i < size; i++) {
				String key = in.readUTF();

				Object value = readNullableObject(in).orElse(null);

				result.put(key, value);

			}
			return result;
		}

	}

	private final SerializerMapper mapper = new SerializerMapper();

	private final MapSerializer mapSerializer = new MapSerializer();

	public ObjectStreamStateSerializer(AgentStateFactory<State> stateFactory) {
		super(stateFactory);
		mapper.register(Collection.class, new ListSerializer());
		mapper.register(Map.class, new MapSerializer());
	}

	public SerializerMapper mapper() {
		return mapper;
	}

	@Override
	public void write(State object, ObjectOutput out) throws IOException {
		mapSerializer.write(object.data(), mapper.objectOutputWithMapper(out));
	}

	@Override
	public final State read(ObjectInput in) throws IOException, ClassNotFoundException {
		return stateFactory().apply(mapSerializer.read(mapper.objectInputWithMapper(in)));
	}

}
