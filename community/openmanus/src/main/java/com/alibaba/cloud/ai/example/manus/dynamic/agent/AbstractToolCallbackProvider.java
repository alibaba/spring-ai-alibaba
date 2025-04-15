/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.agent;

import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractToolCallbackProvider implements ToolCallbackProvider {

	private final Map<String, Map<String, ManusConfiguration.ToolCallBackContext>> cachedToolCallbackMap = new ConcurrentHashMap<>();

	@Override
	public Map<String, ManusConfiguration.ToolCallBackContext> getToolCallBackContexts(String planId) {
		return cachedToolCallbackMap.computeIfAbsent(planId, k -> getToolCallBackContexts());
	}

	protected abstract Map<String, ManusConfiguration.ToolCallBackContext> getToolCallBackContexts();

	@Override
	public Map<String, ManusConfiguration.ToolCallBackContext> removePlan(String planId) {
		return cachedToolCallbackMap.remove(planId);
	}

}
