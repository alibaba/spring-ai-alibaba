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
