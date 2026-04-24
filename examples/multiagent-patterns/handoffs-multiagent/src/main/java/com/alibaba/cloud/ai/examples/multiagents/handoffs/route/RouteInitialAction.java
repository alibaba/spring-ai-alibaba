/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.handoffs.route;

import com.alibaba.cloud.ai.examples.multiagents.handoffs.state.MultiAgentStateConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;

import java.util.concurrent.CompletableFuture;

/**
 * Routes from START to the active agent. Defaults to sales_agent when
 * active_agent is not set or not support_agent.
 */
public class RouteInitialAction implements AsyncCommandAction {

	@Override
	public CompletableFuture<Command> apply(OverAllState state, RunnableConfig config) {
		String target = state.value(MultiAgentStateConstants.ACTIVE_AGENT)
				.map(Object::toString)
				.filter(MultiAgentStateConstants.SUPPORT_AGENT::equals)
				.orElse(MultiAgentStateConstants.SALES_AGENT);
		return CompletableFuture.completedFuture(new Command(target));
	}
}
