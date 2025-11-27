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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public interface BaseCheckpointSaver {

	public String THREAD_ID_DEFAULT = "$default";

	/**
	 * Configures ObjectMapper with Spring AI Message type handlers for checkpoint
	 * serialization.
	 * This is a public static method to allow other checkpoint savers to reuse the
	 * same configuration.
	 * 
	 * @param objectMapper the ObjectMapper to configure
	 * @return the configured ObjectMapper
	 */
	static ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
		ObjectMapper mapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
		mapper.registerModule(new Jdk8Module());

		// Register Spring AI Message type handlers for proper
		// serialization/deserialization
		// This is crucial to prevent Message objects from being deserialized as HashMap
		SimpleModule module = new SimpleModule();

		// Use the centralized registration logic from SpringAIJacksonStateSerializer
		SpringAIJacksonStateSerializer.registerMessageHandlers(module);

		mapper.registerModule(module);

		// Configure default typing for non-final types (similar to
		// SpringAIJacksonStateSerializer)
		ObjectMapper.DefaultTypeResolverBuilder typeResolver = new ObjectMapper.DefaultTypeResolverBuilder(
				ObjectMapper.DefaultTyping.NON_FINAL, LaissezFaireSubTypeValidator.instance) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean useForType(JavaType t) {
				if (t.isTypeOrSubTypeOf(java.util.Map.class) || t.isMapLikeType() || t.isCollectionLikeType()
						|| t.isTypeOrSubTypeOf(java.util.Collection.class) || t.isArrayType()) {
					return false;
				}

				// Skip non-static inner classes, local classes, and anonymous classes
				// They cannot be deserialized by Jackson because they require an outer class instance
				Class<?> rawClass = t.getRawClass();
				if (rawClass != null) {
					// Non-static inner class
					if (rawClass.isMemberClass() && !java.lang.reflect.Modifier.isStatic(rawClass.getModifiers())) {
						return false;
					}
					// Local class or anonymous class
					if (rawClass.isLocalClass() || rawClass.isAnonymousClass()) {
						return false;
					}
				}

				return super.useForType(t);
			}
		};
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.init(JsonTypeInfo.Id.CLASS, null);
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.inclusion(JsonTypeInfo.As.PROPERTY);
		typeResolver = (ObjectMapper.DefaultTypeResolverBuilder) typeResolver.typeProperty("@class");
		mapper.setDefaultTyping(typeResolver);

		return mapper;
	}

	record Tag(String threadId, Collection<Checkpoint> checkpoints) {
		public Tag(String threadId, Collection<Checkpoint> checkpoints) {
			this.threadId = threadId;
			this.checkpoints = ofNullable(checkpoints).map(List::copyOf).orElseGet(List::of);
		}
	}

	default Tag release(RunnableConfig config) throws Exception {
		return null;
	}

	Collection<Checkpoint> list(RunnableConfig config);

	Optional<Checkpoint> get(RunnableConfig config);

	RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception;

	boolean clear(RunnableConfig config);

	default Optional<Checkpoint> getLast(LinkedList<Checkpoint> checkpoints, RunnableConfig config) {
		return (checkpoints == null || checkpoints.isEmpty()) ? Optional.empty() : ofNullable(checkpoints.peek());
	}

	default LinkedList<Checkpoint> getLinkedList(List<Checkpoint> checkpoints) {
		return Objects.nonNull(checkpoints) ? new LinkedList<>(checkpoints) : new LinkedList<>();
	}

}
