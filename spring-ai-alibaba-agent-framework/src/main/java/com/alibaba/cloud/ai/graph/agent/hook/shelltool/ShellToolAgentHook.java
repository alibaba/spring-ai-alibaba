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
package com.alibaba.cloud.ai.graph.agent.hook.shelltool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.HookType;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ToolInjection;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook for managing ShellTool session.
 * This hook initializes the shell session before the agent starts and cleans it up after the agent finishes.
 */
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class ShellToolAgentHook implements AgentHook, ToolInjection {

	private static final Logger log = LoggerFactory.getLogger(ShellToolAgentHook.class);

	private ShellTool shellTool;

	@Override
	public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
		if (shellTool == null) {
			log.warn("ShellToolAgentHook: No ShellTool injected, skipping initialization");
			return CompletableFuture.completedFuture(new HashMap<>());
		}

		log.info("ShellToolAgentHook: Initializing shell session before agent execution");

		try {
			shellTool.getSessionManager().initialize();
			log.info("Shell session initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize shell session", e);
			throw new RuntimeException("Failed to initialize shell session", e);
		}

		return CompletableFuture.completedFuture(new HashMap<>());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
		if (shellTool == null) {
			log.warn("ShellToolAgentHook: No ShellTool injected, skipping cleanup");
			return CompletableFuture.completedFuture(new HashMap<>());
		}

		log.info("ShellToolAgentHook: Cleaning up shell session after agent execution");

		try {
			shellTool.getSessionManager().cleanup();
			log.info("Shell session cleaned up successfully");
		} catch (Exception e) {
			log.error("Failed to cleanup shell session", e);
			// Don't throw exception in cleanup to avoid masking original errors
		}

		return CompletableFuture.completedFuture(new HashMap<>());
	}

	@Override
	public String getName() {
		return "ShellToolAgentHook";
	}

	@Override
	public HookType getHookType() {
		return HookType.AGENT;
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	@Override
	public void injectTool(ToolCallback toolCallback) {
		log.info("ShellToolAgentHook: Processing tool callback for shell tool extraction");

		try {
			ShellTool extracted = extractShellTool(toolCallback);
			if (extracted != null) {
				this.shellTool = extracted;
				log.info("Successfully extracted and injected ShellTool from tool: {}",
					toolCallback.getToolDefinition().name());
			} else {
				log.warn("Failed to extract ShellTool from tool: {}",
					toolCallback.getToolDefinition().name());
			}
		} catch (Exception e) {
			log.error("Error extracting ShellTool from tool callback", e);
		}
	}

	/**
	 * Extract ShellTool instance from ToolCallback using reflection.
	 * Supports FunctionToolCallback wrapping ShellTool.
	 */
	private ShellTool extractShellTool(ToolCallback toolCallback) {
		try {

			// Try to access the 'function' field from FunctionToolCallback
			Class<?> clazz = toolCallback.getClass();

			// Look for 'function' field in the class hierarchy
			while (clazz != null) {
				try {
					Field functionField = clazz.getDeclaredField("toolFunction");
					functionField.setAccessible(true);
					Object function = functionField.get(toolCallback);

					if (function instanceof ShellTool) {
						return (ShellTool) function;
					}
					break;
				} catch (NoSuchFieldException e) {
					// Try parent class
					clazz = clazz.getSuperclass();
				}
			}
		} catch (Exception e) {
			log.debug("Could not extract ShellTool from ToolCallback via reflection", e);
		}

		return null;
	}

	@Override
	public String getRequiredToolName() {
		// Match by tool name "shell"
		return "shell";
	}

	@Override
	public Class<? extends ToolCallback> getRequiredToolType() {
		// We don't filter by ToolCallback type because ShellTool is wrapped
		// We rely on tool name matching instead
		return null;
	}

	/**
	 * Get the injected ShellTool instance.
	 * @return the ShellTool instance, or null if not injected
	 */
	protected ShellTool getShellTool() {
		return shellTool;
	}
}
