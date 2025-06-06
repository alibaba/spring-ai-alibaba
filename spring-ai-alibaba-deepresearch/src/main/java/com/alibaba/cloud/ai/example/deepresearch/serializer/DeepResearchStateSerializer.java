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
		out.writeUTF(json);
	}

	@Override
	public OverAllState read(ObjectInput in) throws IOException {
		String json = in.readUTF();
		return objectMapper.readValue(json, OverAllState.class);
	}

}