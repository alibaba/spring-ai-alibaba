package com.alibaba.cloud.ai.graph.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface Serializer<T> {

	void write(T object, ObjectOutput out) throws IOException;

	T read(ObjectInput in) throws IOException, ClassNotFoundException;

	default String contentType() {
		return "application/octet-stream";
	}

	default byte[] objectToBytes(T object) throws IOException {
		Objects.requireNonNull(object, "object cannot be null");
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ObjectOutputStream oas = new ObjectOutputStream(stream);
			write(object, oas);
			oas.flush();
			return stream.toByteArray();
		}
	}

	default T bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(bytes, "bytes cannot be null");
		if (bytes.length == 0) {
			throw new IllegalArgumentException("bytes cannot be empty");
		}
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			ObjectInputStream ois = new ObjectInputStream(stream);
			return read(ois);
		}
	}

	@Deprecated(forRemoval = true)
	default byte[] writeObject(T object) throws IOException {
		return objectToBytes(object);
	}

	@Deprecated(forRemoval = true)
	default T readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return bytesToObject(bytes);
	}

	default T cloneObject(T object) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(object, "object cannot be null");
		return readObject(writeObject(object));
	}

	// Fix issue for string greater than 65K
	static void writeUTF(String object, ObjectOutput out) throws IOException {
		Objects.requireNonNull(object, "object cannot be null");
		if (object.isEmpty()) {
			out.writeInt(0);
			return;
		}
		byte[] utf8Bytes = object.getBytes(StandardCharsets.UTF_8);
		out.writeInt(utf8Bytes.length); // prefix with length
		out.write(utf8Bytes);
	}

	// Fix issue for string greater than 65K
	static String readUTF(ObjectInput in) throws IOException {
		int length = in.readInt();
		if (length == 0) {
			return "";
		}
		byte[] utf8Bytes = new byte[length];
		in.readFully(utf8Bytes);
		return new String(utf8Bytes, StandardCharsets.UTF_8);
	}

}
