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
package com.alibaba.cloud.ai.graph.agent;

import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * Abstract base class for all agents in the graph system. Contains common properties and
 * methods shared by different agent implementations.
 */
public abstract class BaseAgent {

	/** The agent's name. Must be a unique identifier within the graph. */
	protected String name;

	/**
	 * One line description about the agent's capability. The system can use this for
	 * decision-making when delegating control to different agents.
	 */
	protected String description;

	/** The output key for the agent's result */
	protected String outputKey;

	/**
	 * Protected constructor for initializing all base agent properties.
	 * @param name the unique name of the agent
	 * @param description the description of the agent's capability
	 * @param outputKey the output key for the agent's result
	 */
	protected BaseAgent(String name, String description, String outputKey) {
		this.name = name;
		this.description = description;
		this.outputKey = outputKey;
	}

	/**
	 * Default protected constructor for subclasses that need to initialize properties
	 * differently.
	 */
	protected BaseAgent() {
		// Allow subclasses to initialize properties through other means
	}

	/**
	 * Gets the agent's unique name.
	 * @return the unique name of the agent.
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 * @return the description of the agent.
	 */
	public String description() {
		return description;
	}

	/**
	 * Gets the output key for the agent's result.
	 * @return the output key.
	 */
	public String outputKey() {
		return outputKey;
	}

	/**
	 * Gets the list of sub-agents.
	 * @return the list of sub-agents.
	 */
	public abstract AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException;

	public abstract Optional<OverAllState> invoke(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException;

}
