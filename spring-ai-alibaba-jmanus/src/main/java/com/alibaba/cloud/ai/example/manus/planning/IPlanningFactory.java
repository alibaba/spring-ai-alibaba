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
package com.alibaba.cloud.ai.example.manus.planning;

import java.util.Map;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.ToolCallbackProvider;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;

/**
 * Planning factory interface, providing planning-related object creation functionality
 */
public interface IPlanningFactory {

	/**
	 * Create planning coordinator
	 * @param context
	 * @return Planning coordinator
	 */
	PlanningCoordinator createPlanningCoordinator(ExecutionContext context);

	/**
	 * Create tool callback mapping
	 * @param planId Plan ID
	 * @param rootPlanId Root plan ID
	 * @param expectedReturnInfo Expected return information
	 * @return Tool callback mapping
	 */
	Map<String, PlanningFactory.ToolCallBackContext> toolCallbackMap(String planId, String rootPlanId,
			String expectedReturnInfo);

	/**
	 * Create RestClient
	 * @return RestClient builder
	 */
	RestClient.Builder createRestClient();

	/**
	 * Create empty tool callback provider
	 * @return Tool callback provider functional interface
	 */
	ToolCallbackProvider emptyToolCallbackProvider();

}
