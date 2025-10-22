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
package com.alibaba.cloud.ai.graph.agent.hook.shelltool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.HookType;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.interceptor.shelltool.ShellToolInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class ShellToolHook implements AgentHook {

	private static final Logger log = LoggerFactory.getLogger(ShellToolHook.class);

	private final ShellToolInterceptor interceptor;

	public ShellToolHook(ShellToolInterceptor interceptor) {
		if (interceptor == null) {
			throw new IllegalArgumentException("ShellToolInterceptor cannot be null");
		}
		this.interceptor = interceptor;
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
		try {
			// Initialize shell session and run startup commands
			interceptor.beforeAgent();
			return CompletableFuture.completedFuture(Map.of());
		} catch (Exception e) {
			return CompletableFuture.failedFuture(
				new RuntimeException("Failed to initialize shell session", e)
			);
		}
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
		try {
			// Run shutdown commands and cleanup resources
			interceptor.afterAgent();
			return CompletableFuture.completedFuture(Map.of());
		} catch (Exception e) {
			log.error("Failed to cleanup shell session", e);
			// Don't fail the agent run if cleanup fails, just log it
			return CompletableFuture.completedFuture(Map.of());
		}
	}

	@Override
	public String getName() {
		return "ShellToolHook";
	}

	@Override
	public HookType getHookType() {
		return HookType.MODEL;
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

}
