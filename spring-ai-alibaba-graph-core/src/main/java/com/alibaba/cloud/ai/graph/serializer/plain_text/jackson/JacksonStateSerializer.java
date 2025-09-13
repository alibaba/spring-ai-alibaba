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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
		String json = objectMapper.writeValueAsString(data);
		Serializer.writeUTF(json, out);
	}

	@Override
	public final Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
		String json = Serializer.readUTF(in);
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

}
