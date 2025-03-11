package com.alibaba.cloud.ai.graph.serializer.std;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.serializer.Serializer;

public interface NullableObjectSerializer<T> extends Serializer<T> {

	default void writeNullableObject(Object object, ObjectOutput out) throws IOException {
		if (object == null) {
			out.writeByte(0);
		}
		else {
			out.writeByte(1);
			out.writeObject(object);
		}
	}

	default Optional<Object> readNullableObject(ObjectInput in) throws IOException, ClassNotFoundException {
		byte b = in.readByte();
		return (b == 0) ? Optional.empty() : Optional.of(in.readObject());
	}

	default void writeNullableUTF(String object, ObjectOutput out) throws IOException {
		if (object == null) {
			out.writeByte(0);
		}
		else {
			out.writeByte(1);
			out.writeUTF(object);
		}
	}

	default Optional<String> readNullableUTF(ObjectInput in) throws IOException {
		byte b = in.readByte();
		if (b == 0) {
			return Optional.empty();
		}
		return Optional.of(in.readUTF());
	}

}
