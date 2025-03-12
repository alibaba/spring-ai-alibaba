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

package com.alibaba.cloud.ai.graph.serializer;

import java.io.IOException;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

public abstract class StateSerializer<T> implements Serializer<T> {

	private final AgentStateFactory<T> stateFactory;

	protected StateSerializer(@NonNull AgentStateFactory<T> stateFactory) {
		this.stateFactory = stateFactory;
	}

	public final AgentStateFactory<T> stateFactory() {
		return stateFactory;
	}

	public final T stateOf(@NonNull Map<String, Object> data) {
		return stateFactory.apply(data);
	}

	public final T cloneObject(@NonNull Map<String, Object> data) throws IOException, ClassNotFoundException {
		return cloneObject(stateFactory().apply(data));
	}

}
