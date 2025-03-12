/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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