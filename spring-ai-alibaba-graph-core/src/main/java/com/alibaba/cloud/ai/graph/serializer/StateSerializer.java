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
package com.alibaba.cloud.ai.graph.serializer;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Objects;

public abstract class StateSerializer implements Serializer<OverAllState> {

	private final AgentStateFactory<OverAllState> stateFactory;

	protected StateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this.stateFactory = Objects.requireNonNull(stateFactory, "stateFactory cannot be null");
	}

	public final AgentStateFactory<OverAllState> stateFactory() {
		return stateFactory;
	}

	public final OverAllState stateOf(Map<String, Object> data) {
		Objects.requireNonNull(data, "data cannot be null");
		return stateFactory.apply(data);
	}

	public final OverAllState cloneObject(Map<String, Object> data) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(data, "data cannot be null");
		return cloneObject(stateFactory().apply(data));
	}

	@Override
	public final void write(OverAllState object, ObjectOutput out) throws IOException {
		writeData(object.data(), out);
	}

	@Override
	public final OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
		return stateFactory().apply(readData(in));
	}

	public abstract void writeData(Map<String, Object> data, ObjectOutput out) throws IOException;

	public abstract Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException;

	public final byte[] dataToBytes(Map<String, Object> data) throws IOException {
		Objects.requireNonNull(data, "object cannot be null");
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ObjectOutputStream oas = new ObjectOutputStream(stream);
			writeData(data, oas);
			oas.flush();
			return stream.toByteArray();
		}
	}

	public final Map<String, Object> dataFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(bytes, "bytes cannot be null");
		if (bytes.length == 0) {
			throw new IllegalArgumentException("bytes cannot be empty");
		}
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			ObjectInputStream ois = new ObjectInputStream(stream);
			return readData(ois);
		}
	}

}
