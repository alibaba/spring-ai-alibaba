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
package com.alibaba.cloud.ai.graph.agent.hook;


import com.alibaba.cloud.ai.graph.KeyStrategy;

import java.util.List;
import java.util.Map;

public interface Hook {
	String getName();

	void setAgentName(String agentName);

	String getAgentName();

	default List<JumpTo> canJumpTo() {
		return List.of();
	}

	default Map<String, KeyStrategy> getKeyStrategys() {
		return Map.of();
	}

	/**
	 * Get the positions where this hook should be executed.
	 * By default, this method checks for the @HookPositions annotation on the implementing class.
	 *
	 * @return array of HookPosition values
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
}
