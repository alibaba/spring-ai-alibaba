/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.agent;

import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import reactor.core.publisher.Flux;

/**
 * Interface for executing agent operations. Provides methods for both streaming and
 * synchronous execution of agent tasks.
 *
 * @since 1.0.0.3
 */
public interface AgentExecutor {

	/**
	 * Executes the agent operation in streaming mode.
	 * @param context The execution context containing agent state and configuration
	 * @param request The input request for the agent
	 * @return A Flux of agent responses
	 */
	Flux<AgentResponse> streamExecute(AgentContext context, AgentRequest request);

	/**
	 * Executes the agent operation synchronously.
	 * @param context The execution context containing agent state and configuration
	 * @param request The input request for the agent
	 * @return The agent response
	 */
	AgentResponse execute(AgentContext context, AgentRequest request);

}
