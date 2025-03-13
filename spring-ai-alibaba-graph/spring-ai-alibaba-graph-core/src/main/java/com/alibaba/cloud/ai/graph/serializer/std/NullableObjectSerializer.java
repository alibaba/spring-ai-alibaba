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
