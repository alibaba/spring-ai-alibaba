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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.tool.ToolCallback;

/**
 * Marker interface for tool callbacks that need state injection.
 * AgentToolNode will inject state/config/updateMap for tools implementing this interface.
 *
 * <p>Tools implementing this interface will receive the following keys in their ToolContext:</p>
 * <ul>
 *   <li>{@code AGENT_STATE_CONTEXT_KEY} - The current OverAllState</li>
 *   <li>{@code AGENT_CONFIG_CONTEXT_KEY} - The RunnableConfig</li>
 *   <li>{@code AGENT_STATE_FOR_UPDATE_CONTEXT_KEY} - A Map for state updates</li>
 * </ul>
 *
 * @author disaster
 * @since 1.0.0
 * @see AsyncToolCallback
 * @see CancellableAsyncToolCallback
 */
public interface StateAwareToolCallback extends ToolCallback {
	// marker interface - no methods required
}
