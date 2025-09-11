package com.alibaba.cloud.ai.graph.serializer.std;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectStreamStateSerializer extends StateSerializer {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectStreamStateSerializer.class);

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
					Serializer.writeUTF(e.getKey(), out);

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
				String key = Serializer.readUTF(in);

				Object value = readNullableObject(in).orElse(null);

				result.put(key, value);

			}
			return result;
		}

	}

	private final SerializerMapper mapper = new SerializerMapper();

	private final MapSerializer mapSerializer = new MapSerializer();

	public ObjectStreamStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);
		mapper.register(Collection.class, new ListSerializer());
		mapper.register(Map.class, new MapSerializer());
	}

	public SerializerMapper mapper() {
		return mapper;
	}

	@Override
	public final void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
		mapSerializer.write(data, mapper.objectOutputWithMapper(out));
	}

	@Override
	public final Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
		return mapSerializer.read(mapper.objectInputWithMapper(in));
	}

}
