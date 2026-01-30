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
package com.alibaba.cloud.ai.graph.agent.hook.shelltool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ToolInjection;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.agent.tools.ShellSessionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hook for managing ShellTool2 session.
 * This hook initializes the shell session before the agent starts and cleans it up after the agent finishes.
 * Supports ShellTool2 (@Tool annotation).
 */
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class ShellToolAgentHook extends AgentHook implements ToolInjection {

	private static final Logger log = LoggerFactory.getLogger(ShellToolAgentHook.class);

	private ShellTool2 shellTool2;
	private String shellToolName;

	/**
	 * Private constructor for builder pattern.
	 */
	private ShellToolAgentHook() {
	}

	/**
	 * Private constructor with ShellTool2 for builder pattern.
	 * @param shellTool2 the ShellTool2 instance to use
	 * @param shellToolName the name of the shell tool
	 */
	private ShellToolAgentHook(ShellTool2 shellTool2, String shellToolName) {
		this.shellTool2 = shellTool2;
		this.shellToolName = shellToolName;
	}

	/**
	 * Create a new builder instance.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
		ShellSessionManager sessionManager = getSessionManager();
		if (sessionManager == null) {
			log.warn("ShellToolAgentHook: No ShellTool2 injected, skipping initialization");
			return CompletableFuture.completedFuture(new HashMap<>());
		}

		log.info("ShellToolAgentHook: Initializing shell session before agent execution");

		try {
			sessionManager.initialize(config);
			log.info("Shell session initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize shell session", e);
			throw new RuntimeException("Failed to initialize shell session", e);
		}

		return CompletableFuture.completedFuture(new HashMap<>());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
		ShellSessionManager sessionManager = getSessionManager();
		if (sessionManager == null) {
			log.warn("ShellToolAgentHook: No ShellTool2 injected, skipping cleanup");
			return CompletableFuture.completedFuture(new HashMap<>());
		}

		log.info("ShellToolAgentHook: Cleaning up shell session after agent execution");

		try {
			sessionManager.cleanup(config);
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
	public void injectTool(ToolCallback toolCallback) {
		if (shellTool2 != null) {
			// Tool already injected
			return;
		}

		log.info("ShellToolAgentHook: Processing tool callback for shell tool extraction");

		try {
			// Extract ShellTool2
			ShellTool2 extractedShellTool2 = extractShellTool2(toolCallback);
			if (extractedShellTool2 != null) {
				this.shellTool2 = extractedShellTool2;
				log.info("Successfully extracted and injected ShellTool2 from tool: {}",
						toolCallback.getToolDefinition().name());
				return;
			}

			log.warn("Failed to extract ShellTool2 from tool: {}",
					toolCallback.getToolDefinition().name());
		} catch (Exception e) {
			log.error("Error extracting ShellTool2 from tool callback", e);
		}
	}

	/**
	 * Extract ShellTool2 instance from ToolCallback using reflection.
	 * Supports MethodToolCallback wrapping ShellTool2 (with @Tool annotation).
	 */
	private ShellTool2 extractShellTool2(ToolCallback toolCallback) {
		try {
			Class<?> clazz = toolCallback.getClass();

			// Look for 'toolObject' field in MethodToolCallback
			while (clazz != null) {
				try {
					Field toolObjectField = clazz.getDeclaredField("toolObject");
					toolObjectField.setAccessible(true);
					Object toolObject = toolObjectField.get(toolCallback);

					if (toolObject instanceof ShellTool2) {
						return (ShellTool2) toolObject;
					}
					break;
				} catch (NoSuchFieldException e) {
					// Try parent class
					clazz = clazz.getSuperclass();
				}
			}
		} catch (Exception e) {
			log.debug("Could not extract ShellTool2 from ToolCallback via reflection", e);
		}

		return null;
	}

	@Override
	public List<ToolCallback> getTools() {
		if (shellTool2 == null) {
			log.info("No ShellTool2 instance injected, creating default instance");
			this.shellTool2 = ShellTool2.builder(System.getProperty("user.dir")).build();
		}
		return Arrays.asList(ToolCallbacks.from(shellTool2));
	}

	@Override
	public String getRequiredToolName() {
		// Match by tool name "shell"
		return shellToolName;
	}

	@Override
	public Class<? extends ToolCallback> getRequiredToolType() {
		// We don't filter by ToolCallback type because ShellTool2 is wrapped
		// We rely on tool name matching instead
		return null;
	}

	/**
	 * Get the injected ShellTool2 instance.
	 * @return the ShellTool2 instance, or null if not injected
	 */
	protected ShellTool2 getShellTool2() {
		return shellTool2;
	}

	/**
	 * Get the ShellSessionManager from ShellTool2.
	 * @return the ShellSessionManager instance, or null if ShellTool2 is not injected
	 */
	private ShellSessionManager getSessionManager() {
		if (shellTool2 != null) {
			return shellTool2.getSessionManager();
		}
		return null;
	}

	/**
	 * Builder class for constructing ShellToolAgentHook instances.
	 */
	public static class Builder {
		private ShellTool2 shellTool2;
		private String shellToolName;

		/**
		 * Set the ShellTool2 instance.
		 * @param shellTool2 the ShellTool2 to use
		 * @return this builder instance
		 */
		public Builder shellTool2(ShellTool2 shellTool2) {
			this.shellTool2 = shellTool2;
			return this;
		}

		/**
		 * Set the shell tool name.
		 * @param shellToolName the name of the shell tool
		 * @return this builder instance
		 */
		public Builder shellToolName(String shellToolName) {
			this.shellToolName = shellToolName;
			return this;
		}

		/**
		 * Build the ShellToolAgentHook instance.
		 * @return a new ShellToolAgentHook instance
		 */
		public ShellToolAgentHook build() {
			return new ShellToolAgentHook(shellTool2, shellToolName);
		}
	}
}
