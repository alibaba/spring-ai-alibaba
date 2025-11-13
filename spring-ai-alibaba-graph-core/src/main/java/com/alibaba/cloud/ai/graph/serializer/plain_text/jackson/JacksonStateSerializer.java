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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Base Implementation of {@link PlainTextStateSerializer} using Jackson library. Need to
 * be extended from specific state implementation
 */
public abstract class JacksonStateSerializer extends PlainTextStateSerializer {

	protected final ObjectMapper objectMapper;

	protected TypeMapper typeMapper = new TypeMapper();

	protected JacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new ObjectMapper());
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

	}

	protected JacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory, ObjectMapper objectMapper) {
		super(stateFactory);
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");

		this.objectMapper.registerModule(new Jdk8Module());
		this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
				false);
		this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
				false);
		this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

		var module = new SimpleModule();
		module.addSerializer(GraphResponse.class, new GraphResponseSerializer());
		module.addSerializer(CompletableFuture.class, new CompletableFutureSerializer());
		module.addDeserializer(Map.class, new GenericMapDeserializer(typeMapper));
		module.addDeserializer(List.class, new GenericListDeserializer(typeMapper));

		this.objectMapper.registerModule(module);

	}

	public TypeMapper typeMapper() {
		return typeMapper;
	}

	public ObjectMapper objectMapper() {
		return objectMapper;
	}

	@Override
	public String contentType() {
		return "application/json";
	}

	@Override
	public final void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
		Map<String, Object> sanitized = sanitizeState(data);
		String json = objectMapper.writeValueAsString(sanitized != null ? sanitized : data);
		Serializer.writeUTF(json, out);
	}

	@Override
	public final Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
		String json = Serializer.readUTF(in);
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

	private static Map<String, Object> sanitizeState(Map<String, Object> source) {
		if (source == null) {
			return null;
		}
		Map<String, Object> sanitized = new LinkedHashMap<>(source.size());
		source.forEach((key, value) -> sanitized.put(key, sanitizeValue(value)));
		return sanitized;
	}

	private static Object sanitizeValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof GraphResponse<?> response) {
			return snapshotGraphResponse(response);
		}
		if (value instanceof Map<?, ?> map) {
			return sanitizeNestedMap(map);
		}
		if (value instanceof Collection<?> collection) {
			return collection.stream().map(JacksonStateSerializer::sanitizeValue)
				.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		}
		Class<?> valueClass = value.getClass();
		if (valueClass.isArray()) {
			return sanitizeArray(value, valueClass.getComponentType());
		}
		if (value instanceof Optional<?> optional) {
			return optional.map(JacksonStateSerializer::sanitizeValue).orElse(null);
		}
		if (value instanceof CompletableFuture<?> future) {
			return snapshotFuture(future);
		}
		return value;
	}

	private static Map<String, Object> sanitizeNestedMap(Map<?, ?> map) {
		Map<String, Object> sanitized = new LinkedHashMap<>(map.size());
		map.forEach((k, v) -> sanitized.put(String.valueOf(k), sanitizeValue(v)));
		return sanitized;
	}

	private static Object sanitizeArray(Object array, Class<?> componentType) {
		if (componentType.isPrimitive()) {
			return array;
		}
		int length = Array.getLength(array);
		Object[] sanitizedElements = new Object[length];
		boolean preserveComponentType = true;
		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			Object sanitized = sanitizeValue(element);
			sanitizedElements[i] = sanitized;
			if (sanitized != null && !componentType.isInstance(sanitized)) {
				preserveComponentType = false;
			}
		}
		if (preserveComponentType) {
			Object typedArray = Array.newInstance(componentType, length);
			for (int i = 0; i < length; i++) {
				Array.set(typedArray, i, sanitizedElements[i]);
			}
			return typedArray;
		}
		return sanitizedElements;
	}

	private static Map<String, Object> snapshotGraphResponse(GraphResponse<?> response) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		if (response == null) {
			snapshot.put("status", "pending");
			snapshot.put("error", Boolean.FALSE);
			return snapshot;
		}
		if (response.isError()) {
			snapshot.put("status", "error");
			snapshot.put("error", Boolean.TRUE);
			Throwable throwable = extractThrowable(response.getOutput());
			if (throwable != null) {
				snapshot.put("exception", throwable.getClass().getName());
				snapshot.put("message", throwable.getMessage());
			}
		}
		else if (response.isDone()) {
			snapshot.put("status", "done");
			snapshot.put("error", Boolean.FALSE);
			response.resultValue().ifPresent(result -> snapshot.put("result", sanitizeValue(result)));
		}
		else {
			CompletableFuture<?> future = response.getOutput();
			snapshot.putAll(snapshotFuture(future));
		}
		Map<String, Object> metadata = response.getAllMetadata();
		if (!metadata.isEmpty()) {
			snapshot.put("metadata", sanitizeState(metadata));
		}
		return snapshot;
	}

	private static Map<String, Object> snapshotFuture(CompletableFuture<?> future) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		if (future == null) {
			snapshot.put("status", "pending");
			snapshot.put("error", Boolean.FALSE);
			return snapshot;
		}
		if (!future.isDone()) {
			snapshot.put("status", "pending");
			snapshot.put("error", Boolean.FALSE);
			return snapshot;
		}
		if (future.isCompletedExceptionally()) {
			snapshot.put("status", "error");
			snapshot.put("error", Boolean.TRUE);
			Throwable throwable = extractThrowable(future);
			if (throwable != null) {
				snapshot.put("exception", throwable.getClass().getName());
				snapshot.put("message", throwable.getMessage());
			}
			return snapshot;
		}
		Object result = future.getNow(null);
		snapshot.put("status", "completed");
		snapshot.put("error", Boolean.FALSE);
		if (result != null) {
			snapshot.put("result", sanitizeValue(result));
		}
		return snapshot;
	}

	private static Throwable extractThrowable(CompletableFuture<?> future) {
		if (future == null || !future.isCompletedExceptionally()) {
			return null;
		}
		Throwable throwable = future.handle((value, ex) -> ex).join();
		if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
			return completionException.getCause();
		}
		return throwable;
	}

	private static final class GraphResponseSerializer extends JsonSerializer<GraphResponse> {

		@Override
		public void serialize(GraphResponse value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			Map<String, Object> snapshot = snapshotGraphResponse(value);
			serializers.defaultSerializeValue(snapshot, gen);
		}

		@Override
		public void serializeWithType(GraphResponse value, JsonGenerator gen, SerializerProvider serializers,
				TypeSerializer typeSer) throws IOException {
			Map<String, Object> snapshot = snapshotGraphResponse(value);
			serializers.findValueSerializer(Map.class, null).serializeWithType(snapshot, gen, serializers, typeSer);
		}
	}

	private static final class CompletableFutureSerializer extends JsonSerializer<CompletableFuture> {

		@Override
		public void serialize(CompletableFuture value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			Map<String, Object> snapshot = snapshotFuture(value);
			serializers.defaultSerializeValue(snapshot, gen);
		}

		@Override
		public void serializeWithType(CompletableFuture value, JsonGenerator gen, SerializerProvider serializers,
				TypeSerializer typeSer) throws IOException {
			Map<String, Object> snapshot = snapshotFuture(value);
			serializers.findValueSerializer(Map.class, null).serializeWithType(snapshot, gen, serializers, typeSer);
		}
	}

}
