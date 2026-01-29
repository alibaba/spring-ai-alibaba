/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hook;


import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_HOOK_NAME_PREFIX;

/**
 * Hook interface for intercepting and customizing agent execution flow.
 * <p>
 * Hooks allow you to inject custom logic at specific points in the agent execution lifecycle.
 * There are two main types of hooks:
 * <ul>
 *   <li>{@link AgentHook}: Executes before/after the entire agent loop</li>
 *   <li>{@link ModelHook}: Executes before/after model calls</li>
 * </ul>
 * <p>
 * Hooks can also provide interceptors, control flow routing, and define key strategies
 * for state management.
 *
 * @see AgentHook
 * @see ModelHook
 * @see HookPosition
 */
public interface Hook extends Prioritized {
	/**
	 * Gets the unique name of this hook.
	 * The name is used to identify the hook in the execution graph.
	 *
	 * @return the hook name, must not be null
	 */
	String getName();

	/**
	 * Sets the name of the agent that owns this hook.
	 * This is typically called by the framework during agent initialization.
	 *
	 * @param agentName the agent name, must not be null
	 */
	void setAgentName(String agentName);

	/**
	 * Gets the name of the agent that owns this hook.
	 *
	 * @return the agent name, or null if not set
	 */
	String getAgentName();

	/**
	 * Gets the ReactAgent instance that owns this hook.
	 * This allows hooks to access the agent's state and configuration.
	 *
	 * @return the ReactAgent instance, or null if not set
	 */
	ReactAgent getAgent();

	/**
	 * Sets the ReactAgent instance that owns this hook.
	 * This is typically called by the framework during agent initialization.
	 *
	 * @param agent the ReactAgent instance, must not be null
	 */
	void setAgent(ReactAgent agent);

	/**
	 * Gets the list of model interceptors provided by this hook.
	 * <p>
	 * Model interceptors can modify model requests/responses, add retry logic,
	 * implement guardrails, or provide other cross-cutting concerns.
	 * <p>
	 * These interceptors will be merged with the agent's configured interceptors
	 * and applied to all model calls.
	 *
	 * @return list of model interceptors, empty list by default
	 * @see ModelInterceptor
	 */
	default List<ModelInterceptor> getModelInterceptors() {
		return List.of();
	}

	/**
	 * Gets the list of tool interceptors provided by this hook.
	 * <p>
	 * Tool interceptors can modify tool calls, add retry logic, implement tool selection,
	 * or provide other cross-cutting concerns for tool execution.
	 * <p>
	 * These interceptors will be merged with the agent's configured interceptors
	 * and applied to all tool calls.
	 *
	 * @return list of tool interceptors, empty list by default
	 * @see ToolInterceptor
	 */
	default List<ToolInterceptor> getToolInterceptors() {
		return List.of();
	}

	/**
	 * Gets the list of tools provided by this hook.
	 * <p>
	 * Tools provided by hooks will be merged with the agent's configured tools
	 * and made available for the agent to use during execution.
	 * <p>
	 * These tools will be registered with the agent's tool node and can be
	 * invoked by the agent during its execution.
	 *
	 * @return list of tool callbacks, empty list by default
	 * @see ToolCallback
	 */
	default List<ToolCallback> getTools() {
		return List.of();
	}

	/**
	 * Gets the list of allowed jump destinations for this hook.
	 * <p>
	 * Hooks can control the flow of execution by specifying where the agent
	 * can jump to after the hook executes. This allows hooks to redirect the
	 * execution flow based on their logic.
	 * <p>
	 * Common jump destinations include:
	 * <ul>
	 *   <li>{@link JumpTo#model}: Jump to model node</li>
	 *   <li>{@link JumpTo#tool}: Jump to tool node</li>
	 *   <li>{@link JumpTo#end}: Jump to end (terminate agent)</li>
	 * </ul>
	 *
	 * @return list of allowed jump destinations, empty list by default
	 * @see JumpTo
	 */
	default List<JumpTo> canJumpTo() {
		return List.of();
	}

	/**
	 * Gets the key strategies for state management.
	 * <p>
	 * Key strategies define how state values are merged when updating the agent's state.
	 * This allows hooks to control how their state updates interact with the agent's state.
	 * <p>
	 * Common strategies include:
	 * <ul>
	 *   <li>{@link com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy}: Append to existing values</li>
	 *   <li>{@link com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy}: Replace existing values</li>
	 * </ul>
	 *
	 * @return map of key names to their strategies, empty map by default
	 * @see KeyStrategy
	 */
	default Map<String, KeyStrategy> getKeyStrategys() {
		return Map.of();
	}

	/**
	 * Gets the positions where this hook should be executed in the agent execution flow.
	 * <p>
	 * The execution order is determined by:
	 * <ol>
	 *   <li>If the implementing class has a {@link HookPositions} annotation, use its value</li>
	 *   <li>Otherwise, use default positions based on hook type:
	 *     <ul>
	 *       <li>{@link AgentHook}: {@link HookPosition#BEFORE_AGENT} and {@link HookPosition#AFTER_AGENT}</li>
	 *       <li>{@link ModelHook}: {@link HookPosition#BEFORE_MODEL} and {@link HookPosition#AFTER_MODEL}</li>
	 *     </ul>
	 *   </li>
	 *   <li>If neither applies, return empty array (hook will not be executed)</li>
	 * </ol>
	 *
	 * @return array of HookPosition values indicating where this hook should execute
	 * @see HookPosition
	 * @see HookPositions
	 */
	default HookPosition[] getHookPositions() {
		HookPositions annotation = this.getClass().getAnnotation(HookPositions.class);
		if (annotation != null) {
			return annotation.value();
		}
		// Default fallback based on hook type
		if (this instanceof AgentHook) {
			return new HookPosition[]{HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT};
		} else if (this instanceof ModelHook) {
			return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
		}
		return new HookPosition[0];
	}

	/**
	 * Generates the full qualified name for a hook in the execution graph.
	 * <p>
	 * The full name is constructed by prefixing the hook's name with the agent hook name prefix.
	 * This ensures unique identification of hooks within the graph execution context.
	 *
	 * @param hook the hook instance, must not be null
	 * @return the full hook name with prefix (e.g., "agent.hook.myHook")
	 */
	static String getFullHookName(Hook hook) {
		return AGENT_HOOK_NAME_PREFIX + hook.getName();
	}
}
