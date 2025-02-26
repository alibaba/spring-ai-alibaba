package com.alibaba.cloud.ai.graph.serializer.plain_text.gson;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

/**
 * Base Implementation of {@link PlainTextStateSerializer} using GSON library . Need to be
 * extended from specific state implementation
 *
 */
public abstract class GsonStateSerializer extends PlainTextStateSerializer {

	protected final Gson gson;

	protected GsonStateSerializer(@NonNull AgentStateFactory<OverAllState> stateFactory, Gson gson) {
		super(stateFactory);
		this.gson = gson;
	}

	protected GsonStateSerializer(@NonNull AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new GsonBuilder().serializeNulls().create());
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(OverAllState object, ObjectOutput out) throws IOException {
		String json = gson.toJson(object);
		out.writeUTF(json);

	}

	@Override
	public OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
		return gson.fromJson(in.readUTF(), getStateType());
	}

}