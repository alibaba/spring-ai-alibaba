package com.alibaba.cloud.ai.graph.serializer.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class JSONStateSerializer extends PlainTextStateSerializer {

	public static final JSONStateSerializer INSTANCE = new JSONStateSerializer();

	final ObjectMapper objectMapper;

	public JSONStateSerializer() {
		this(new ObjectMapper());
	}

	public JSONStateSerializer(@NonNull ObjectMapper objectMapper) {
		super(stringObjectMap -> null);
		this.objectMapper = objectMapper;
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		var module = new SimpleModule();
		module.addDeserializer(NodeState.class, new StateDeserializer());
		module.addDeserializer(AgentOutcome.class, new AgentOutcomeDeserializer());
		module.addDeserializer(AgentAction.class, new AgentActionDeserializer());
		module.addDeserializer(AgentFinish.class, new AgentFinishDeserializer());

		objectMapper.registerModule(module);
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(OverAllState object, ObjectOutput out) throws IOException {
		var json = objectMapper.writeValueAsString(object);
		out.writeUTF(json);
	}

	@Override
	public OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
		var json = in.readUTF();
		return objectMapper.readValue(json, OverAllState.class);
	}

}
