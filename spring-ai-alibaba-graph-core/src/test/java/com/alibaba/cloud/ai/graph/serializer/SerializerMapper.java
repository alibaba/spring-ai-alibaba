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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SerializerMapper {

	static final Serializer<Object> DEFAULT_SERIALIZER = new Serializer<Object>() {
		@Override
		public void write(Object object, ObjectOutput out) throws IOException {
			out.writeObject(object);
		}

		@Override
		public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
			return in.readObject();
		}
	};

	static class Key {

		private final String _className;

		private final Class<?> _clazz;

		public static Key of(Class<?> clazz) {
			return new Key(clazz);
		}

		public static Key of(String className) {
			return new Key(className);
		}

		private Key(Class<?> clazz) {
			_className = clazz.getName();
			_clazz = clazz;
		}

		private Key(String className) {
			_className = className;
			_clazz = null;
		}

		String getTypeName() {
			return _className;
		}

		Class<?> getType() {
			return _clazz;
		}

		@Override
		public boolean equals(Object o) {
			return Objects.equals(o, _className);
		}

		@Override
		public int hashCode() {
			return Objects.hash(_className);
		}

	}

	private final Map<Key, Serializer<?>> _serializers = new HashMap<>();

	public SerializerMapper register(Class<?> clazz, Serializer<?> serializer) {
		_serializers.put(Key.of(clazz), serializer);
		return this;
	}

	public boolean unregister(Class<? extends Serializer<?>> clazz) {
		Objects.requireNonNull(clazz, "Serializer's class cannot be null");
		Serializer<?> serializer = _serializers.remove(Key.of(clazz));
		return serializer != null;
	}

	@SuppressWarnings("unchecked")
	public Optional<Serializer<Object>> getSerializer(Class<?> clazz) {
		Serializer<?> ser = _serializers.get(Key.of(clazz));

		return (ser != null) ?

				Optional.of((Serializer<Object>) ser) :

				_serializers.entrySet()
					.stream()
					.filter(e -> e.getKey().getType().isAssignableFrom(clazz))
					.findFirst()
					.map(e -> (Serializer<Object>) e.getValue());

	}

	@SuppressWarnings("unchecked")
	public Optional<Serializer<Object>> getSerializer(String className) {
		return Optional.ofNullable((Serializer<Object>) _serializers.get(Key.of(className)));
	}

	public Serializer<Object> getDefaultSerializer() {
		return DEFAULT_SERIALIZER;
	}

	public final ObjectOutput objectOutputWithMapper(ObjectOutput out) {

		final ObjectOutputWithMapper mapperOut;
		if (out instanceof ObjectOutputWithMapper) {
			mapperOut = (ObjectOutputWithMapper) out;
		}
		else {
			mapperOut = new ObjectOutputWithMapper(out, this);
		}

		return mapperOut;
	}

	public final ObjectInput objectInputWithMapper(ObjectInput in) {

		final ObjectInputWithMapper mapperIn;
		if (in instanceof ObjectInputWithMapper) {
			mapperIn = (ObjectInputWithMapper) in;
		}
		else {
			mapperIn = new ObjectInputWithMapper(in, this);
		}

		return mapperIn;

	}

	@Override
	public String toString() {
		List<String> typeNames = _serializers.keySet().stream().map(Key::getTypeName).collect(Collectors.toList());
		return format("SerializerMapper: \n%s", String.join("\n", typeNames));

	}

}
