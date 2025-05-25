package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

/**
 * Base Implementation of {@link PlainTextStateSerializer} using Jackson library. Need to
 * be extended from specific state implementation
 *
 */
public abstract class JacksonStateSerializer extends PlainTextStateSerializer {

	protected final ObjectMapper objectMapper;

	protected JacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new ObjectMapper());
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

	}

	protected JacksonStateSerializer(@NonNull AgentStateFactory<OverAllState> stateFactory,
			@NonNull ObjectMapper objectMapper) {
		super(stateFactory);
		this.objectMapper = objectMapper;
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
	public OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
		String json = in.readUTF();
		return objectMapper.readValue(json, getStateType());
	}

}
