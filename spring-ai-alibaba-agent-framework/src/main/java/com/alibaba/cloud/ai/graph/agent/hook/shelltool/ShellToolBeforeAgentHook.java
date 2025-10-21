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
import com.alibaba.cloud.ai.graph.agent.hook.BeforeAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.interceptor.shelltool.ShellToolInterceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Before-agent hook that initializes the shell session before agent execution starts.
 *
 * This hook works in conjunction with ShellToolInterceptor to provide persistent
 * shell execution capabilities. It must be paired with ShellToolAfterAgentHook
 * to ensure proper cleanup.
 *
 * Example:
 * <pre>
 * ShellToolInterceptor interceptor = ShellToolInterceptor.builder()
 *     .workspaceRoot("/tmp/agent-workspace")
 *     .commandTimeout(30000)
 *     .build();
 *
 * ShellToolBeforeAgentHook beforeHook = new ShellToolBeforeAgentHook(interceptor);
 * ShellToolAfterAgentHook afterHook = new ShellToolAfterAgentHook(interceptor);
 *
 * // Register both hooks in agent
 * </pre>
 */
public class ShellToolBeforeAgentHook extends BeforeAgentHook {

	private final ShellToolInterceptor interceptor;

	public ShellToolBeforeAgentHook(ShellToolInterceptor interceptor) {
		if (interceptor == null) {
			throw new IllegalArgumentException("ShellToolInterceptor cannot be null");
		}
		this.interceptor = interceptor;
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
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
	public String getName() {
		return "ShellToolBeforeAgent";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}
}
