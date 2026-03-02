/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.support;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model interceptor that applies step-based configuration: system prompt and tools
 * are chosen from {@link StepConfig} based on {@code current_step} in the request context
 * (state). Enables the handoffs state-machine pattern with a single agent.
 */
public class StepConfigInterceptor extends ModelInterceptor {

	private final Map<String, StepConfig> stepConfigMap;

	public StepConfigInterceptor(Map<String, StepConfig> stepConfigMap) {
		this.stepConfigMap = stepConfigMap;
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		Map<String, Object> context = request.getContext();
		String currentStep = (String) context.getOrDefault(SupportStateConstants.CURRENT_STEP,
				SupportStateConstants.STEP_WARRANTY_COLLECTOR);

		StepConfig stepConfig = stepConfigMap.get(currentStep);
		if (stepConfig == null) {
			stepConfig = stepConfigMap.get(SupportStateConstants.STEP_WARRANTY_COLLECTOR);
		}

		for (String required : stepConfig.requiredKeys()) {
			if (context.get(required) == null) {
				throw new IllegalStateException(required + " must be set before reaching " + currentStep);
			}
		}

		String systemPrompt = formatPrompt(stepConfig.prompt(), context);
		List<String> toolNames = stepConfig.tools().stream()
				.map(t -> t.getToolDefinition().name())
				.collect(Collectors.toList());

		ModelRequest overridden = ModelRequest.builder(request)
				.systemMessage(new SystemMessage(systemPrompt))
				.tools(toolNames)
				.dynamicToolCallbacks(new ArrayList<>(stepConfig.tools()))
				.build();

		return handler.call(overridden);
	}

	private static String formatPrompt(String template, Map<String, Object> context) {
		String s = template;
		for (Map.Entry<String, Object> e : context.entrySet()) {
			if (e.getValue() != null && !(e.getValue() instanceof List) && !"messages".equals(e.getKey())) {
				s = s.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
			}
		}
		return s;
	}

	@Override
	public String getName() {
		return "StepConfig";
	}

	/**
	 * Step configuration: prompt template and tools for one step.
	 */
	public record StepConfig(String prompt, List<ToolCallback> tools, List<String> requiredKeys) {
	}
}
