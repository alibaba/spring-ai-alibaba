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
package com.alibaba.cloud.ai.graph.agent.runner;

import java.util.List;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.common.collect.ImmutableList;

public abstract class BaseNodeAgent implements NodeAction {
	/** The agent's name. Must be a unique identifier within the graph. */
	protected String name;

	/**
	 * One line description about the agent's capability. The system can use this for decision-making
	 * when delegating control to different agents.
	 */
	protected String description;


	protected final List<? extends BaseNodeAgent> subAgents;

	public BaseNodeAgent(
			String name,
			String description,
			List<? extends BaseNodeAgent> subAgents) {
		this.name = name;
		this.description = description;
		this.subAgents = subAgents != null ? subAgents : ImmutableList.of();
	}

	/**
	 * Gets the agent's unique name.
	 *
	 * @return the unique name of the agent.
	 */
	public final String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 *
	 * @return the description of the agent.
	 */
	public final String description() {
		return description;
	}

}
