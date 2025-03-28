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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;

public interface Serializer<T> {

	void write(T object, ObjectOutput out) throws IOException;

	T read(ObjectInput in) throws IOException, ClassNotFoundException;

	default String mimeType() {
		return "application/octet-stream";
	}

	default byte[] writeObject(T object) throws IOException {
		Objects.requireNonNull(object, "object cannot be null");
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ObjectOutputStream oas = new ObjectOutputStream(stream);
			write(object, oas);
			oas.flush();
			return stream.toByteArray();
		}
	}

	default T readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(bytes, "bytes cannot be null");
		if (bytes.length == 0) {
			throw new IllegalArgumentException("bytes cannot be empty");
		}
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			ObjectInputStream ois = new ObjectInputStream(stream);
			return read(ois);
		}
	}

	default T cloneObject(T object) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(object, "object cannot be null");
		return readObject(writeObject(object));
	}

}
