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

import org.springframework.ai.tool.ToolCallback;

/**
 * Interface for hooks that need a specific tool injected.
 * Hooks implementing this interface can declare which tool they need,
 * and the framework will automatically inject the matching tool.
 */
public interface ToolInjection {

	/**
	 * Inject a tool into the hook.
	 * Only the tool matching the required tool name or type will be injected.
	 *
	 * @param tool the tool callback to inject
	 */
	void injectTool(ToolCallback tool);

	/**
	 * Get the required tool name that this hook needs.
	 * Return null to match by tool type instead.
	 *
	 * @return the tool name required by this hook, or null to match by type
	 */
	default String getRequiredToolName() {
		return null;
	}

	/**
	 * Get the required tool type (class) that this hook needs.
	 * Return null to match by tool name instead.
	 * If both name and type are null, the first available tool will be injected.
	 *
	 * @return the tool class type required by this hook, or null to match by name
	 */
	default Class<? extends ToolCallback> getRequiredToolType() {
		return null;
	}
}

