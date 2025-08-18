/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent;

import java.util.Optional;
import java.util.function.BiFunction;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class AgentTool implements BiFunction<OverAllState, ToolContext, OverAllState> {
	private ReactAgent2 agent;

	public AgentTool(ReactAgent2 agent) {
		this.agent = agent;
	}

	@Override
	public OverAllState apply(OverAllState s, ToolContext toolContext) {
		OverAllState state = (OverAllState)toolContext.getContext().get("state");
		Optional<OverAllState> resultState = null;
		try {
			resultState = agent.getCompiledGraph().invoke(state.data());
			return resultState.orElseThrow(() -> new RuntimeException("Failed to run agent: " + agent.name()));
		}
		catch (GraphRunnerException e) {
			throw new RuntimeException(e);
		}
		catch (GraphStateException e) {
			throw new RuntimeException(e);
		}
	}

	public static AgentTool create(ReactAgent2 agent) {
		return new AgentTool(agent);
	}

	public static ToolCallback getFunctionToolCallback(ReactAgent2 agent) {
		return FunctionToolCallback.builder(agent.name(), AgentTool.create(agent))
				.description(agent.description())
				.inputType(OverAllState.class)
				.build();
	}
}
