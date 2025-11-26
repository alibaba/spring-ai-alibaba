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
import org.springframework.ai.chat.model.ChatResponse;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
				false);
		this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
				false);
		this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

		var module = new SimpleModule();
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
		// Normalize state before serialization to convert non-serializable objects
		// (GraphResponse, CompletableFuture) into serializable structures
		Map<String, Object> normalized = normalizeForSerialization(data);
		String json = objectMapper.writeValueAsString(normalized);
		Serializer.writeUTF(json, out);
	}

	@Override
	public final Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
		String json = Serializer.readUTF(in);
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Normalize state map before serialization.
	 * Converts non-serializable objects (GraphResponse, CompletableFuture) to Maps.
	 */
	private Map<String, Object> normalizeForSerialization(Map<String, Object> state) {
		if (state == null || state.isEmpty()) {
			return state;
		}
		Map<String, Object> result = new LinkedHashMap<>(state.size());
		state.forEach((key, value) -> result.put(key, normalizeValue(value)));
		return result;
	}

	/**
	 * Normalize a single value for serialization.
	 * Only transforms GraphResponse, ChatResponse and CompletableFuture, recursively checks containers.
	 */
	private Object normalizeValue(Object value) {
		if (value == null) {
			return null;
		}

		// 1. GraphResponse → snapshot map (fully normalize internally)
		if (value instanceof GraphResponse) {
			return normalizeGraphResponse((GraphResponse<?>) value);
		}

		// 2. ChatResponse → snapshot map (fully normalize internally)
		if (value instanceof ChatResponse) {
			return normalizeChatResponse((ChatResponse) value);
		}

		// 3. CompletableFuture → snapshot map (fully normalize internally)
		if (value instanceof CompletableFuture) {
			return normalizeCompletableFuture((CompletableFuture<?>) value);
		}

		// 4. Map → shallow scan for GraphResponse/ChatResponse/CompletableFuture
		if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			Map<Object, Object> result = new LinkedHashMap<>(map.size());
			boolean changed = false;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object normalized = normalizeValue(entry.getValue());
				result.put(entry.getKey(), normalized);
				if (normalized != entry.getValue()) {
					changed = true;
				}
			}
			return changed ? result : value;
		}

		// 5. Collection → shallow scan for GraphResponse/ChatResponse/CompletableFuture
		if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			List<Object> result = new ArrayList<>(collection.size());
			boolean changed = false;
			for (Object item : collection) {
				Object normalized = normalizeValue(item);
				result.add(normalized);
				if (normalized != item) {
					changed = true;
				}
			}
			return changed ? result : value;
		}

	// 6. Array → shallow scan for GraphResponse/ChatResponse/CompletableFuture
	if (value.getClass().isArray()) {
		// Check if it's a primitive array (int[], double[], etc.)
		Class<?> componentType = value.getClass().getComponentType();
		if (componentType.isPrimitive()) {
			// Primitive arrays cannot contain GraphResponse/ChatResponse/CompletableFuture
			// Return as-is, let Jackson handle the serialization
			return value;
		}
		
		// Object array - check for GraphResponse/ChatResponse/CompletableFuture
		Object[] array = (Object[]) value;
		Object[] result = new Object[array.length];
		boolean changed = false;
		for (int i = 0; i < array.length; i++) {
			Object normalized = normalizeValue(array[i]);
			result[i] = normalized;
			if (normalized != array[i]) {
				changed = true;
			}
		}
		// If nothing changed, return original array to preserve type
		return changed ? result : value;
	}

	// 7. Optional → unwrap and check
		if (value instanceof Optional) {
			Optional<?> opt = (Optional<?>) value;
			if (opt.isEmpty()) {
				return null;
			}
			Object unwrapped = opt.get();
			Object normalized = normalizeValue(unwrapped);
			return normalized == unwrapped ? value : normalized;
		}

		// 7. Other types → return as-is (assumed serializable)
		return value;
	}

	/**
	 * Convert GraphResponse into serializable snapshot map with type marker.
	 */
	private Map<String, Object> normalizeGraphResponse(GraphResponse<?> response) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("@type", "GraphResponse");
		snapshot.put("error", response.isError());

		// Extract result or error
		if (response.isError()) {
			// Error case
			snapshot.put("status", "error");
			if (response.getOutput() != null) {
				try {
					response.getOutput().getNow(null);
				} catch (Exception e) {
					Map<String, Object> errorMap = new LinkedHashMap<>();
					errorMap.put("message", e.getMessage());
					errorMap.put("class", e.getClass().getName());
					snapshot.put("errorDetails", errorMap);
				}
			}
		} else if (response.isDone()) {
			// Done case (output == null, value in resultValue)
			snapshot.put("status", "done");
			response.resultValue().ifPresent(result -> snapshot.put("result", deepNormalizeValue(result)));
		} else {
			// Pending or completed case (output != null and not error)
			// The output CompletableFuture contains the value
			if (response.getOutput() != null && response.getOutput().isDone()) {
				snapshot.put("status", "completed");
				try {
					Object result = response.getOutput().getNow(null);
					snapshot.put("result", deepNormalizeValue(result));
				} catch (Exception ignored) {
					// Should not happen as we checked isDone() and !isError()
				}
			} else {
				snapshot.put("status", "pending");
			}
		}

		snapshot.put("metadata", new LinkedHashMap<>(response.getAllMetadata()));
		return snapshot;
	}

	/**
	 * Convert ChatResponse into serializable snapshot map with type marker.
	 */
	private Map<String, Object> normalizeChatResponse(ChatResponse response) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("@type", "ChatResponse");
		
		// Normalize result (Generation list)
		if (response.getResult() != null) {
			snapshot.put("result", deepNormalizeValue(response.getResult()));
		} else {
			snapshot.put("result", null);
		}
		
		// Normalize metadata
		if (response.getMetadata() != null) {
			snapshot.put("metadata", deepNormalizeValue(response.getMetadata()));
		} else {
			snapshot.put("metadata", null);
		}
		
		return snapshot;
	}

	/**
	 * Deep normalize a value (for use inside GraphResponse/ChatResponse/CompletableFuture).
	 * Recursively normalizes all containers.
	 */
	private Object deepNormalizeValue(Object value) {
		if (value == null) {
			return null;
		}

		// 1. GraphResponse → snapshot map
		if (value instanceof GraphResponse) {
			return normalizeGraphResponse((GraphResponse<?>) value);
		}

		// 2. ChatResponse → snapshot map
		if (value instanceof ChatResponse) {
			return normalizeChatResponse((ChatResponse) value);
		}

		// 3. CompletableFuture → snapshot map
		if (value instanceof CompletableFuture) {
			return normalizeCompletableFuture((CompletableFuture<?>) value);
		}

		// 4. Map → recursive normalization
		if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			Map<Object, Object> normalized = new LinkedHashMap<>(map.size());
			map.forEach((k, v) -> normalized.put(k, deepNormalizeValue(v)));
			return normalized;
		}

		// 5. Collection → recursive normalization
		if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			return collection.stream().map(this::deepNormalizeValue).collect(Collectors.toList());
		}

	// 6. Array → recursive normalization
	if (value.getClass().isArray()) {
		// Check if it's a primitive array
		Class<?> componentType = value.getClass().getComponentType();
		if (componentType.isPrimitive()) {
			// Primitive arrays cannot contain GraphResponse/CompletableFuture
			return value;
		}
		
		Object[] array = (Object[]) value;
		return Arrays.stream(array).map(this::deepNormalizeValue).toArray();
	}

		// 6. Optional → unwrap and normalize
		if (value instanceof Optional) {
			return ((Optional<?>) value).map(this::deepNormalizeValue).orElse(null);
		}

		// 7. Other types → return as-is (assumed serializable)
		return value;
	}

	/**
	 * Convert CompletableFuture into serializable snapshot map with type marker.
	 */
	private Map<String, Object> normalizeCompletableFuture(CompletableFuture<?> future) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("@type", "CompletableFuture");

		if (future.isDone()) {
			if (future.isCompletedExceptionally()) {
				snapshot.put("status", "failed");
				try {
					future.getNow(null);
				}
				catch (Exception e) {
					Map<String, Object> errorMap = new LinkedHashMap<>();
					errorMap.put("message", e.getMessage());
					errorMap.put("class", e.getClass().getName());
					snapshot.put("error", errorMap);
				}
			}
			else {
				snapshot.put("status", "completed");
				try {
					Object result = future.getNow(null);
					snapshot.put("result", deepNormalizeValue(result));
				}
				catch (Exception ignored) {
					snapshot.put("result", null);
				}
			}
		}
		else {
			snapshot.put("status", "pending");
		}

		return snapshot;
	}

}
