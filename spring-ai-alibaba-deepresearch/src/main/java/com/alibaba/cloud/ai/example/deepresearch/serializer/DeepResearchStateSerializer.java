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
package com.alibaba.cloud.ai.example.deepresearch.serializer;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

public class DeepResearchStateSerializer extends PlainTextStateSerializer {

	protected final ObjectMapper objectMapper;

	public DeepResearchStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new ObjectMapper());
	}

	protected DeepResearchStateSerializer(AgentStateFactory<OverAllState> stateFactory, ObjectMapper objectMapper) {
		super(stateFactory);
		this.objectMapper = objectMapper;

		// register MessageDeserializer
		SimpleModule messageModule = new SimpleModule();
		messageModule.addDeserializer(Message.class, new MessageDeserializer());
		objectMapper.registerModule(messageModule);

		// register DeepResearchDeserializer
		SimpleModule stateModule = new SimpleModule();
		stateModule.addDeserializer(OverAllState.class, new DeepResearchDeserializer(objectMapper));
		objectMapper.registerModule(stateModule);

		// other properties
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		objectMapper.registerModule(new JavaTimeModule());
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(OverAllState object, ObjectOutput out) throws IOException {
		String json = objectMapper.writeValueAsString(object);

		// 这边修改的原因在于，序列化长度限制的问题，当数据量过大时，可能会导致序列化失败。修改`DeepResearchStateSerializer`使用字节数组方式避免UTF长度限制
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		out.writeInt(jsonBytes.length);
		out.write(jsonBytes);
	}

	@Override
	public OverAllState read(ObjectInput in) throws IOException {

		// 这边修改的原因在于，序列化长度限制的问题，当数据量过大时，可能会导致序列化失败。修改`DeepResearchStateSerializer`使用字节数组方式避免UTF长度限制
		int length = in.readInt();
		byte[] jsonBytes = new byte[length];
		in.readFully(jsonBytes);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		return objectMapper.readValue(json, OverAllState.class);
	}

}
